package campus.ai.rag.service.service.admin.kb.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.kb.KbDocumentUpdateStatusDTO;
import campus.ai.rag.service.dto.admin.kb.KbDocumentUploadDTO;
import campus.ai.rag.service.dto.admin.kb.KbImageUnderstandIndexDTO;
import campus.ai.rag.service.entity.kb.KbCategoryEntity;
import campus.ai.rag.service.entity.kb.KbDocumentEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.kb.KbCategoryMapper;
import campus.ai.rag.service.mapper.kb.KbDocumentMapper;
import campus.ai.rag.service.service.admin.kb.AdminKbDocumentService;
import campus.ai.rag.service.service.admin.kb.DocumentParseService;
import campus.ai.rag.service.service.admin.kb.ImageUnderstandService;
import campus.ai.rag.service.vo.admin.kb.KbDocumentImagePreviewVO;
import campus.ai.rag.service.vo.admin.kb.KbImageUnderstandTaskVO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentPageVO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class AdminKbDocumentServiceImpl implements AdminKbDocumentService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String EMBEDDING_MODEL = "text-embedding-v4";
    private static final String[] ALLOWED_EXTS = {"pdf", "txt", "md", "doc", "docx"};
    /**
     * 上传文件落盘目录：默认基于 user.dir，避免 MultipartFile.transferTo 在某些容器实现下
     * 将相对路径解析到 Tomcat 临时目录导致父目录不存在。
     */
    private static final Path BASE_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"))
            .resolve(Paths.get("uploads", "kb"));
    private static final int CHUNK_SIZE = 700;
    private static final int CHUNK_OVERLAP = 120;
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PROCESSING = "PROCESSING";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_STATUS_FAILED = "FAILED";
    private static final String TASK_TYPE_PREVIEW = "PREVIEW";
    private static final String TASK_TYPE_INDEX = "INDEX";

    private final KbDocumentMapper kbDocumentMapper;
    private final KbCategoryMapper kbCategoryMapper;
    private final DocumentParseService documentParseService;
    private final ImageUnderstandService imageUnderstandService;
    private final RestTemplate restTemplate;
    private final JdbcTemplate pgJdbcTemplate;
    private final String embeddingApiKey;
    private final String embeddingBaseUrl;
    private final Integer embeddingDimensions;
    private final ConcurrentHashMap<String, ImageUnderstandTaskSnapshot> imageTaskStore = new ConcurrentHashMap<>();
    private final ExecutorService imageTaskExecutor = Executors.newFixedThreadPool(2);

    public AdminKbDocumentServiceImpl(KbDocumentMapper kbDocumentMapper,
                                      KbCategoryMapper kbCategoryMapper,
                                      DocumentParseService documentParseService,
                                      ImageUnderstandService imageUnderstandService,
                                      RestTemplate restTemplate,
                                      @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
                                      @Value("${spring.ai.openai.api-key:}") String embeddingApiKey,
                                      @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String embeddingBaseUrl,
                                      @Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer embeddingDimensions) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbCategoryMapper = kbCategoryMapper;
        this.documentParseService = documentParseService;
        this.imageUnderstandService = imageUnderstandService;
        this.restTemplate = restTemplate;
        this.pgJdbcTemplate = pgJdbcTemplate;
        this.embeddingApiKey = embeddingApiKey;
        this.embeddingBaseUrl = embeddingBaseUrl;
        this.embeddingDimensions = embeddingDimensions;
    }

    @Override
    public KbDocumentVO upload(KbDocumentUploadDTO dto, MultipartFile file) {
        validateUpload(dto, file);
        KbCategoryEntity category = kbCategoryMapper.selectById(dto.getCategoryId());
        if (Objects.isNull(category)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "分类不存在");
        }

        String originalName = Objects.requireNonNull(file.getOriginalFilename()).trim();
        String ext = extractExt(originalName);
        validateFileExt(ext);

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM", Locale.ROOT));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = BASE_UPLOAD_DIR.resolve(datePath);
        Path target = dir.resolve(newFileName);

        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档保存失败：" + e.getMessage());
        }

        KbDocumentEntity entity = new KbDocumentEntity();
        entity.setCategoryId(dto.getCategoryId());
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
        kbDocumentMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public KbDocumentVO uploadAndIndex(KbDocumentUploadDTO dto, MultipartFile file) {
        KbDocumentVO uploaded = upload(dto, file);
        if (uploaded == null || uploaded.getId() == null) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "上传失败：未生成文档ID");
        }
        parse(uploaded.getId());
        return index(uploaded.getId());
    }

    @Override
    public KbDocumentVO updateStatus(Long id, KbDocumentUpdateStatusDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getProcessStatus())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "处理状态不能为空");
        }
        KbDocumentEntity entity = getRequiredDocument(id);
        String status = dto.getProcessStatus().trim().toUpperCase(Locale.ROOT);
        validateStatus(status);

        entity.setProcessStatus(status);
        if (STATUS_FAILED.equals(status)) {
            entity.setFailReason(trimOrNull(dto.getFailReason()));
        } else {
            entity.setFailReason(null);
        }
        if (STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status)) {
            entity.setProcessedAt(LocalDateTime.now());
        }
        kbDocumentMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public KbDocumentVO retry(Long id) {
        KbDocumentEntity entity = getRequiredDocument(id);
        if (!STATUS_FAILED.equals(entity.getProcessStatus())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅失败状态文档可重试");
        }
        entity.setProcessStatus(STATUS_PENDING);
        entity.setFailReason(null);
        entity.setProcessedAt(null);
        entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
        kbDocumentMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public KbDocumentVO parse(Long id) {
        KbDocumentEntity entity = getRequiredDocument(id);
        entity.setProcessStatus(STATUS_PROCESSING);
        entity.setFailReason(null);
        kbDocumentMapper.updateById(entity);

        try {
            Path path = Paths.get(entity.getStoragePath());
            String parsed = documentParseService.parseToText(path, entity.getFileExt());
            if (isBlank(parsed)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档解析结果为空");
            }

            entity.setParsedContent(parsed);
            entity.setProcessStatus(STATUS_SUCCESS);
            entity.setProcessedAt(LocalDateTime.now());
            entity.setFailReason(null);
            kbDocumentMapper.updateById(entity);
            return toVO(entity);
        } catch (BusinessException ex) {
            entity.setProcessStatus(STATUS_FAILED);
            entity.setFailReason(ex.getMessage());
            entity.setProcessedAt(LocalDateTime.now());
            kbDocumentMapper.updateById(entity);
            throw ex;
        } catch (Exception ex) {
            entity.setProcessStatus(STATUS_FAILED);
            entity.setFailReason("文档解析异常：" + ex.getMessage());
            entity.setProcessedAt(LocalDateTime.now());
            kbDocumentMapper.updateById(entity);
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档解析异常：" + ex.getMessage());
        }
    }

    @Override
    public KbDocumentVO index(Long id) {
        KbDocumentEntity entity = getRequiredDocument(id);
        if (isBlank(entity.getParsedContent())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "请先完成文档解析");
        }
        return indexByText(entity, entity.getParsedContent());
    }

    @Override
    public KbDocumentImagePreviewVO previewImageUnderstand(Long id) {
        KbDocumentEntity entity = getRequiredDocument(id);
        Path path = Paths.get(entity.getStoragePath());
        List<KbDocumentImagePreviewVO.Item> items = imageUnderstandService.understandImages(entity.getId(), path, entity.getFileExt());
        String mergedText = buildImageUnderstandText(items);
        entity.setImageUnderstandText(trimOrNull(mergedText));
        kbDocumentMapper.updateById(entity);

        KbDocumentImagePreviewVO vo = new KbDocumentImagePreviewVO();
        vo.setDocumentId(entity.getId());
        vo.setImageCount(items.size());
        vo.setMergedText(mergedText);
        vo.setItems(items);
        return vo;
    }

    @Override
    public KbDocumentVO imageUnderstandAndIndex(Long id) {
        KbDocumentEntity entity = getRequiredDocument(id);
        if (isBlank(entity.getParsedContent())) {
            parse(id);
            entity = getRequiredDocument(id);
        }
        KbDocumentImagePreviewVO preview = previewImageUnderstand(id);
        String merged = mergeParsedAndImageText(entity.getParsedContent(), preview.getMergedText());
        return indexByText(entity, merged);
    }

    @Override
    public KbImageUnderstandTaskVO submitImageUnderstandPreviewTask(Long id) {
        getRequiredDocument(id);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImageUnderstandTaskSnapshot task = new ImageUnderstandTaskSnapshot();
        task.taskId = taskId;
        task.documentId = id;
        task.taskType = TASK_TYPE_PREVIEW;
        task.taskStatus = TASK_STATUS_PENDING;
        task.message = "任务已提交，等待处理";
        task.submittedAt = LocalDateTime.now();
        imageTaskStore.put(taskId, task);

        imageTaskExecutor.submit(() -> runPreviewTask(taskId));
        return toTaskVO(task);
    }

    @Override
    public KbImageUnderstandTaskVO submitImageUnderstandIndexTask(Long id, KbImageUnderstandIndexDTO dto) {
        getRequiredDocument(id);
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImageUnderstandTaskSnapshot task = new ImageUnderstandTaskSnapshot();
        task.taskId = taskId;
        task.documentId = id;
        task.taskType = TASK_TYPE_INDEX;
        task.taskStatus = TASK_STATUS_PENDING;
        task.message = "任务已提交，等待处理";
        task.submittedAt = LocalDateTime.now();
        task.manualImageText = dto == null ? null : trimOrNull(dto.getImageUnderstandText());
        imageTaskStore.put(taskId, task);

        imageTaskExecutor.submit(() -> runIndexTask(taskId));
        return toTaskVO(task);
    }

    @Override
    public KbImageUnderstandTaskVO submitImageUnderstandIndexTaskByTaskId(String taskId, KbImageUnderstandIndexDTO dto) {
        if (isBlank(taskId)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "任务ID不能为空");
        }
        ImageUnderstandTaskSnapshot sourceTask = imageTaskStore.get(taskId.trim());
        if (sourceTask == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "图片理解任务不存在");
        }
        if (!TASK_STATUS_SUCCESS.equals(sourceTask.taskStatus)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅成功任务可执行一键向量化");
        }
        if (sourceTask.documentId == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "任务缺少文档ID");
        }

        String newTaskId = UUID.randomUUID().toString().replace("-", "");
        ImageUnderstandTaskSnapshot newTask = new ImageUnderstandTaskSnapshot();
        newTask.taskId = newTaskId;
        newTask.documentId = sourceTask.documentId;
        newTask.taskType = TASK_TYPE_INDEX;
        newTask.taskStatus = TASK_STATUS_PENDING;
        newTask.message = "任务已提交，等待处理";
        newTask.submittedAt = LocalDateTime.now();
        newTask.manualImageText = dto == null ? null : trimOrNull(dto.getImageUnderstandText());
        if (isBlank(newTask.manualImageText)) {
            newTask.manualImageText = trimOrNull(sourceTask.mergedText);
        }
        imageTaskStore.put(newTaskId, newTask);

        imageTaskExecutor.submit(() -> runIndexTask(newTaskId));
        return toTaskVO(newTask);
    }

    @Override
    public KbImageUnderstandTaskVO getImageUnderstandTask(String taskId) {
        if (isBlank(taskId)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "任务ID不能为空");
        }
        ImageUnderstandTaskSnapshot task = imageTaskStore.get(taskId.trim());
        if (task == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "任务不存在或已过期");
        }
        return toTaskVO(task);
    }

    @Override
    public List<KbImageUnderstandTaskVO> listImageUnderstandTasks(Long documentId) {
        return imageTaskStore.values().stream()
                .filter(task -> documentId == null || Objects.equals(task.documentId, documentId))
                .sorted(Comparator.comparing((ImageUnderstandTaskSnapshot t) -> t.submittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    private KbDocumentVO indexByText(KbDocumentEntity entity, String sourceText) {
        entity.setProcessStatus(STATUS_PROCESSING);
        entity.setFailReason(null);
        kbDocumentMapper.updateById(entity);

        try {
            List<String> chunks = splitText(sourceText);
            if (chunks.isEmpty()) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档切片结果为空");
            }

            pgJdbcTemplate.update("DELETE FROM kb_chunk WHERE document_id = ?", entity.getId());
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] vector = embedWithQwen(chunkText);
                String vectorLiteral = toVectorLiteral(vector);
                pgJdbcTemplate.update(
                        "INSERT INTO kb_chunk(document_id, chunk_index, content, embedding_model, embedding, created_at) " +
                                "VALUES (?, ?, ?, ?, ?::vector, now())",
                        entity.getId(), i, chunkText, entity.getEmbeddingModel(), vectorLiteral
                );
            }

            entity.setChunkCount(chunks.size());
            entity.setProcessStatus(STATUS_SUCCESS);
            entity.setFailReason(null);
            entity.setProcessedAt(LocalDateTime.now());
            kbDocumentMapper.updateById(entity);
            return toVO(entity);
        } catch (BusinessException ex) {
            entity.setProcessStatus(STATUS_FAILED);
            entity.setFailReason(ex.getMessage());
            entity.setProcessedAt(LocalDateTime.now());
            kbDocumentMapper.updateById(entity);
            throw ex;
        } catch (Exception ex) {
            entity.setProcessStatus(STATUS_FAILED);
            entity.setFailReason("向量化入库异常：" + ex.getMessage());
            entity.setProcessedAt(LocalDateTime.now());
            kbDocumentMapper.updateById(entity);
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化入库异常：" + ex.getMessage());
        }
    }

    @Override
    public KbDocumentVO getById(Long id) {
        return toVO(getRequiredDocument(id));
    }

    @Override
    public KbDocumentPageVO listPage(Long pageNum, Long pageSize) {
        long currentPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        long offset = (currentPageNum - 1) * currentPageSize;

        Long total = kbDocumentMapper.selectCount(new LambdaQueryWrapper<>());
        List<KbDocumentEntity> list = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocumentEntity>()
                .orderByDesc(KbDocumentEntity::getId)
                .last("limit " + offset + "," + currentPageSize));

        KbDocumentPageVO pageVO = new KbDocumentPageVO();
        pageVO.setPageNum(currentPageNum);
        pageVO.setPageSize(currentPageSize);
        pageVO.setTotal(total == null ? 0L : total);
        pageVO.setList(list.stream().map(this::toVO).collect(Collectors.toList()));
        return pageVO;
    }

    @Override
    public List<KbDocumentVO> listAll() {
        List<KbDocumentEntity> list = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocumentEntity>()
                .orderByDesc(KbDocumentEntity::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    private void validateUpload(KbDocumentUploadDTO dto, MultipartFile file) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "上传参数不能为空");
        }
        if (Objects.isNull(dto.getCategoryId())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "分类ID不能为空");
        }
        if (isBlank(dto.getTitle())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文档标题不能为空");
        }
        if (Objects.isNull(file) || file.isEmpty()) {
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

    private void validateStatus(String status) {
        if (!STATUS_PENDING.equals(status)
                && !STATUS_PROCESSING.equals(status)
                && !STATUS_SUCCESS.equals(status)
                && !STATUS_FAILED.equals(status)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "状态仅支持 PENDING/PROCESSING/SUCCESS/FAILED");
        }
    }

    private KbDocumentEntity getRequiredDocument(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文档ID不能为空");
        }
        KbDocumentEntity entity = kbDocumentMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "文档不存在");
        }
        return entity;
    }

    private String extractExt(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文件后缀不能为空");
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private KbDocumentVO toVO(KbDocumentEntity entity) {
        KbDocumentVO vo = new KbDocumentVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setTitle(entity.getTitle());
        vo.setOriginalFileName(entity.getOriginalFileName());
        vo.setFileExt(entity.getFileExt());
        vo.setContentType(entity.getContentType());
        vo.setFileSize(entity.getFileSize());
        vo.setStoragePath(entity.getStoragePath());
        vo.setProcessStatus(entity.getProcessStatus());
        vo.setFailReason(entity.getFailReason());
        vo.setRetryCount(entity.getRetryCount());
        vo.setEmbeddingModel(entity.getEmbeddingModel());
        vo.setChunkCount(entity.getChunkCount() == null ? 0 : entity.getChunkCount());
        vo.setParsedContent(entity.getParsedContent());
        vo.setImageUnderstandText(entity.getImageUnderstandText());
        vo.setParsedLength(entity.getParsedContent() == null ? 0 : entity.getParsedContent().length());
        vo.setProcessedAt(entity.getProcessedAt());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trim = value.trim();
        return trim.isEmpty() ? null : trim;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildImageUnderstandText(List<KbDocumentImagePreviewVO.Item> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (KbDocumentImagePreviewVO.Item item : items) {
            if (item == null || isBlank(item.getText())) {
                continue;
            }
            sb.append("【图片").append(item.getImageIndex()).append("】").append("\n");
            sb.append(item.getText().trim()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String mergeParsedAndImageText(String parsedText, String imageText) {
        String parsed = parsedText == null ? "" : parsedText.trim();
        String image = imageText == null ? "" : imageText.trim();
        if (parsed.isEmpty()) {
            return image;
        }
        if (image.isEmpty()) {
            return parsed;
        }
        return parsed + "\n\n" + "【图片理解补充】" + "\n" + image;
    }

    private void runPreviewTask(String taskId) {
        ImageUnderstandTaskSnapshot task = imageTaskStore.get(taskId);
        if (task == null) {
            return;
        }
        task.taskStatus = TASK_STATUS_PROCESSING;
        task.message = "图片理解处理中";
        task.startedAt = LocalDateTime.now();
        try {
            KbDocumentImagePreviewVO previewVO = previewImageUnderstand(task.documentId);
            task.taskStatus = TASK_STATUS_SUCCESS;
            task.message = "图片理解完成";
            task.imageCount = previewVO.getImageCount();
            task.mergedText = previewVO.getMergedText();
            task.finishedAt = LocalDateTime.now();
        } catch (Exception ex) {
            task.taskStatus = TASK_STATUS_FAILED;
            task.message = ex.getMessage();
            task.finishedAt = LocalDateTime.now();
        }
    }

    private void runIndexTask(String taskId) {
        ImageUnderstandTaskSnapshot task = imageTaskStore.get(taskId);
        if (task == null) {
            return;
        }
        task.taskStatus = TASK_STATUS_PROCESSING;
        task.message = "图片理解与向量化处理中";
        task.startedAt = LocalDateTime.now();
        try {
            KbDocumentEntity entity = getRequiredDocument(task.documentId);
            if (isBlank(entity.getParsedContent())) {
                parse(task.documentId);
                entity = getRequiredDocument(task.documentId);
            }

            String imageText = trimOrNull(task.manualImageText);
            if (isBlank(imageText)) {
                KbDocumentImagePreviewVO previewVO = previewImageUnderstand(task.documentId);
                imageText = trimOrNull(previewVO.getMergedText());
                task.imageCount = previewVO.getImageCount();
                task.mergedText = previewVO.getMergedText();
            } else {
                entity.setImageUnderstandText(imageText);
                kbDocumentMapper.updateById(entity);
                task.mergedText = imageText;
            }

            String merged = mergeParsedAndImageText(entity.getParsedContent(), imageText);
            KbDocumentVO result = indexByText(entity, merged);
            task.taskStatus = TASK_STATUS_SUCCESS;
            task.message = "图片文本一键向量化完成";
            task.document = result;
            task.finishedAt = LocalDateTime.now();
        } catch (Exception ex) {
            task.taskStatus = TASK_STATUS_FAILED;
            task.message = ex.getMessage();
            task.finishedAt = LocalDateTime.now();
        }
    }

    private KbImageUnderstandTaskVO toTaskVO(ImageUnderstandTaskSnapshot task) {
        KbImageUnderstandTaskVO vo = new KbImageUnderstandTaskVO();
        vo.setTaskId(task.taskId);
        vo.setDocumentId(task.documentId);
        vo.setTaskType(task.taskType);
        vo.setTaskStatus(task.taskStatus);
        vo.setMessage(task.message);
        vo.setImageCount(task.imageCount);
        vo.setMergedText(task.mergedText);
        vo.setDocument(task.document);
        vo.setSubmittedAt(task.submittedAt);
        vo.setStartedAt(task.startedAt);
        vo.setFinishedAt(task.finishedAt);
        return vo;
    }

    @PreDestroy
    public void shutdownTaskExecutor() {
        imageTaskExecutor.shutdown();
    }

    private static class ImageUnderstandTaskSnapshot {
        private String taskId;
        private Long documentId;
        private String taskType;
        private String taskStatus;
        private String message;
        private Integer imageCount;
        private String mergedText;
        private String manualImageText;
        private KbDocumentVO document;
        private LocalDateTime submittedAt;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }

    private List<String> splitText(String text) {
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < source.length()) {
            int end = Math.min(start + CHUNK_SIZE, source.length());
            String chunk = source.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= source.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量结果为空");
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
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
        if (!(dataObj instanceof List)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化返回缺少data");
        }
        List<?> dataList = (List<?>) dataObj;
        if (dataList.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化返回缺少data");
        }
        Object firstObj = dataList.get(0);
        if (!(firstObj instanceof Map)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果结构异常");
        }
        Map<?, ?> first = (Map<?, ?>) firstObj;
        Object embeddingObj = first.get("embedding");
        if (!(embeddingObj instanceof List)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果缺少embedding");
        }
        List<?> embeddingList = (List<?>) embeddingObj;
        if (embeddingList.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果缺少embedding");
        }
        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            Object numObj = embeddingList.get(i);
            if (!(numObj instanceof Number)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "向量化结果包含非数值元素");
            }
            Number num = (Number) numObj;
            result[i] = num.floatValue();
        }
        return result;
    }
}

