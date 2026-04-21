package campus.ai.rag.service.service.admin.model.impl;

import campus.ai.rag.service.entity.model.AiTokenBillingLogEntity;
import campus.ai.rag.service.mapper.model.AiTokenBillingLogMapper;
import campus.ai.rag.service.service.admin.model.AdminTokenBillingLogService;
import campus.ai.rag.service.vo.admin.model.AiCallTrendPointVO;
import campus.ai.rag.service.vo.admin.model.AiModelUsageShareVO;
import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogPageVO;
import campus.ai.rag.service.vo.admin.model.AiTokenConsumptionPointVO;
import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminTokenBillingLogServiceImpl implements AdminTokenBillingLogService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AiTokenBillingLogMapper aiTokenBillingLogMapper;

    public AdminTokenBillingLogServiceImpl(AiTokenBillingLogMapper aiTokenBillingLogMapper) {
        this.aiTokenBillingLogMapper = aiTokenBillingLogMapper;
    }

    @Override
    public AiTokenBillingLogPageVO listPage(Long pageNum,
                                             Long pageSize,
                                             Long userId,
                                             String scene,
                                             String modelCode,
                                             Integer status,
                                             String startTime,
                                             String endTime) {
        long pNum = (pageNum == null || pageNum < 1) ? 1L : pageNum;
        long pSize = (pageSize == null || pageSize < 1) ? 20L : pageSize;
        pSize = Math.min(pSize, 200L);

        LambdaQueryWrapper<AiTokenBillingLogEntity> qw = new LambdaQueryWrapper<AiTokenBillingLogEntity>()
                .orderByDesc(AiTokenBillingLogEntity::getCreatedAt)
                .orderByDesc(AiTokenBillingLogEntity::getId);

        if (userId != null) {
            qw.eq(AiTokenBillingLogEntity::getUserId, userId);
        }
        if (!isBlank(scene)) {
            qw.eq(AiTokenBillingLogEntity::getScene, scene.trim().toUpperCase());
        }
        if (!isBlank(modelCode)) {
            qw.eq(AiTokenBillingLogEntity::getModelCode, modelCode.trim());
        }
        if (status != null) {
            qw.eq(AiTokenBillingLogEntity::getStatus, status);
        }

        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        if (st != null && et != null) {
            qw.between(AiTokenBillingLogEntity::getCreatedAt, st, et);
        } else if (st != null) {
            qw.ge(AiTokenBillingLogEntity::getCreatedAt, st);
        } else if (et != null) {
            qw.le(AiTokenBillingLogEntity::getCreatedAt, et);
        }

        Page<AiTokenBillingLogEntity> page = aiTokenBillingLogMapper.selectPage(new Page<>(pNum, pSize), qw);
        List<AiTokenBillingLogVO> list = page.getRecords()
                .stream()
                .filter(Objects::nonNull)
                .map(this::toVO)
                .collect(Collectors.toList());

        AiTokenBillingLogPageVO vo = new AiTokenBillingLogPageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(list);
        return vo;
    }

    private AiTokenBillingLogVO toVO(AiTokenBillingLogEntity e) {
        AiTokenBillingLogVO vo = new AiTokenBillingLogVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setScene(e.getScene());
        vo.setModelCode(e.getModelCode());
        vo.setInvokeType(e.getInvokeType());
        vo.setBillingType(e.getBillingType());
        vo.setPromptTokens(e.getPromptTokens());
        vo.setCompletionTokens(e.getCompletionTokens());
        vo.setTotalTokens(e.getTotalTokens());
        vo.setConsumedPoints(e.getConsumedPoints());
        vo.setElapsedMs(e.getElapsedMs());
        vo.setStatus(e.getStatus());
        vo.setErrorMessage(e.getErrorMessage());
        vo.setCreatedAt(e.getCreatedAt());
        return vo;
    }

    @Override
    public List<AiCallTrendPointVO> callTrend(String granularity, String startTime, String endTime) {
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        List<Map<String, Object>> rows = aiTokenBillingLogMapper.selectCallTrend(
                toBucketExpr(normalizeGranularity(granularity)),
                st,
                et
        );
        List<AiCallTrendPointVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AiCallTrendPointVO item = new AiCallTrendPointVO();
            item.setBucket(String.valueOf(row.get("bucket")));
            item.setCallCount(toLong(row.get("callCount")));
            item.setSuccessRate(toDouble(row.get("successRate")));
            item.setAvgElapsedMs(toLong(row.get("avgElapsedMs")));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<AiTokenConsumptionPointVO> tokenConsumption(String granularity, String startTime, String endTime) {
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        List<Map<String, Object>> rows = aiTokenBillingLogMapper.selectTokenConsumption(
                toBucketExpr(normalizeGranularity(granularity)),
                st,
                et
        );
        List<AiTokenConsumptionPointVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            AiTokenConsumptionPointVO item = new AiTokenConsumptionPointVO();
            item.setBucket(String.valueOf(row.get("bucket")));
            item.setPromptTokens(toLong(row.get("promptTokens")));
            item.setCompletionTokens(toLong(row.get("completionTokens")));
            item.setTotalTokens(toLong(row.get("totalTokens")));
            item.setConsumedPoints(toBigDecimal(row.get("consumedPoints")));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<AiModelUsageShareVO> modelUsageShare(String startTime, String endTime) {
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        List<Map<String, Object>> rows = aiTokenBillingLogMapper.selectModelUsageShare(st, et);
        long total = rows.stream().mapToLong(row -> toLong(row.get("callCount"))).sum();
        List<AiModelUsageShareVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long callCount = toLong(row.get("callCount"));
            AiModelUsageShareVO item = new AiModelUsageShareVO();
            item.setModelCode(String.valueOf(row.get("modelCode")));
            item.setCallCount(callCount);
            item.setUsageRate(total <= 0 ? 0d : round2((callCount * 100d) / total));
            result.add(item);
        }
        return result;
    }

    private LocalDateTime parseDateTime(String v) {
        if (isBlank(v)) {
            return null;
        }
        try {
            return LocalDateTime.parse(v.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // 前端传参可能为空或格式不对：直接忽略该条件
            return null;
        }
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private String normalizeGranularity(String granularity) {
        if ("hour".equalsIgnoreCase(granularity)) {
            return "hour";
        }
        return "day";
    }

    private String toBucketExpr(String granularity) {
        if ("hour".equals(granularity)) {
            return "DATE_FORMAT(created_at, '%Y-%m-%d %H:00')";
        }
        return "DATE_FORMAT(created_at, '%Y-%m-%d')";
    }

    private Long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private Double toDouble(Object v) {
        if (v == null) {
            return 0d;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception ignore) {
            return 0d;
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception ignore) {
            return BigDecimal.ZERO;
        }
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}

