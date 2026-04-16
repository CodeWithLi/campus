package campus.ai.rag.service.service.admin.kb.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.kb.KbRagDebugDTO;
import campus.ai.rag.service.entity.kb.KbDocumentEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.kb.KbDocumentMapper;
import campus.ai.rag.service.service.admin.kb.AdminKbRagDebugService;
import campus.ai.rag.service.vo.admin.kb.KbRagDebugVO;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class AdminKbRagDebugServiceImpl implements AdminKbRagDebugService {

    private static final String EMBEDDING_MODEL = "text-embedding-v4";

    private final AiChatRouter aiChatRouter;
    private final RestTemplate restTemplate;
    private final JdbcTemplate pgJdbcTemplate;
    private final KbDocumentMapper kbDocumentMapper;
    private final String embeddingApiKey;
    private final String embeddingBaseUrl;
    private final Integer embeddingDimensions;

    public AdminKbRagDebugServiceImpl(AiChatRouter aiChatRouter,
                                     RestTemplate restTemplate,
                                     @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate,
                                     KbDocumentMapper kbDocumentMapper,
                                     @Value("${spring.ai.openai.api-key:}") String embeddingApiKey,
                                     @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String embeddingBaseUrl,
                                     @Value("${spring.ai.openai.embedding.options.dimensions:1024}") Integer embeddingDimensions) {
        this.aiChatRouter = aiChatRouter;
        this.restTemplate = restTemplate;
        this.pgJdbcTemplate = pgJdbcTemplate;
        this.kbDocumentMapper = kbDocumentMapper;
        this.embeddingApiKey = embeddingApiKey;
        this.embeddingBaseUrl = embeddingBaseUrl;
        this.embeddingDimensions = embeddingDimensions;
    }

    @Override
    public KbRagDebugVO debug(KbRagDebugDTO dto) {
        if (dto == null || isBlank(dto.getQuestion())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "question 不能为空");
        }
        int topK = (dto.getTopK() == null ? 5 : dto.getTopK());
        if (topK <= 0 || topK > 20) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "topK 范围仅支持 1~20");
        }

        float[] q = embedWithQwen(dto.getQuestion().trim());
        String qVector = toVectorLiteral(q);

        List<KbRagDebugVO.Hit> hits = searchTopK(qVector, dto.getCategoryIds(), topK);
        String ragContext = buildRagContext(hits);
        String modelCode = isBlank(dto.getModelCode()) ? aiChatRouter.getDefaultModelCode() : dto.getModelCode().trim();
        String answer = aiChatRouter.chat(modelCode, "RAG", dto.getQuestion().trim(), ragContext);

        KbRagDebugVO vo = new KbRagDebugVO();
        vo.setModelCode(modelCode);
        vo.setQuestion(dto.getQuestion().trim());
        vo.setAnswer(answer);
        vo.setHits(hits);
        boolean returnCtx = dto.getReturnRagContext() == null || Boolean.TRUE.equals(dto.getReturnRagContext());
        vo.setRagContext(returnCtx ? ragContext : null);
        return vo;
    }

    private List<KbRagDebugVO.Hit> searchTopK(String qVectorLiteral, List<Long> categoryIds, int topK) {
        // kb_chunk 在 PostgreSQL；kb_document 在 MySQL，不能在 PG 里 JOIN MySQL 表。
        // 方案：先在 MySQL 按分类取可用 documentId，再在 PG 用 document_id IN (...) 过滤检索，最后回填 MySQL 标题/分类信息。

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

        List<KbRagDebugVO.Hit> hits = pgJdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            KbRagDebugVO.Hit hit = new KbRagDebugVO.Hit();
            hit.setDocumentId(rs.getLong("document_id"));
            hit.setChunkIndex(rs.getInt("chunk_index"));
            hit.setContent(rs.getString("content"));
            hit.setDistance(rs.getDouble("distance"));
            return hit;
        });

        fillMysqlDocumentInfo(hits);
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

    private void fillMysqlDocumentInfo(List<KbRagDebugVO.Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        List<Long> docIds = hits.stream().map(KbRagDebugVO.Hit::getDocumentId).filter(Objects::nonNull).distinct().toList();
        if (docIds.isEmpty()) {
            return;
        }
        List<KbDocumentEntity> docs = kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocumentEntity>()
                .select(KbDocumentEntity::getId, KbDocumentEntity::getTitle, KbDocumentEntity::getCategoryId)
                .in(KbDocumentEntity::getId, docIds));
        Map<Long, KbDocumentEntity> map = (docs == null ? List.<KbDocumentEntity>of() : docs).stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(KbDocumentEntity::getId, d -> d, (a, b) -> a));
        for (KbRagDebugVO.Hit hit : hits) {
            if (hit == null) {
                continue;
            }
            KbDocumentEntity doc = map.get(hit.getDocumentId());
            if (doc != null) {
                hit.setDocumentTitle(doc.getTitle());
                hit.setCategoryId(doc.getCategoryId());
            }
        }
    }

    private String buildRagContext(List<KbRagDebugVO.Hit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (KbRagDebugVO.Hit hit : hits) {
            if (hit == null || isBlank(hit.getContent())) {
                continue;
            }
            sb.append("【文档#").append(hit.getDocumentId());
            if (!isBlank(hit.getDocumentTitle())) {
                sb.append(" ").append(hit.getDocumentTitle().trim());
            }
            sb.append(" | chunk=").append(hit.getChunkIndex());
            sb.append(" | dist=").append(String.format(Locale.ROOT, "%.4f", hit.getDistance()));
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
}

