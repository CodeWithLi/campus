package campus.ai.rag.service.service.admin.model.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.model.AiTokenBillingLogEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiTokenBillingLogMapper;
import campus.ai.rag.service.service.admin.model.AdminTokenBillingLogService;
import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogPageVO;
import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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
        vo.setStatus(e.getStatus());
        vo.setErrorMessage(e.getErrorMessage());
        vo.setCreatedAt(e.getCreatedAt());
        return vo;
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
}

