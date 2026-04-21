package campus.ai.rag.service.service.wx.reminder.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxNoticeAgentCreateDTO;
import campus.ai.rag.service.dto.wx.WxScheduleReminderCreateDTO;
import campus.ai.rag.service.entity.forum.ForumNoticeAgentRecordEntity;
import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumNoticeAgentRecordMapper;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.service.admin.kb.ImageUnderstandService;
import campus.ai.rag.service.service.wx.reminder.WxNoticeAgentService;
import campus.ai.rag.service.service.wx.reminder.WxScheduleReminderService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxNoticeAgentParseVO;
import campus.ai.rag.service.vo.wx.WxNoticeAgentRecordVO;
import campus.ai.rag.service.vo.wx.WxScheduleReminderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.TimeZone;

@Service
public class WxNoticeAgentServiceImpl implements WxNoticeAgentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(WxNoticeAgentServiceImpl.class);
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})\\s*[-~至到]\\s*(\\d{1,2}:\\d{2})");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}[年\\-/.]\\d{1,2}[月\\-/.]\\d{1,2}日?).{0,10}(\\d{4}[年\\-/.]\\d{1,2}[月\\-/.]\\d{1,2}日?)");
    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(\\d{4})[年\\-/.](\\d{1,2})[月\\-/.](\\d{1,2})日?");
    private static final Pattern MD_DATE_PATTERN = Pattern.compile("(\\d{1,2})[\\-/.月](\\d{1,2})日?");

    private final RestTemplate restTemplate;
    private final AiChatRouter aiChatRouter;
    private final WxScheduleReminderService wxScheduleReminderService;
    private final ForumNoticeAgentRecordMapper forumNoticeAgentRecordMapper;
    private final ImageUnderstandService imageUnderstandService;
    private final AiModelConfigMapper aiModelConfigMapper;

    @Value("${xf.ocr.general.host:cbm01.cn-huabei-1.xf-yun.com}")
    private String generalOcrHost;
    @Value("${xf.ocr.general.sign-host:api.xf-yun.com}")
    private String generalOcrSignHost;
    @Value("${xf.ocr.general.path:/v1/private/se75ocrbm}")
    private String generalOcrPath;
    @Value("${xf.ocr.general.app-id:}")
    private String generalOcrAppId;
    @Value("${xf.ocr.general.api-key:}")
    private String generalOcrApiKey;
    @Value("${xf.ocr.general.api-secret:}")
    private String generalOcrApiSecret;

    @Value("${xf.ocr.pdf.app-id:}")
    private String pdfOcrAppId;
    @Value("${xf.ocr.pdf.api-secret:}")
    private String pdfOcrApiSecret;
    @Value("${xf.ocr.pdf.start-url:https://iocr.xfyun.cn/ocrzdq/v1/pdfOcr/start}")
    private String pdfOcrStartUrl;
    @Value("${xf.ocr.pdf.status-url:https://iocr.xfyun.cn/ocrzdq/v1/pdfOcr/status}")
    private String pdfOcrStatusUrl;

    public WxNoticeAgentServiceImpl(RestTemplate restTemplate,
                                    AiChatRouter aiChatRouter,
                                    WxScheduleReminderService wxScheduleReminderService,
                                    ForumNoticeAgentRecordMapper forumNoticeAgentRecordMapper,
                                    ImageUnderstandService imageUnderstandService,
                                    AiModelConfigMapper aiModelConfigMapper) {
        this.restTemplate = restTemplate;
        this.aiChatRouter = aiChatRouter;
        this.wxScheduleReminderService = wxScheduleReminderService;
        this.forumNoticeAgentRecordMapper = forumNoticeAgentRecordMapper;
        this.imageUnderstandService = imageUnderstandService;
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    @PostConstruct
    public void logOcrConfigOnStartup() {
        // 用于确认：当前运行进程到底加载了哪套 yml 配置（避免 target/classes 或旧 jar 混淆）
        String requestUrl = "https://" + safe(generalOcrHost) + safe(generalOcrPath);
        log.info("[NOTICE_AGENT_OCR] startup config: requestUrl={}, appId={}, apiKeyPrefix={}, apiSecretPrefix={}",
                requestUrl, safe(generalOcrAppId), maskKeyPrefix(generalOcrApiKey), maskKeyPrefix(generalOcrApiSecret));
    }

    @Override
    public WxNoticeAgentParseVO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        String ext = extension(filename);
        String route;
        String rawText;
        try {
            if ("pdf".equals(ext)) {
                Path temp = Files.createTempFile("notice-agent-", ".pdf");
                file.transferTo(temp.toFile());
                int pages = countPdfPages(temp);
                if (pages <= 5) {
                    route = "PDF_PAGE_OCR";
                    rawText = parseSmallPdfByPageOcr(temp);
                } else {
                    route = "PDF_API_OCR";
                    rawText = parseLargePdfByPdfApi(temp);
                }
                Files.deleteIfExists(temp);
            } else if (isImageExt(ext)) {
                route = "IMAGE_MM_HYBRID";
                rawText = buildHybridImageText(file.getBytes(), ext);
            } else {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持 pdf/jpg/jpeg/png/bmp");
            }
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文件处理失败：" + e.getMessage());
        }
        return extractReminderItems(userId, filename, ext, route, rawText);
    }

    private String buildHybridImageText(byte[] imageBytes, String ext) {
        String visionText = "";
        String ocrText = "";
        Exception visionErr = null;
        Exception ocrErr = null;
        try {
            visionText = imageUnderstandService.understandSingleImage(imageBytes);
        } catch (Exception e) {
            visionErr = e;
            log.warn("[NOTICE_AGENT_MM] image understand failed: {}", truncate(e.getMessage(), 180));
        }
        try {
            ocrText = callGeneralOcr(imageBytes, ext);
        } catch (Exception e) {
            ocrErr = e;
            log.warn("[NOTICE_AGENT_MM] general ocr failed: {}", truncate(e.getMessage(), 180));
        }
        if (isBlank(visionText) && isBlank(ocrText)) {
            String msg = "图片解析失败";
            if (visionErr != null || ocrErr != null) {
                msg += "：";
                if (visionErr != null) {
                    msg += "视觉模型-" + safe(visionErr.getMessage());
                }
                if (ocrErr != null) {
                    msg += (visionErr != null ? "；" : "") + "OCR-" + safe(ocrErr.getMessage());
                }
            }
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, msg);
        }
        if (isBlank(visionText)) {
            return ocrText;
        }
        if (isBlank(ocrText)) {
            return visionText;
        }
        return """
                [IMAGE_UNDERSTAND]
                %s

                [GENERAL_OCR]
                %s
                """.formatted(visionText, ocrText);
    }

    @Override
    public WxNoticeAgentParseVO parseByImageUnderstand(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = extension(filename);
        if (!isImageExt(ext)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片理解仅支持 jpg/jpeg/png/bmp");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        try {
            String rawText = imageUnderstandService.understandSingleImage(file.getBytes());
            return extractReminderItems(userId, filename, ext, "IMAGE_UNDERSTAND", rawText);
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片读取失败：" + e.getMessage());
        }
    }

    @Override
    public WxNoticeAgentParseVO parseByImageModel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = extension(filename);
        if (!isImageExt(ext)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片大模型仅支持 jpg/jpeg/png/bmp");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        try {
            String rawText = callQwenPlusVision(file.getBytes(), ext);
            return extractReminderItems(userId, filename, ext, "IMAGE_MODEL_ONLY", rawText);
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片读取失败：" + e.getMessage());
        }
    }

    private String callQwenPlusVision(byte[] imageBytes, String ext) {
        AiModelConfigEntity model = resolveDefaultModelConfig();
        if (isBlank(model.getApiBaseUrl()) || isBlank(model.getApiKey()) || isBlank(model.getModelNameRemote())) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "qwen模型配置不完整，请检查 ai_model_config");
        }
        String endpoint = buildChatCompletionsEndpoint(model.getApiBaseUrl());
        String mime = switch (ext == null ? "" : ext.toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "bmp" -> "image/bmp";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model.getModelNameRemote());
        body.put("temperature", model.getTemperature() == null ? 0.1 : model.getTemperature());
        body.put("max_tokens", model.getMaxTokens() == null ? 2000 : model.getMaxTokens());
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "text",
                "text", "你是课程表识别助手。请只输出可读文本，尽量保留周次、日期、星期、课程名、时间段、地点；不要解释。"
        ));
        content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl)
        ));
        body.put("messages", List.of(Map.of("role", "user", "content", content)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(model.getApiKey().trim());
        String resp = restTemplate.postForObject(endpoint, new HttpEntity<>(body, headers), String.class);
        if (isBlank(resp)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "qwen模型返回为空");
        }
        JsonNode root = readJson(resp);
        JsonNode message = root.path("choices").isArray() && root.path("choices").size() > 0
                ? root.path("choices").get(0).path("message")
                : null;
        if (message == null || message.isMissingNode()) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "qwen模型返回结构异常");
        }
        JsonNode contentNode = message.path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode n : contentNode) {
                if (n.path("type").asText("").equalsIgnoreCase("text")) {
                    sb.append(n.path("text").asText("")).append('\n');
                }
            }
            return sb.toString().trim();
        }
        return contentNode.toString();
    }

    private AiModelConfigEntity resolveDefaultModelConfig() {
        String modelCode = aiChatRouter.getDefaultModelCode();
        AiModelConfigEntity model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, modelCode)
                .last("limit 1"));
        if (model != null) {
            return model;
        }
        String altCode = modelCode != null && modelCode.contains("_")
                ? modelCode.replace("_", " ")
                : (modelCode == null ? "" : modelCode.replace(" ", "_"));
        model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, altCode)
                .last("limit 1"));
        if (model == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "未找到默认模型配置: " + modelCode);
        }
        return model;
    }

    private String buildChatCompletionsEndpoint(String apiBaseUrl) {
        String base = apiBaseUrl == null ? "" : apiBaseUrl.trim();
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.endsWith("/v1")) {
            base = base + "/v1";
        }
        return base + "/chat/completions";
    }

    @Override
    public List<WxScheduleReminderVO> createBatch(WxNoticeAgentCreateDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒项不能为空");
        }
        List<WxScheduleReminderVO> out = new ArrayList<>();
        for (WxNoticeAgentCreateDTO.ItemDTO item : dto.getItems()) {
            if (item == null || item.getRemindAt() == null || item.getRemindAt() <= 0 || isBlank(item.getTitle())) {
                continue;
            }
            WxScheduleReminderCreateDTO create = new WxScheduleReminderCreateDTO();
            create.setTitle(item.getTitle().trim());
            create.setType(normalizeType(item.getType()));
            create.setRemindAt(item.getRemindAt());
            out.add(wxScheduleReminderService.create(create));
        }
        return out;
    }

    @Override
    public List<WxNoticeAgentRecordVO> listRecords() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        List<ForumNoticeAgentRecordEntity> list = forumNoticeAgentRecordMapper.selectList(new LambdaQueryWrapper<ForumNoticeAgentRecordEntity>()
                .eq(ForumNoticeAgentRecordEntity::getUserId, userId)
                .eq(ForumNoticeAgentRecordEntity::getDeleted, 0)
                .orderByDesc(ForumNoticeAgentRecordEntity::getId)
                .last("limit 30"));
        List<WxNoticeAgentRecordVO> out = new ArrayList<>();
        for (ForumNoticeAgentRecordEntity entity : list) {
            WxNoticeAgentRecordVO vo = new WxNoticeAgentRecordVO();
            vo.setId(entity.getId());
            vo.setFileName(entity.getFileName());
            vo.setRoute(entity.getRoute());
            vo.setSummary(entity.getSummary());
            vo.setRawText(entity.getRawText());
            vo.setItemsJson(entity.getItemsJson());
            vo.setItemCount(entity.getItemCount() == null ? 0 : entity.getItemCount());
            vo.setCreatedAt(entity.getCreatedAt() == null ? "" : entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            out.add(vo);
        }
        return out;
    }

    @Override
    public void deleteRecord(Long recordId) {
        if (recordId == null || recordId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "记录ID不能为空");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        int affected = forumNoticeAgentRecordMapper.update(null, new LambdaUpdateWrapper<ForumNoticeAgentRecordEntity>()
                .set(ForumNoticeAgentRecordEntity::getDeleted, 1)
                .eq(ForumNoticeAgentRecordEntity::getId, recordId)
                .eq(ForumNoticeAgentRecordEntity::getUserId, userId)
                .eq(ForumNoticeAgentRecordEntity::getDeleted, 0));
        if (affected <= 0) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "记录不存在或无权限删除");
        }
    }

    private WxNoticeAgentParseVO extractReminderItems(Long userId, String fileName, String fileExt, String route, String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "OCR未识别到有效文本");
        }
        String cleanedText = simplifyOcrText(text);
        String prompt = """
                你是校园通知解析智能体，尤其擅长“课表/课程安排”解析。请把文本解析为JSON，要求：
                1) 只输出JSON，不要任何解释；
                2) 结构：{"summary":"...","items":[{"title":"...","type":"exam|ddl|activity","remindAt":毫秒时间戳,"source":"..."}]}
                3) remindAt 必须是未来时间；如果只有截止时间，按截止前30分钟；课表场景优先取“课程开始前30分钟”；
                4) 若识别到课程块（课程名+时间段+地点），title 用“课程名@地点”或“课程名”，source 放原始时间段；
                5) 同一天同课程只保留最关键提醒，最多输出8条；
                6) 忽略无意义噪声（坐标、id、seq、graph、layout、page等技术字段）。
                当前时间(UTC+8)：%s
                通知文本：
                %s
                """.formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), truncate(cleanedText, 12000));
        String json = aiChatRouter.rawChat(aiChatRouter.getDefaultModelCode(), prompt);
        log.info("[NOTICE_AGENT_LLM_RAW] {}", json);
        try {
            JsonNode root = OBJECT_MAPPER.readTree(extractJson(json));
            WxNoticeAgentParseVO vo = new WxNoticeAgentParseVO();
            vo.setRoute(route);
            vo.setRawText(text);
            vo.setSummary(root.path("summary").asText(""));
            List<WxNoticeAgentParseVO.ItemVO> items = extractItemsFromJson(root, vo.getSummary());

            // 一次提取为空，进行“课表专项二次提取”
            if (items.isEmpty()) {
                String retryPrompt = """
                        你正在处理课表OCR文本，请务必输出可创建提醒的JSON。
                        输出结构固定：{"summary":"...","items":[{"title":"课程名@地点","type":"activity","remindAt":毫秒时间戳,"source":"开始-结束"}]}
                        规则：
                        - 从文本里提取所有课程开始时间，提醒时间=开始前30分钟；
                        - remindAt 必须是未来时间戳（毫秒）；
                        - 不要输出过去时间；
                        - 只输出JSON。
                        当前时间(UTC+8)：%s
                        文本：
                        %s
                        """.formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), truncate(cleanedText, 12000));
                String retryJson = aiChatRouter.rawChat(aiChatRouter.getDefaultModelCode(), retryPrompt);
                log.info("[NOTICE_AGENT_LLM_RETRY_RAW] {}", retryJson);
                try {
                    JsonNode retryRoot = OBJECT_MAPPER.readTree(extractJson(retryJson));
                    if (isBlank(vo.getSummary())) {
                        vo.setSummary(retryRoot.path("summary").asText(""));
                    }
                    items.addAll(extractItemsFromJson(retryRoot, vo.getSummary()));
                } catch (Exception ignore) {
                    // 二次提取失败走规则兜底
                }
            }

            // 仍为空时，走规则兜底（至少给出可编辑候选）
            if (items.isEmpty()) {
                items.addAll(buildFallbackScheduleItems(cleanedText));
            }
            // 课表专项：规则引擎优先（星期列 -> 具体日期，课程开始前30分钟）
            List<WxNoticeAgentParseVO.ItemVO> courseItems = buildCourseScheduleItems(cleanedText, vo.getSummary());
            if (!courseItems.isEmpty()) {
                if (isBlank(vo.getSummary())) {
                    DateRange textRange = parseDateRange(cleanedText);
                    if (textRange != null) {
                        vo.setSummary("课程周：" + textRange.start + " 至 " + textRange.end);
                    }
                }
                items = mergeItemsPreferCourse(courseItems, items, 8);
            }
            vo.setItems(items);
            vo.setRecordId(saveRecord(userId, fileName, fileExt, route, text, vo.getSummary(), items));
            return vo;
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "通知结构化解析失败：" + e.getMessage());
        }
    }

    private List<WxNoticeAgentParseVO.ItemVO> extractItemsFromJson(JsonNode root, String summary) {
        List<WxNoticeAgentParseVO.ItemVO> items = new ArrayList<>();
        if (root == null || root.isNull()) {
            return items;
        }
        JsonNode arr = root.path("items");
        if (!arr.isArray()) {
            return items;
        }
        Set<String> dedup = new HashSet<>();
        for (JsonNode n : arr) {
            String source = n.path("source").asText("");
            long ts = parseRemindAtFlexible(n.path("remindAt"));
            ts = normalizeRemindAt(ts, source, summary);
            DateRange range = parseDateRange(summary);
            ts = adjustToRangeFuture(ts, source, range);
            if (ts <= 0) continue;
            String title = n.path("title").asText("");
            if (isBlank(title)) continue;
            WxNoticeAgentParseVO.ItemVO item = new WxNoticeAgentParseVO.ItemVO();
            item.setTitle(title.trim());
            item.setType(normalizeType(n.path("type").asText("activity")));
            item.setRemindAt(ts);
            item.setSource(source);
            String key = item.getTitle() + "#" + item.getRemindAt();
            if (dedup.add(key)) {
                items.add(item);
            }
        }
        return items;
    }

    private long normalizeRemindAt(long ts, String source, String summary) {
        DateRange range = parseDateRange(summary);
        LocalTime sourceTime = parseStartTime(source);
        if (range == null) {
            return ts;
        }
        LocalDateTime dt = ts > 0
                ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ts), ZoneId.systemDefault())
                : null;

        // 修复“年错位”（例如识别成 2025，但总结范围是 2026-04-20~2026-04-26）
        if (dt != null && (dt.toLocalDate().isBefore(range.start) || dt.toLocalDate().isAfter(range.end))) {
            LocalDate candidate = LocalDate.of(range.start.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
            if (!candidate.isBefore(range.start) && !candidate.isAfter(range.end)) {
                dt = LocalDateTime.of(candidate, dt.toLocalTime());
            }
        }

        // 优先使用 source 时间段的开始时间-30 分钟
        if (sourceTime != null) {
            LocalDate base = (dt != null) ? dt.toLocalDate() : LocalDate.now();
            if (base.isBefore(range.start)) base = range.start;
            if (base.isAfter(range.end)) base = range.end;
            dt = LocalDateTime.of(base, sourceTime.minusMinutes(30));
        }
        return dt == null ? ts : dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 课表候选不直接丢弃“过去时间”：
     * - 若存在课程周范围，尝试在范围内平移到最近可用日期（保持同一时分）；
     * - 平移后仍不可用则保留原值（供前端手动编辑），避免候选被吞掉。
     */
    private long adjustToRangeFuture(long ts, String source, DateRange range) {
        if (ts <= 0) {
            return ts;
        }
        if (range == null) {
            return ts;
        }
        LocalDateTime dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ts), ZoneId.systemDefault());
        LocalTime sourceTime = parseStartTime(source);
        LocalTime remindTime = sourceTime != null ? sourceTime.minusMinutes(30) : dt.toLocalTime();
        LocalDate date = dt.toLocalDate();
        if (date.isBefore(range.start)) {
            date = range.start;
        } else if (date.isAfter(range.end)) {
            date = range.end;
        }
        LocalDate cursor = date;
        LocalDate today = LocalDate.now();
        if (cursor.isBefore(today)) {
            cursor = today;
        }
        if (cursor.isAfter(range.end)) {
            return ts;
        }
        for (LocalDate d = cursor; !d.isAfter(range.end); d = d.plusDays(1)) {
            long candidate = toEpochMilli(d, remindTime);
            if (candidate > System.currentTimeMillis()) {
                return candidate;
            }
        }
        return ts;
    }

    private LocalTime parseStartTime(String source) {
        if (isBlank(source)) return null;
        Matcher m = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(source);
        if (!m.find()) return null;
        int h = Integer.parseInt(m.group(1));
        int mm = Integer.parseInt(m.group(2));
        if (h < 0 || h > 23 || mm < 0 || mm > 59) return null;
        return LocalTime.of(h, mm);
    }

    private DateRange parseDateRange(String summary) {
        if (isBlank(summary)) return null;
        Matcher m = DATE_RANGE_PATTERN.matcher(summary);
        if (!m.find()) return null;
        try {
            LocalDate start = parseFlexibleDate(m.group(1), null);
            LocalDate end = parseFlexibleDate(m.group(2), null);
            if (start == null || end == null) {
                return null;
            }
            if (end.isBefore(start)) return null;
            return new DateRange(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        private DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    private long parseRemindAtFlexible(JsonNode node) {
        if (node == null || node.isNull()) return 0L;
        if (node.isNumber()) {
            return node.asLong(0L);
        }
        String s = node.asText("");
        if (isBlank(s)) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (Exception ignore) {
            // continue
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private Long saveRecord(Long userId,
                            String fileName,
                            String fileExt,
                            String route,
                            String rawText,
                            String summary,
                            List<WxNoticeAgentParseVO.ItemVO> items) {
        ForumNoticeAgentRecordEntity entity = new ForumNoticeAgentRecordEntity();
        entity.setUserId(userId);
        entity.setFileName(fileName);
        entity.setFileExt(fileExt);
        entity.setRoute(route);
        entity.setRawText(truncate(rawText, 20000));
        entity.setSummary(truncate(summary, 2000));
        entity.setItemsJson(toJson(items));
        entity.setItemCount(items == null ? 0 : items.size());
        entity.setDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        forumNoticeAgentRecordMapper.insert(entity);
        return entity.getId();
    }

    private int countPdfPages(Path pdf) {
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            return doc.getNumberOfPages();
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "PDF页数读取失败：" + e.getMessage());
        }
    }

    private String parseSmallPdfByPageOcr(Path pdf) {
        StringBuilder sb = new StringBuilder();
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = doc.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 180);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                String pageText = callGeneralOcr(baos.toByteArray(), "png");
                sb.append("\n\n--- 第").append(i + 1).append("页 ---\n").append(pageText);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "PDF逐页OCR失败：" + e.getMessage());
        }
    }

    private String parseLargePdfByPdfApi(Path pdf) {
        ensurePdfApiConfig();
        HttpHeaders headers = new HttpHeaders();
        headers.set("appId", pdfOcrAppId);
        long ts = System.currentTimeMillis() / 1000;
        headers.set("timestamp", String.valueOf(ts));
        headers.set("signature", signPdfApi(pdfOcrAppId, pdfOcrApiSecret, ts));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new FileSystemResource(pdf));
        form.add("exportFormat", "markdown");
        ResponseEntity<String> startResp = restTemplate.postForEntity(pdfOcrStartUrl, new HttpEntity<>(form, headers), String.class);
        String taskNo = parsePdfTaskNo(startResp.getBody());
        if (isBlank(taskNo)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "PDF OCR任务创建失败");
        }
        for (int i = 0; i < 18; i++) {
            sleep(5000);
            String url = pdfOcrStatusUrl + "?taskNo=" + taskNo;
            ResponseEntity<String> statusResp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode root = readJson(statusResp.getBody());
            String status = root.path("data").path("status").asText("");
            if ("FINISH".equalsIgnoreCase(status)) {
                String downUrl = root.path("data").path("downUrl").asText("");
                if (isBlank(downUrl)) break;
                return Optional.ofNullable(restTemplate.getForObject(downUrl, String.class)).orElse("");
            }
            if ("FAILED".equalsIgnoreCase(status) || "ANY_FAILED".equalsIgnoreCase(status) || "STOP".equalsIgnoreCase(status)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "PDF OCR任务失败：" + status);
            }
        }
        throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "PDF OCR任务超时");
    }

    private String callGeneralOcr(byte[] imageBytes, String ext) {
        ensureGeneralOcrConfig();

        // 按你提供的 SDK（ocrllm-java-demo）实现：签名 host 使用 requestUrl 自己的 host（cbm01...），authorization 字符串为 hmac username=...
        String requestUrl = "https://" + safe(generalOcrHost) + safe(generalOcrPath);
        String authUrl = assembleRequestUrlLikeSdk(requestUrl, generalOcrApiKey, generalOcrApiSecret, "POST");
        String signHost = safeHostFromUrl(requestUrl);
        log.info("[NOTICE_AGENT_OCR] requestUrl={}, signHost={}, path={}, appId={}, apiKeyPrefix={}",
                requestUrl, signHost, safe(generalOcrPath), safe(generalOcrAppId), maskKeyPrefix(generalOcrApiKey));

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        Map<String, Object> body = new LinkedHashMap<>();
        // SDK demo 的 status=0（开始）；这里保持一致
        body.put("header", Map.of("app_id", generalOcrAppId, "status", 0));
        body.put("parameter", Map.of("ocr", Map.of(
                "result_option", "normal,no_line_position",
                "result_format", "json",
                "output_type", "one_shot",
                "result", Map.of("encoding", "utf8", "compress", "raw", "format", "plain")
        )));
        body.put("payload", Map.of("image", Map.of(
                "encoding", ext,
                "image", base64,
                "status", 0,
                "seq", 0
        )));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final String resp;
        try {
            // RestTemplate 传 String URL 可能会对已编码的 query 参数再次编码，导致服务端验签失败；
            // 用 URI 传入可避免二次编码，行为与 OkHttp 的 .url(String) 更一致。
            resp = restTemplate.postForObject(URI.create(authUrl), new HttpEntity<>(body, headers), String.class);
        } catch (HttpClientErrorException e) {
            // apikey not found / signature mismatch 等，带上脱敏信息便于定位
            String msg = e.getResponseBodyAsString();
            log.warn("[NOTICE_AGENT_OCR] call failed: status={}, apiKeyPrefix={}, requestUrl={}, signHost={}, path={}. body={}",
                    e.getStatusCode().value(), maskKeyPrefix(generalOcrApiKey), requestUrl, signHost, safe(generalOcrPath), truncate(msg, 300));
            throw e;
        }
        JsonNode root = readJson(resp);
        log.info("begin");
        log.info(String.valueOf(root));
        int code = root.path("header").path("code").asInt(-1);
        if (code != 0) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "通用OCR失败：" + root.path("header").path("message").asText("unknown"));
        }
        String textB64 = root.path("payload").path("result").path("text").asText("");
        if (isBlank(textB64)) return "";
        try {
            String decoded = new String(Base64.getDecoder().decode(textB64), StandardCharsets.UTF_8);
            return extractOcrReadableText(decoded);
        } catch (Exception e) {
            return textB64;
        }
    }

    /**
     * 完全对齐 ocrllm-java-demo 的 assembleRequestUrl 实现：
     * - host = new URL(requestUrl).getHost()
     * - date = RFC1123 + GMT
     * - signOrigin = "host: {host}\ndate: {date}\n{METHOD} {path} HTTP/1.1"
     * - authorization_origin = "hmac username=\"{apiKey}\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"{base64(hmac)}\""
     * - authorization = base64(authorization_origin)
     * - requestUrl?authorization=...&host=...&date=...
     */
    private String assembleRequestUrlLikeSdk(String requestUrl, String apiKey, String apiSecret, String method) {
        try {
            URL url = new URL(requestUrl);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            String host = url.getHost();

            String signOrigin = new StringBuilder("host: ").append(host).append("\n")
                    .append("date: ").append(date).append("\n")
                    .append(method).append(" ").append(url.getPath()).append(" HTTP/1.1")
                    .toString();

            String signature = hmacSha256Base64LowercaseAlgo(apiSecret, signOrigin);
            String authorizationOrigin = String.format(
                    "hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    apiKey, "hmac-sha256", "host date request-line", signature
            );
            String authBase = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));
            return String.format("%s?authorization=%s&host=%s&date=%s",
                    requestUrl, urlEncode(authBase), urlEncode(host), urlEncode(date));
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "通用OCR鉴权URL生成失败：" + e.getMessage());
        }
    }

    private String safeHostFromUrl(String requestUrl) {
        try {
            return new URL(requestUrl).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private String hmacSha256Base64LowercaseAlgo(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("hmacsha256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "hmacsha256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // fallback：部分 JCE provider 可能只认 "HmacSHA256"
            return hmacSha256Base64(secret, content);
        }
    }

    private String parsePdfTaskNo(String body) {
        JsonNode root = readJson(body);
        return root.path("data").path("taskNo").asText("");
    }

    private String signPdfApi(String appId, String secret, long ts) {
        try {
            String auth = md5(appId + ts);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(auth.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "PDF OCR签名失败：" + e.getMessage());
        }
    }

    private String md5(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] d = md.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String extractJson(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) s = s.substring(firstNl + 1);
            int end = s.lastIndexOf("```");
            if (end > 0) s = s.substring(0, end);
        }
        return s.trim();
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj == null ? List.of() : obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private JsonNode readJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json == null ? "{}" : json);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "JSON解析失败：" + e.getMessage());
        }
    }

    /**
     * 对齐 SDK 返回结果：优先使用 document.markdown（可读性最好）；
     * 若不存在，再退化为提取 text 字段。
     */
    private String extractOcrReadableText(String decoded) {
        String src = decoded == null ? "" : decoded.trim();
        if (src.isEmpty()) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(src);
            JsonNode docs = root.path("document");
            if (docs.isArray()) {
                for (JsonNode n : docs) {
                    if ("markdown".equalsIgnoreCase(n.path("name").asText(""))) {
                        String markdown = n.path("value").asText("");
                        if (!isBlank(markdown)) {
                            return markdown;
                        }
                    }
                }
            }
            return simplifyOcrText(src);
        } catch (Exception ignore) {
            return src;
        }
    }

    /**
     * OCR返回经常是超大结构化JSON（含坐标/版面信息），这里尽量提取“可读文本行”，降低噪声干扰。
     */
    private String simplifyOcrText(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(s);
            LinkedHashSet<String> lines = new LinkedHashSet<>();
            collectUsefulText(root, lines);
            if (lines.isEmpty()) {
                return s;
            }
            StringBuilder out = new StringBuilder();
            for (String line : lines) {
                out.append(line).append('\n');
            }
            return out.toString().trim();
        } catch (Exception ignore) {
            return s;
        }
    }

    private void collectUsefulText(JsonNode node, LinkedHashSet<String> lines) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                String key = e.getKey();
                JsonNode value = e.getValue();
                if ("text".equals(key) && value != null) {
                    appendTextNode(value, lines);
                    continue;
                }
                collectUsefulText(value, lines);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectUsefulText(child, lines);
            }
        }
    }

    private void appendTextNode(JsonNode value, LinkedHashSet<String> lines) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isTextual()) {
            String t = normalizeLine(value.asText(""));
            if (!isBlank(t)) {
                lines.add(t);
            }
            return;
        }
        if (value.isArray()) {
            for (JsonNode item : value) {
                appendTextNode(item, lines);
            }
        }
    }

    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        String t = line.replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) {
            return "";
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.startsWith("id") || lower.contains("graph") || lower.contains("layout") || lower.contains("contour")) {
            return "";
        }
        return t;
    }

    private List<WxNoticeAgentParseVO.ItemVO> buildFallbackScheduleItems(String text) {
        if (isBlank(text)) {
            return List.of();
        }
        List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        Pattern p = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
        String course = "";
        String place = "";
        List<WxNoticeAgentParseVO.ItemVO> out = new ArrayList<>();
        Set<String> dedup = new HashSet<>();
        LocalDate baseDate = LocalDate.now();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = p.matcher(line);
            if (m.matches() && i + 1 < lines.size()) {
                String next = lines.get(i + 1);
                if (p.matcher(next).matches()) {
                    int h = Integer.parseInt(m.group(1));
                    int min = Integer.parseInt(m.group(2));
                    long remindAt = toEpochMilli(baseDate, LocalTime.of(h, min).minusMinutes(30));
                    String title = !isBlank(course) ? course : "课程提醒";
                    if (!isBlank(place)) {
                        title = title + "@" + place;
                    }
                    String key = title + "#" + remindAt;
                    if (dedup.add(key)) {
                        WxNoticeAgentParseVO.ItemVO item = new WxNoticeAgentParseVO.ItemVO();
                        item.setTitle(title);
                        item.setType("activity");
                        item.setRemindAt(remindAt);
                        item.setSource(line + "-" + next);
                        out.add(item);
                    }
                    continue;
                }
            }
            if (line.contains("课程") || line.contains("技术") || line.contains("语言") || line.contains("机器人")) {
                course = line.replaceAll("\\s+", "");
                continue;
            }
            if (line.contains("楼") || line.contains("@本部")) {
                place = line.replaceAll("\\s+", "");
            }
        }
        if (out.size() > 8) {
            return out.subList(0, 8);
        }
        return out;
    }

    private List<WxNoticeAgentParseVO.ItemVO> buildCourseScheduleItems(String text, String summary) {
        if (isBlank(text)) {
            return List.of();
        }
        DateRange range = parseDateRange(summary);
        if (range == null) {
            range = parseDateRange(text);
        }
        LocalDate defaultDate = range != null ? range.start : LocalDate.now();
        List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<WxNoticeAgentParseVO.ItemVO> out = new ArrayList<>();
        Set<String> dedup = new HashSet<>();
        String contextCourse = "";
        String contextPlace = "";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (looksLikeCourseName(line)) {
                contextCourse = cleanupCourseText(line);
            }
            if (looksLikePlace(line)) {
                contextPlace = cleanupPlaceText(line);
            }
            Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(line);
            while (timeMatcher.find()) {
                LocalTime startTime = parseHm(timeMatcher.group(1));
                LocalTime endTime = parseHm(timeMatcher.group(2));
                if (startTime == null || endTime == null) {
                    continue;
                }
                LocalDate date = resolveDateFromContext(line, lines, i, range, defaultDate);
                if (date == null) {
                    date = defaultDate;
                }
                long remindAt = toEpochMilli(date, startTime.minusMinutes(30));
                String near = collectNearText(lines, i);
                String title = resolveCourseTitle(near, contextCourse, contextPlace);
                if (isBlank(title)) {
                    title = "课程提醒";
                }
                String source = timeMatcher.group(1) + "-" + timeMatcher.group(2);
                WxNoticeAgentParseVO.ItemVO item = new WxNoticeAgentParseVO.ItemVO();
                item.setTitle(title);
                item.setType("activity");
                item.setRemindAt(remindAt);
                item.setSource(source);
                String key = title + "#" + remindAt;
                if (dedup.add(key)) {
                    out.add(item);
                }
            }
        }
        out.sort(Comparator.comparingLong(WxNoticeAgentParseVO.ItemVO::getRemindAt));
        if (out.size() > 8) {
            return new ArrayList<>(out.subList(0, 8));
        }
        return out;
    }

    private LocalTime parseHm(String hm) {
        if (isBlank(hm)) {
            return null;
        }
        try {
            String[] arr = hm.split(":");
            int h = Integer.parseInt(arr[0]);
            int m = Integer.parseInt(arr[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                return null;
            }
            return LocalTime.of(h, m);
        } catch (Exception ignore) {
            return null;
        }
    }

    private LocalDate resolveDateFromContext(String line,
                                             List<String> lines,
                                             int idx,
                                             DateRange range,
                                             LocalDate defaultDate) {
        LocalDate hit = parseFlexibleDate(line, range);
        if (hit != null) {
            return hit;
        }
        if (idx > 0) {
            hit = parseFlexibleDate(lines.get(idx - 1), range);
            if (hit != null) return hit;
        }
        if (idx + 1 < lines.size()) {
            hit = parseFlexibleDate(lines.get(idx + 1), range);
            if (hit != null) return hit;
        }
        DayOfWeek dow = parseWeekday(line);
        if (dow == null && idx > 0) {
            dow = parseWeekday(lines.get(idx - 1));
        }
        if (dow == null && idx + 1 < lines.size()) {
            dow = parseWeekday(lines.get(idx + 1));
        }
        if (dow != null) {
            LocalDate from = range != null ? range.start : defaultDate;
            return alignToWeekday(from, dow, range);
        }
        return defaultDate;
    }

    private LocalDate parseFlexibleDate(String text, DateRange range) {
        if (isBlank(text)) {
            return null;
        }
        Matcher full = FULL_DATE_PATTERN.matcher(text);
        if (full.find()) {
            try {
                LocalDate d = LocalDate.of(Integer.parseInt(full.group(1)),
                        Integer.parseInt(full.group(2)),
                        Integer.parseInt(full.group(3)));
                if (range == null || (!d.isBefore(range.start) && !d.isAfter(range.end))) {
                    return d;
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
        Matcher md = MD_DATE_PATTERN.matcher(text);
        if (md.find()) {
            int mm = Integer.parseInt(md.group(1));
            int dd = Integer.parseInt(md.group(2));
            int year = range != null ? range.start.getYear() : LocalDate.now().getYear();
            try {
                LocalDate d = LocalDate.of(year, mm, dd);
                if (range == null || (!d.isBefore(range.start) && !d.isAfter(range.end))) {
                    return d;
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
        return null;
    }

    private DayOfWeek parseWeekday(String text) {
        if (isBlank(text)) return null;
        if (text.contains("周一") || text.contains("星期一")) return DayOfWeek.MONDAY;
        if (text.contains("周二") || text.contains("星期二")) return DayOfWeek.TUESDAY;
        if (text.contains("周三") || text.contains("星期三")) return DayOfWeek.WEDNESDAY;
        if (text.contains("周四") || text.contains("星期四")) return DayOfWeek.THURSDAY;
        if (text.contains("周五") || text.contains("星期五")) return DayOfWeek.FRIDAY;
        if (text.contains("周六") || text.contains("星期六")) return DayOfWeek.SATURDAY;
        if (text.contains("周日") || text.contains("星期日") || text.contains("星期天") || text.contains("周天")) return DayOfWeek.SUNDAY;
        return null;
    }

    private LocalDate alignToWeekday(LocalDate from, DayOfWeek dow, DateRange range) {
        LocalDate d = from;
        for (int i = 0; i < 7; i++) {
            if (d.getDayOfWeek() == dow) {
                if (range == null || (!d.isBefore(range.start) && !d.isAfter(range.end))) {
                    return d;
                }
            }
            d = d.plusDays(1);
        }
        return range != null ? range.start : from;
    }

    private String collectNearText(List<String> lines, int idx) {
        StringBuilder sb = new StringBuilder();
        for (int j = Math.max(0, idx - 2); j <= Math.min(lines.size() - 1, idx + 2); j++) {
            sb.append(lines.get(j)).append(' ');
        }
        return sb.toString();
    }

    private String resolveCourseTitle(String nearText, String contextCourse, String contextPlace) {
        String course = contextCourse;
        String place = contextPlace;
        if (!isBlank(nearText)) {
            String[] tokens = nearText.split("\\s+");
            for (String token : tokens) {
                if (looksLikeCourseName(token)) {
                    course = cleanupCourseText(token);
                } else if (looksLikePlace(token)) {
                    place = cleanupPlaceText(token);
                }
            }
        }
        if (isBlank(course)) {
            return "";
        }
        return isBlank(place) ? course : (course + "@" + place);
    }

    private boolean looksLikeCourseName(String text) {
        if (isBlank(text)) return false;
        return text.contains("课程")
                || text.contains("实验")
                || text.contains("设计")
                || text.contains("高数")
                || text.contains("英语")
                || text.contains("物理")
                || text.contains("化学")
                || text.contains("程序")
                || text.contains("Java")
                || text.contains("Python");
    }

    private boolean looksLikePlace(String text) {
        if (isBlank(text)) return false;
        return text.contains("楼") || text.contains("教室") || text.contains("@本部") || text.contains("校区");
    }

    private String cleanupCourseText(String text) {
        return text == null ? "" : text.replaceAll("[\\[\\]【】()（）]", "").trim();
    }

    private String cleanupPlaceText(String text) {
        return text == null ? "" : text.replaceAll("[\\[\\]【】()（）]", "").trim();
    }

    private List<WxNoticeAgentParseVO.ItemVO> mergeItemsPreferCourse(List<WxNoticeAgentParseVO.ItemVO> courseItems,
                                                                      List<WxNoticeAgentParseVO.ItemVO> aiItems,
                                                                      int limit) {
        List<WxNoticeAgentParseVO.ItemVO> out = new ArrayList<>();
        Set<String> dedup = new HashSet<>();
        for (WxNoticeAgentParseVO.ItemVO item : courseItems) {
            String key = item.getTitle() + "#" + item.getRemindAt();
            if (dedup.add(key)) {
                out.add(item);
            }
        }
        for (WxNoticeAgentParseVO.ItemVO item : aiItems) {
            String key = item.getTitle() + "#" + item.getRemindAt();
            if (dedup.add(key)) {
                out.add(item);
            }
        }
        out.sort(Comparator.comparingLong(WxNoticeAgentParseVO.ItemVO::getRemindAt));
        if (out.size() > limit) {
            return new ArrayList<>(out.subList(0, limit));
        }
        return out;
    }

    private long toEpochMilli(LocalDate date, LocalTime time) {
        LocalDateTime dt = LocalDateTime.of(date, time);
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if ("exam".equals(t) || "ddl".equals(t) || "activity".equals(t)) return t;
        return "activity";
    }

    private String extension(String name) {
        int idx = name == null ? -1 : name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isImageExt(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "bmp".equals(ext);
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private String maskKeyPrefix(String v) {
        if (isBlank(v)) return "";
        String s = v.trim();
        int n = Math.min(6, s.length());
        return s.substring(0, n) + "***";
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String hmacSha256Base64(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "OCR签名失败：" + e.getMessage());
        }
    }

    private String urlEncode(String v) {
        return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private void ensureGeneralOcrConfig() {
        if (isBlank(generalOcrAppId) || isBlank(generalOcrApiKey) || isBlank(generalOcrApiSecret)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "未配置通用OCR参数：xf.ocr.general.app-id/api-key/api-secret");
        }
    }

    private void ensurePdfApiConfig() {
        if (isBlank(pdfOcrAppId) || isBlank(pdfOcrApiSecret)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "未配置PDF OCR参数：xf.ocr.pdf.app-id/api-secret");
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
    }
}
