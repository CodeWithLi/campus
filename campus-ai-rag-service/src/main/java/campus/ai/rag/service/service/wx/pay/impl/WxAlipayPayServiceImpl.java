package campus.ai.rag.service.service.wx.pay.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxAlipayCreateOrderDTO;
import campus.ai.rag.service.entity.wx.WxRechargeOrderEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxRechargeOrderMapper;
import campus.ai.rag.service.mapper.wx.WxPowerPointCreditLogMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.properties.AlipayProperties;
import campus.ai.rag.service.service.wx.pay.WxAlipayPayService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxAlipayCreateOrderVO;
import campus.ai.rag.service.vo.wx.WxRechargeOrderVO;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class WxAlipayPayServiceImpl implements WxAlipayPayService {

    private static final String CHANNEL = "ALIPAY_WAP";
    private static final String PRODUCT_CODE = "QUICK_WAP_WAY";
    private static final BigDecimal POINTS_PER_YUAN = new BigDecimal("100");
    private static final Set<BigDecimal> ALLOWED_AMOUNTS = Set.of(
            new BigDecimal("1.00"),
            new BigDecimal("30.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00")
    );

    private final WxRechargeOrderMapper wxRechargeOrderMapper;
    private final WxPowerPointCreditLogMapper wxPowerPointCreditLogMapper;
    private final WxUserMapper wxUserMapper;
    private final AlipayProperties alipayProperties;

    public WxAlipayPayServiceImpl(WxRechargeOrderMapper wxRechargeOrderMapper,
                                  WxPowerPointCreditLogMapper wxPowerPointCreditLogMapper,
                                  WxUserMapper wxUserMapper,
                                  AlipayProperties alipayProperties) {
        this.wxRechargeOrderMapper = wxRechargeOrderMapper;
        this.wxPowerPointCreditLogMapper = wxPowerPointCreditLogMapper;
        this.wxUserMapper = wxUserMapper;
        this.alipayProperties = alipayProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WxAlipayCreateOrderVO createWapOrder(WxAlipayCreateOrderDTO dto) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        BigDecimal amountYuan = normalizeAmount(dto == null ? null : dto.getAmountYuan());
        if (!ALLOWED_AMOUNTS.contains(amountYuan)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持充值 1/30/50/100 元");
        }
        BigDecimal points = amountYuan.multiply(POINTS_PER_YUAN).setScale(2, RoundingMode.HALF_UP);
        String orderNo = buildOrderNo();

        WxRechargeOrderEntity order = new WxRechargeOrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setChannel(CHANNEL);
        order.setAmountYuan(amountYuan);
        order.setPoints(points);
        order.setSubject("校园AI算力点充值");
        order.setStatus("CREATED");
        wxRechargeOrderMapper.insert(order);

        String payUrl = buildPayUrl(orderNo, amountYuan, order.getSubject());
        WxAlipayCreateOrderVO vo = new WxAlipayCreateOrderVO();
        vo.setOrderNo(orderNo);
        vo.setAmountYuan(amountYuan);
        vo.setPoints(points);
        vo.setPayUrl(payUrl);
        return vo;
    }

    @Override
    public String buildPayUrlByOrderNo(String orderNo) {
        if (isBlank(orderNo)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "订单号不能为空");
        }
        WxRechargeOrderEntity order = wxRechargeOrderMapper.selectOne(new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .eq(WxRechargeOrderEntity::getOrderNo, orderNo.trim())
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "订单不存在");
        }
        if (!"CREATED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "该订单状态不可支付");
        }
        return buildPayUrl(order.getOrderNo(), order.getAmountYuan(), order.getSubject());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            log.warn("[ALIPAY_NOTIFY] empty params");
            return "failure";
        }
        boolean signOk = verifySign(params);
        if (!signOk) {
            log.warn("[ALIPAY_NOTIFY] sign verify failed, out_trade_no={}", params.get("out_trade_no"));
            return "failure";
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        if (isBlank(outTradeNo) || isBlank(tradeStatus)) {
            log.warn("[ALIPAY_NOTIFY] missing key params, out_trade_no={}, trade_status={}", outTradeNo, tradeStatus);
            return "failure";
        }
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("[ALIPAY_NOTIFY] ignore trade status, out_trade_no={}, trade_status={}", outTradeNo, tradeStatus);
            return "success";
        }

        WxRechargeOrderEntity order = wxRechargeOrderMapper.selectOne(new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .eq(WxRechargeOrderEntity::getOrderNo, outTradeNo)
                .last("limit 1"));
        if (order == null) {
            log.warn("[ALIPAY_NOTIFY] order not found, out_trade_no={}", outTradeNo);
            return "failure";
        }
        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            log.info("[ALIPAY_NOTIFY] order already paid, out_trade_no={}", outTradeNo);
            return "success";
        }

        int changed = wxRechargeOrderMapper.markPaidIfCreated(
                outTradeNo,
                params.get("trade_no"),
                params.get("buyer_logon_id"),
                parseNotifyTime(params.get("gmt_payment"))
        );
        if (changed > 0) {
            creditPointsOnce(order);
            log.info("[ALIPAY_NOTIFY] paid success, out_trade_no={}, trade_no={}, add_points={}",
                    outTradeNo, params.get("trade_no"), order.getPoints());
        } else {
            log.warn("[ALIPAY_NOTIFY] mark paid skipped, out_trade_no={}, current_status={}", outTradeNo, order.getStatus());
        }
        return "success";
    }

    @Override
    public WxRechargeOrderVO getMyOrder(String orderNo) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxRechargeOrderEntity order = wxRechargeOrderMapper.selectOne(new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .eq(WxRechargeOrderEntity::getUserId, userId)
                .eq(WxRechargeOrderEntity::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "订单不存在");
        }
        return toVO(order);
    }

    @Override
    public List<WxRechargeOrderVO> listMyOrders() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        List<WxRechargeOrderEntity> list = wxRechargeOrderMapper.selectList(new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .eq(WxRechargeOrderEntity::getUserId, userId)
                .orderByDesc(WxRechargeOrderEntity::getId));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WxRechargeOrderVO queryAndSyncMyOrder(String orderNo) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxRechargeOrderEntity order = wxRechargeOrderMapper.selectOne(new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .eq(WxRechargeOrderEntity::getUserId, userId)
                .eq(WxRechargeOrderEntity::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "订单不存在");
        }
        if ("PAID".equalsIgnoreCase(order.getStatus()) || "CLOSED".equalsIgnoreCase(order.getStatus())) {
            return toVO(order);
        }

        try {
            AlipayClient client = new DefaultAlipayClient(buildAlipayConfig());
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(order.getOrderNo());
            request.setBizModel(model);
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                log.warn("[ALIPAY_QUERY] query failed, order_no={}, code={}, sub_msg={}",
                        order.getOrderNo(), response.getCode(), response.getSubMsg());
                return toVO(order);
            }

            String tradeStatus = response.getTradeStatus();
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                int changed = wxRechargeOrderMapper.markPaidIfCreated(
                        order.getOrderNo(),
                        response.getTradeNo(),
                        response.getBuyerLogonId(),
                        parseNotifyTime(response.getSendPayDate())
                );
                if (changed > 0) {
                    creditPointsOnce(order);
                    order.setStatus("PAID");
                    order.setTradeNo(response.getTradeNo());
                    order.setBuyerLogonId(response.getBuyerLogonId());
                    order.setPaidAt(parseNotifyTime(response.getSendPayDate()));
                    log.info("[ALIPAY_QUERY] sync paid success, order_no={}, trade_no={}", order.getOrderNo(), response.getTradeNo());
                }
            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                int changed = wxRechargeOrderMapper.markClosedIfCreated(order.getOrderNo());
                if (changed > 0) {
                    order.setStatus("CLOSED");
                }
            }
        } catch (AlipayApiException e) {
            log.warn("[ALIPAY_QUERY] exception, order_no={}, msg={}", order.getOrderNo(), e.getMessage());
        }
        WxRechargeOrderEntity latest = wxRechargeOrderMapper.selectOne(new LambdaQueryWrapper<WxRechargeOrderEntity>()
                .eq(WxRechargeOrderEntity::getId, order.getId())
                .last("limit 1"));
        return toVO(latest == null ? order : latest);
    }

    /**
     * 强幂等：同一 order_no 只能加点一次。
     */
    private void creditPointsOnce(WxRechargeOrderEntity order) {
        if (order == null || order.getUserId() == null || isBlank(order.getOrderNo())) {
            return;
        }
        int inserted = wxPowerPointCreditLogMapper.insertIgnore(
                order.getUserId(),
                order.getOrderNo(),
                CHANNEL,
                order.getPoints()
        );
        if (inserted <= 0) {
            log.warn("[ALIPAY_CREDIT] skip duplicated credit, order_no={}", order.getOrderNo());
            return;
        }
        wxUserMapper.addPowerPoints(order.getUserId(), order.getPoints());
    }

    private String buildPayUrl(String orderNo, BigDecimal amountYuan, String subject) {
        try {
            AlipayClient client = new DefaultAlipayClient(buildAlipayConfig());
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setReturnUrl(alipayProperties.getReturnUrl());

            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setOutTradeNo(orderNo);
            model.setTotalAmount(amountYuan.setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setSubject(subject);
            model.setProductCode(PRODUCT_CODE);
            request.setBizModel(model);

            AlipayTradeWapPayResponse response = client.pageExecute(request, "GET");
            if (!response.isSuccess() || isBlank(response.getBody())) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "创建支付宝订单失败");
            }
            return response.getBody();
        } catch (AlipayApiException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "创建支付宝订单失败：" + e.getMessage());
        }
    }

    private AlipayConfig buildAlipayConfig() {
        AlipayConfig cfg = new AlipayConfig();
        cfg.setServerUrl(alipayProperties.getGatewayUrl());
        cfg.setAppId(alipayProperties.getAppId());
        cfg.setPrivateKey(readClassPathText(alipayProperties.getPrivateKeyPath()));
        cfg.setFormat(alipayProperties.getFormat());
        cfg.setCharset(alipayProperties.getCharset());
        cfg.setSignType(alipayProperties.getSignType());
        cfg.setAlipayPublicKey(readClassPathText(alipayProperties.getAlipayPublicKeyPath()));
        return cfg;
    }

    private boolean verifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    readClassPathText(alipayProperties.getAlipayPublicKeyPath()),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
        } catch (AlipayApiException e) {
            return false;
        }
    }

    private String readClassPathText(String path) {
        if (isBlank(path)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "支付宝密钥文件未配置");
        }
        try {
            ClassPathResource resource = new ClassPathResource(path);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8).replaceAll("\\s+", "");
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "读取支付宝密钥文件失败：" + path);
        }
    }

    private LocalDateTime parseNotifyTime(String value) {
        if (isBlank(value)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private LocalDateTime parseNotifyTime(Date value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        try {
            Instant instant = value.toInstant();
            return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private WxRechargeOrderVO toVO(WxRechargeOrderEntity order) {
        WxRechargeOrderVO vo = new WxRechargeOrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setChannel(order.getChannel());
        vo.setAmountYuan(order.getAmountYuan());
        vo.setPoints(order.getPoints());
        vo.setStatus(order.getStatus());
        vo.setPaidAt(order.getPaidAt());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private BigDecimal normalizeAmount(BigDecimal amountYuan) {
        if (amountYuan == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "充值金额不能为空");
        }
        return amountYuan.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildOrderNo() {
        return "ALI" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private Long parseUserId(String wxUserId) {
        try {
            return Long.parseLong(wxUserId);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

