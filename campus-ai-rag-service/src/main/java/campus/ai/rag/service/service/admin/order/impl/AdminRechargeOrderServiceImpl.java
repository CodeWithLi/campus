package campus.ai.rag.service.service.admin.order.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.wx.WxRechargeOrderEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxRechargeOrderMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.admin.order.AdminRechargeOrderService;
import campus.ai.rag.service.vo.admin.order.AdminRechargeOrderPageVO;
import campus.ai.rag.service.vo.admin.order.AdminRechargeOrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminRechargeOrderServiceImpl implements AdminRechargeOrderService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WxRechargeOrderMapper wxRechargeOrderMapper;
    private final WxUserMapper wxUserMapper;

    public AdminRechargeOrderServiceImpl(WxRechargeOrderMapper wxRechargeOrderMapper, WxUserMapper wxUserMapper) {
        this.wxRechargeOrderMapper = wxRechargeOrderMapper;
        this.wxUserMapper = wxUserMapper;
    }

    @Override
    public AdminRechargeOrderPageVO listPage(Long pageNum,
                                             Long pageSize,
                                             String orderNo,
                                             Long userId,
                                             String status,
                                             String channel,
                                             String startTime,
                                             String endTime) {
        long pNum = (pageNum == null || pageNum < 1) ? 1L : pageNum;
        long pSize = (pageSize == null || pageSize < 1) ? 20L : pageSize;
        pSize = Math.min(pSize, 200L);

        LambdaQueryWrapper<WxRechargeOrderEntity> qw = new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .orderByDesc(WxRechargeOrderEntity::getId);
        if (!isBlank(orderNo)) {
            qw.like(WxRechargeOrderEntity::getOrderNo, orderNo.trim());
        }
        if (userId != null) {
            qw.eq(WxRechargeOrderEntity::getUserId, userId);
        }
        if (!isBlank(status)) {
            qw.eq(WxRechargeOrderEntity::getStatus, status.trim().toUpperCase());
        }
        if (!isBlank(channel)) {
            qw.eq(WxRechargeOrderEntity::getChannel, channel.trim());
        }
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        if (st != null && et != null) {
            qw.between(WxRechargeOrderEntity::getCreatedAt, st, et);
        } else if (st != null) {
            qw.ge(WxRechargeOrderEntity::getCreatedAt, st);
        } else if (et != null) {
            qw.le(WxRechargeOrderEntity::getCreatedAt, et);
        }

        Page<WxRechargeOrderEntity> page = wxRechargeOrderMapper.selectPage(new Page<>(pNum, pSize), qw);
        List<WxRechargeOrderEntity> records = page.getRecords();
        Map<Long, WxUserEntity> userMap = buildUserMap(records);
        List<AdminRechargeOrderVO> list = records.stream()
                .filter(Objects::nonNull)
                .map(e -> toVO(e, userMap.get(e.getUserId())))
                .toList();

        AdminRechargeOrderPageVO vo = new AdminRechargeOrderPageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(list);
        return vo;
    }

    @Override
    public AdminRechargeOrderVO getById(Long id) {
        if (id == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "订单ID不能为空");
        }
        WxRechargeOrderEntity entity = wxRechargeOrderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "订单不存在");
        }
        WxUserEntity user = entity.getUserId() == null ? null : wxUserMapper.selectById(entity.getUserId());
        return toVO(entity, user);
    }

    private Map<Long, WxUserEntity> buildUserMap(List<WxRechargeOrderEntity> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = records.stream()
                .map(WxRechargeOrderEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return wxUserMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(WxUserEntity::getId, Function.identity(), (a, b) -> a));
    }

    private AdminRechargeOrderVO toVO(WxRechargeOrderEntity entity, WxUserEntity user) {
        AdminRechargeOrderVO vo = new AdminRechargeOrderVO();
        vo.setId(entity.getId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setUserId(entity.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setChannel(entity.getChannel());
        vo.setAmountYuan(entity.getAmountYuan());
        vo.setPoints(entity.getPoints());
        vo.setSubject(entity.getSubject());
        vo.setStatus(entity.getStatus());
        vo.setTradeNo(entity.getTradeNo());
        vo.setBuyerLogonId(entity.getBuyerLogonId());
        vo.setPaidAt(entity.getPaidAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
