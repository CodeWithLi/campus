package campus.ai.rag.service.service.wx.kb.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxKbUploadDTO;
import campus.ai.rag.service.entity.kb.WxKbDocumentEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.kb.WxKbDocumentMapper;
import campus.ai.rag.service.service.admin.kb.DocumentParseService;
import campus.ai.rag.service.service.wx.kb.WxKbDocumentService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxKbDocumentVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WxKbDocumentServiceImpl implements WxKbDocumentService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String EMBEDDING_MODEL = "text-embedding-v4";
    private static final String[] ALLOWED_EXTS = {"pdf", "txt", "md", "doc", "docx"};
    private static final Path BASE_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"))
            .resolve(Paths.get("uploads", "wx_kb"));
    private static final int CHUNK_SIZE = 700;
    private static final int CHUNK_OVERLAP = 120;

    private final WxKbDocumentMapper wxKbDocumentMapper;
    private final DocumentParseService documentParseService;
    private final RestTemplate restTemplate;
    private final JdbcTemplate pgJdbcTemplate;
    private final String embeddingApiKey;
    private final String embeddingBaseUrl;
    private final Integer embeddingDimensions;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);

    public WxKbDocumentServiceImpl(WxKbDocumentMapper wxKbDocumentMapper,
                                   DocumentParseService documentParseService,
                                   RestTemplate restTemplate,
                                   @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
                                   @Value("${spring.ai.openai.api-key:}") String embeddingApiKey,
                                   @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String embeddingBaseUrl,
                                   @Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer embeddingDimensions) {
        this.wxKbDocumentMapper = wxKbDocumentMapper;
        this.documentParseService = documentParseService;
        this.restTemplate = restTemplate;
        this.pgJdbcTemplate = pgJdbcTemplate;
        this.embeddingApiKey = embeddingApiKey;
        this.embeddingBaseUrl = embeddingBaseUrl;
        this.embeddingDimensions = embeddingDimensions;
    }

    @Override
    public WxKbDocumentVO upload(WxKbUploadDTO dto, MultipartFile file) {
        String userIdStr = WxAuthContextUtils.requireWxUserId();
        Long userId = parseUserId(userIdStr);
        validateUpload(dto, file);

        String originalName = Objects.requireNonNull(file.getOriginalFilename()).trim();
        String ext = extractExt(originalName);
        validateFileExt(ext);

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM", Locale.ROOT));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = BASE_UPLOAD_DIR.resolve(Paths.get(String.valueOf(userId), datePath));
        Path target = dir.resolve(newFileName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档保存失败：" + e.getMessage());
        }

        WxKbDocumentEntity entity = new WxKbDocumentEntity();
        entity.setUserId(userId);
        entity.setTitle(dto.getTitle().trim());
        entity.setOriginalFileName(originalName);
        entity.setFileExt(ext);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setStoragePath(target.toString().replace("\\", "/"));
        entity.setProcessStatus(STATUS_PENDING);
        entity.setRetryCount(0);
        entity.setEmbeddingModel(EMBEDDING_MODEL);
        entity.setChunkCount(0);
        entity.setParsedContent(null);
        entity.setRemark(trimOrNull(dto.getRemark()));
        wxKbDocumentMapper.insert(entity);
        if (entity.getId() != null) {
            asyncParseAndIndex(userId, entity.getId());
        }
        return toVO(entity);
    }

    private void asyncParseAndIndex(Long userId, Long documentId) {
        asyncExecutor.submit(() -> {
            try {
                parseAndIndex(userId, documentId);
            } catch (Exception ignored) {
                // 状态与失败原因已在 parseAndIndex 内更新
            }
        });
    }

    private WxKbDocumentVO parseAndIndex(Long userId, Long documentId) {
        WxKbDocumentEntity entity = getRequired(userId, documentId);

        entity.setProcessStatus(STATUS_PROCESSING);
        entity.setFailReason(null);
        wxKbDocumentMapper.updateById(entity);

        try {
            Path path = Paths.get(entity.getStoragePath());
            String parsed = documentParseService.parseToText(path, entity.getFileExt());
            if (isBlank(parsed)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档解析结果为空");
            }
            entity.setParsedContent(parsed);
            wxKbDocumentMapper.updateById(entity);

            List<String> chunks = chunkText(parsed);
            if (chunks.isEmpty()) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "切片结果为空");
            }

            // 清理旧向量
            pgJdbcTemplate.update("DELETE FROM wx_kb_chunk WHERE user_id = ? AND document_id = ?", userId, entity.getId());

            int idx = 0;
            for (String chunk : chunks) {
                float[] emb = embedWithQwen(chunk);
                String vec = toVectorLiteral(emb);
                pgJdbcTemplate.update(
                        "INSERT INTO wx_kb_chunk(user_id, document_id, chunk_index, content, embedding) VALUES(?,?,?,?,?::vector)",
                        userId, entity.getId(), idx, chunk, vec
                );
                idx++;
            }

            entity.setChunkCount(chunks.size());
            entity.setProcessStatus(STATUS_SUCCESS);
            entity.setProcessedAt(LocalDateTime.now());
            entity.setFailReason(null);
            wxKbDocumentMapper.updateById(entity);
            return toVO(entity);
        } catch (BusinessException ex) {
            entity.setProcessStatus(STATUS_FAILED);
            entity.setFailReason(ex.getMessage());
            entity.setProcessedAt(LocalDateTime.now());
            wxKbDocumentMapper.updateById(entity);
            throw ex;
        } catch (Exception ex) {
            entity.setProcessStatus(STATUS_FAILED);
            entity.setFailReason("处理异常：" + ex.getMessage());
            entity.setProcessedAt(LocalDateTime.now());
            wxKbDocumentMapper.updateById(entity);
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "处理异常：" + ex.getMessage());
        }
    }

    private WxKbDocumentEntity getRequired(Long userId, Long id) {
        if (id == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文档ID不能为空");
        }
        WxKbDocumentEntity entity = wxKbDocumentMapper.selectById(id);
        if (entity == null || entity.getUserId() == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "文档不存在");
        }
        return entity;
    }

    private List<String> chunkText(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return List.of();
        List<String> list = new ArrayList<>();
        int start = 0;
        while (start < t.length()) {
            int end = Math.min(t.length(), start + CHUNK_SIZE);
            String chunk = t.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                list.add(chunk);
            }
            if (end >= t.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private float[] embedWithQwen(String text) {
        if (isBlank(embeddingApiKey)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "未配置 DASHSCOPE_API_KEY，无法执行向量化");
        }
        String endpoint = embeddingBaseUrl.endsWith("/") ? embeddingBaseUrl + "embeddings" : embeddingBaseUrl + "/embeddings";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(embeddingApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", EMBEDDING_MODEL);
        body.put("input", text);
        body.put("dimensions", embeddingDimensions);

        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> root = response.getBody();
        if (root == null) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化返回为空");
        }
        Object dataObj = root.get("data");
        if (!(dataObj instanceof List) || ((List<?>) dataObj).isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化返回缺少data");
        }
        Object firstObj = ((List<?>) dataObj).get(0);
        if (!(firstObj instanceof Map)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果结构异常");
        }
        Object embeddingObj = ((Map<?, ?>) firstObj).get("embedding");
        if (!(embeddingObj instanceof List) || ((List<?>) embeddingObj).isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果缺少embedding");
        }
        List<?> embeddingList = (List<?>) embeddingObj;
        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            Object numObj = embeddingList.get(i);
            if (!(numObj instanceof Number)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果包含非数值元素");
            }
            result[i] = ((Number) numObj).floatValue();
        }
        return result;
    }

    private String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量结果为空");
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private void validateUpload(WxKbUploadDTO dto, MultipartFile file) {
        if (dto == null || isBlank(dto.getTitle())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "title 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "上传文件不能为空");
        }
        if (isBlank(file.getOriginalFilename())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文件名不能为空");
        }
    }

    private void validateFileExt(String ext) {
        for (String allowed : ALLOWED_EXTS) {
            if (allowed.equalsIgnoreCase(ext)) {
                return;
            }
        }
        throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持上传 pdf/txt/md/doc/docx 文档");
    }

    private String extractExt(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).trim().toLowerCase(Locale.ROOT);
    }

    private WxKbDocumentVO toVO(WxKbDocumentEntity e) {
        WxKbDocumentVO vo = new WxKbDocumentVO();
        vo.setId(e.getId());
        vo.setTitle(e.getTitle());
        vo.setOriginalFileName(e.getOriginalFileName());
        vo.setFileExt(e.getFileExt());
        vo.setProcessStatus(e.getProcessStatus());
        vo.setFailReason(e.getFailReason());
        vo.setChunkCount(e.getChunkCount());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (Exception e) {
            return -1L;
        }
    }

    private String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    @PreDestroy
    public void shutdownExecutor() {
        asyncExecutor.shutdown();
    }
}

