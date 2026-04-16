package campus.ai.rag.service.service.wx.rag.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxRagDTO;
import campus.ai.rag.service.entity.kb.KbDocumentEntity;
import campus.ai.rag.service.entity.kb.WxKbDocumentEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.kb.KbDocumentMapper;
import campus.ai.rag.service.mapper.kb.WxKbDocumentMapper;
import campus.ai.rag.service.service.wx.rag.WxRagService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxRagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WxRagServiceImpl implements WxRagService {

    private static final String EMBEDDING_MODEL = "text-embedding-v4";
    private static final String SCOPE_SYSTEM = "SYSTEM";
    private static final String SCOPE_USER = "USER";

    private final AiChatRouter aiChatRouter;
    private final RestTemplate restTemplate;
    private final JdbcTemplate pgJdbcTemplate;
    private final KbDocumentMapper kbDocumentMapper;
    private final WxKbDocumentMapper wxKbDocumentMapper;
    private final String embeddingApiKey;
    private final String embeddingBaseUrl;
    private final Integer embeddingDimensions;

    public WxRagServiceImpl(AiChatRouter aiChatRouter,
                            RestTemplate restTemplate,
                            @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
                            KbDocumentMapper kbDocumentMapper,
                            WxKbDocumentMapper wxKbDocumentMapper,
                            @Value("${spring.ai.openai.api-key:}") String embeddingApiKey,
                            @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String embeddingBaseUrl,
                            @Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer embeddingDimensions) {
        this.aiChatRouter = aiChatRouter;
        this.restTemplate = restTemplate;
        this.pgJdbcTemplate = pgJdbcTemplate;
        this.kbDocumentMapper = kbDocumentMapper;
        this.wxKbDocumentMapper = wxKbDocumentMapper;
        this.embeddingApiKey = embeddingApiKey;
        this.embeddingBaseUrl = embeddingBaseUrl;
        this.embeddingDimensions = embeddingDimensions;
    }

    @Override
    public WxRagVO rag(WxRagDTO dto) {
        if (dto == null || isBlank(dto.getQuestion())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "question 不能为空");
        }
        int topK = dto.getTopK() == null ? 5 : dto.getTopK();
        if (topK <= 0 || topK > 20) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "topK 范围仅支持 1~20");
        }

        float[] q = embedWithQwen(dto.getQuestion().trim());
        String qVector = toVectorLiteral(q);

        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        List<WxRagVO.Hit> hits = searchTopK(userId, qVector, dto.getCategoryIds(), topK);
        String ragContext = buildRagContext(hits);

        String modelCode = isBlank(dto.getModelCode()) ? aiChatRouter.getDefaultModelCode() : dto.getModelCode().trim();
        String answer = aiChatRouter.chat(modelCode, "RAG", dto.getQuestion().trim(), ragContext);

        WxRagVO vo = new WxRagVO();
        vo.setModelCode(modelCode);
        vo.setAnswer(answer);
        vo.setHits(hits);
        return vo;
    }

    private List<WxRagVO.Hit> searchTopK(Long userId, String qVectorLiteral, List<Long> categoryIds, int topK) {
        int systemTopK = calcSystemTopK(topK);
        int userTopK = Math.max(0, topK - systemTopK);

        List<WxRagVO.Hit> systemHits = searchSystemTopK(qVectorLiteral, categoryIds, systemTopK);
        List<WxRagVO.Hit> userHits = userTopK <= 0 ? List.of() : searchUserTopK(userId, qVectorLiteral, userTopK);

        List<WxRagVO.Hit> merged = new ArrayList<>(systemHits.size() + userHits.size());
        merged.addAll(systemHits);
        merged.addAll(userHits);
        merged.sort(Comparator.comparingDouble(h -> h.getDistance() == null ? Double.MAX_VALUE : h.getDistance()));
        if (merged.size() > topK) {
            merged = merged.subList(0, topK);
        }
        return merged;
    }

    private int calcSystemTopK(int topK) {
        if (topK <= 1) return 1;
        // 系统库取多一点（约70%），个人库取少一点（剩余）
        int system = (int) Math.ceil(topK * 0.7d);
        system = Math.max(1, Math.min(system, topK));
        return system;
    }

    private List<WxRagVO.Hit> searchSystemTopK(String qVectorLiteral, List<Long> categoryIds, int topK) {
        if (topK <= 0) return List.of();
        List<Long> allowedDocIds = resolveAllowedDocIds(categoryIds);
        if (allowedDocIds != null && allowedDocIds.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.document_id, c.chunk_index, c.content, ");
        sql.append("(c.embedding <-> ?::vector) AS distance ");
        sql.append("FROM kb_chunk c ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        params.add(qVectorLiteral);

        if (allowedDocIds != null) {
            sql.append("AND c.document_id IN (");
            StringJoiner joiner = new StringJoiner(",");
            for (Long id : allowedDocIds) {
                joiner.add("?");
                params.add(id);
            }
            sql.append(joiner).append(") ");
        }

        sql.append("ORDER BY c.embedding <-> ?::vector ");
        sql.append("LIMIT ? ");
        params.add(qVectorLiteral);
        params.add(topK);

        List<WxRagVO.Hit> hits = pgJdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            WxRagVO.Hit hit = new WxRagVO.Hit();
            hit.setScope(SCOPE_SYSTEM);
            hit.setDocumentId(rs.getLong("document_id"));
            hit.setChunkIndex(rs.getInt("chunk_index"));
            hit.setContent(rs.getString("content"));
            hit.setDistance(rs.getDouble("distance"));
            return hit;
        });
        fillSystemMysqlDocumentTitle(hits);
        return hits;
    }

    private List<WxRagVO.Hit> searchUserTopK(Long userId, String qVectorLiteral, int topK) {
        if (topK <= 0) return List.of();
        if (userId == null || userId <= 0) {
            // 未识别到用户时，不做个人库检索，避免越权
            return List.of();
        }

        String sql = ""
                + "SELECT c.document_id, c.chunk_index, c.content, "
                + "(c.embedding <-> ?::vector) AS distance "
                + "FROM wx_kb_chunk c "
                + "WHERE c.user_id = ? "
                + "ORDER BY c.embedding <-> ?::vector "
                + "LIMIT ? ";

        List<WxRagVO.Hit> hits = pgJdbcTemplate.query(sql, new Object[]{qVectorLiteral, userId, qVectorLiteral, topK}, (rs, rowNum) -> {
            WxRagVO.Hit hit = new WxRagVO.Hit();
            hit.setScope(SCOPE_USER);
            hit.setDocumentId(rs.getLong("document_id"));
            hit.setChunkIndex(rs.getInt("chunk_index"));
            hit.setContent(rs.getString("content"));
            hit.setDistance(rs.getDouble("distance"));
            return hit;
        });
        fillUserMysqlDocumentTitle(userId, hits);
        return hits;
    }

    /**
     * @return null 表示不做过滤；空集合表示过滤后无文档
     */
    private List<Long> resolveAllowedDocIds(List<Long> categoryIds) {
        if (categoryIds == null) {
            return null;
        }
        List<Long> filtered = categoryIds.stream().filter(Objects::nonNull).distinct().toList();
        if (filtered.isEmpty()) {
            return null;
        }
        List<KbDocumentEntity> docs = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocumentEntity>()
                .select(KbDocumentEntity::getId)
                .in(KbDocumentEntity::getCategoryId, filtered));
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        return docs.stream().map(KbDocumentEntity::getId).filter(Objects::nonNull).distinct().toList();
    }

    private void fillSystemMysqlDocumentTitle(List<WxRagVO.Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        List<Long> docIds = hits.stream()
                .filter(h -> h != null && SCOPE_SYSTEM.equals(h.getScope()))
                .map(WxRagVO.Hit::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (docIds.isEmpty()) {
            return;
        }
        List<KbDocumentEntity> docs = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocumentEntity>()
                .select(KbDocumentEntity::getId, KbDocumentEntity::getTitle)
                .in(KbDocumentEntity::getId, docIds));
        Map<Long, String> map = (docs == null ? List.<KbDocumentEntity>of() : docs).stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(KbDocumentEntity::getId, KbDocumentEntity::getTitle, (a, b) -> a));
        for (WxRagVO.Hit hit : hits) {
            if (hit == null || !SCOPE_SYSTEM.equals(hit.getScope())) {
                continue;
            }
            hit.setDocumentTitle(map.get(hit.getDocumentId()));
        }
    }

    private void fillUserMysqlDocumentTitle(Long userId, List<WxRagVO.Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        if (userId == null || userId <= 0) {
            return;
        }
        List<Long> docIds = hits.stream().map(WxRagVO.Hit::getDocumentId).filter(Objects::nonNull).distinct().toList();
        if (docIds.isEmpty()) {
            return;
        }
        List<WxKbDocumentEntity> docs = wxKbDocumentMapper.selectList(new LambdaQueryWrapper<WxKbDocumentEntity>()
                .select(WxKbDocumentEntity::getId, WxKbDocumentEntity::getTitle)
                .eq(WxKbDocumentEntity::getUserId, userId)
                .in(WxKbDocumentEntity::getId, docIds));
        Map<Long, String> map = (docs == null ? List.<WxKbDocumentEntity>of() : docs).stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(WxKbDocumentEntity::getId, WxKbDocumentEntity::getTitle, (a, b) -> a));
        for (WxRagVO.Hit hit : hits) {
            if (hit == null) {
                continue;
            }
            hit.setDocumentTitle(map.get(hit.getDocumentId()));
        }
    }

    private String buildRagContext(List<WxRagVO.Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (WxRagVO.Hit hit : hits) {
            if (hit == null || isBlank(hit.getContent())) {
                continue;
            }
            sb.append("【");
            if (SCOPE_USER.equals(hit.getScope())) {
                sb.append("个人库 ");
            } else {
                sb.append("系统库 ");
            }
            sb.append("文档#").append(hit.getDocumentId());
            if (!isBlank(hit.getDocumentTitle())) {
                sb.append(" ").append(hit.getDocumentTitle().trim());
            }
            sb.append(" | chunk=").append(hit.getChunkIndex());
            sb.append(" | dist=").append(String.format(java.util.Locale.ROOT, "%.4f", hit.getDistance()));
            sb.append("】\n");
            sb.append(hit.getContent().trim()).append("\n\n");
        }
        return sb.toString().trim();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
}

