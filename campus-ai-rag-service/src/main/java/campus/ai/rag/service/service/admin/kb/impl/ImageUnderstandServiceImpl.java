package campus.ai.rag.service.service.admin.kb.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.admin.kb.ImageUnderstandService;
import campus.ai.rag.service.vo.admin.kb.KbDocumentImagePreviewVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImageUnderstandServiceImpl implements ImageUnderstandService {

    private static final Logger log = LoggerFactory.getLogger(ImageUnderstandServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_HOST = "spark-api.cn-huabei-1.xf-yun.com";
    private static final String DEFAULT_PATH = "/v2.1/image";
    private static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;
    private static final double MIN_PIXEL_COUNT = 50D * 50D;
    private static final double MAX_PIXEL_COUNT = 6000D * 6000D;
    private static final int MIN_SIDE = 50;
    private static final int MAX_SIDE = 6000;
    private static final Path BASE_IMAGE_DIR = Paths.get(System.getProperty("user.dir"))
            .resolve(Paths.get("uploads", "kb", "images"));

    private final HttpClient httpClient;

    @Value("${xf.image.app-id:}")
    private String appId;

    @Value("${xf.image.api-key:}")
    private String apiKey;

    @Value("${xf.image.api-secret:}")
    private String apiSecret;

    @Value("${xf.image.host:" + DEFAULT_HOST + "}")
    private String host;

    @Value("${xf.image.path:" + DEFAULT_PATH + "}")
    private String path;

    @Value("${xf.image.domain:imagev3}")
    private String domain;

    @Value("${xf.image.prompt:请尽可能完整提取图片中的文字内容，如果有表格请用markdown表格输出，并补充关键图示说明。}")
    private String prompt;

    @Value("${xf.image.connect-timeout-seconds:20}")
    private Integer connectTimeoutSeconds;

    @Value("${xf.image.await-timeout-seconds:90}")
    private Integer awaitTimeoutSeconds;

    public ImageUnderstandServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public List<KbDocumentImagePreviewVO.Item> understandImages(Long documentId, Path filePath, String fileExt) {
        validateConfig();
        if (documentId == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文档ID不能为空");
        }
        String ext = fileExt == null ? "" : fileExt.trim().toLowerCase(Locale.ROOT);
        if (!"docx".equals(ext) && !"pdf".equals(ext)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前图片理解仅支持 pdf/docx 文档");
        }

        List<ImagePayload> images = "pdf".equals(ext) ? extractPdfImages(documentId, filePath) : extractDocxImages(documentId, filePath);
        List<KbDocumentImagePreviewVO.Item> items = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ImagePayload image = images.get(i);
            KbDocumentImagePreviewVO.Item item = new KbDocumentImagePreviewVO.Item();
            item.setImageIndex(i + 1);
            item.setImageExt(image.ext);
            item.setImagePath(image.savedPath == null ? null : image.savedPath.toAbsolutePath().toString());
            if (image.savedPath != null) {
                log.info("KB_IMAGE_EXTRACTED docId={} idx={} path={}", documentId, (i + 1), item.getImagePath());
            }
            log.info("KB_IMAGE_META docId={} idx={} width={} height={} bytes={}",
                    documentId, (i + 1), image.width, image.height, image.bytes == null ? 0 : image.bytes.length);
            if (image.bytes.length > MAX_IMAGE_BYTES) {
                item.setText("图片超过4MB，已跳过");
            } else {
                try {
                    item.setText(callSparkImageUnderstand(image.bytes));
                } catch (BusinessException ex) {
                    String extra = " [idx=" + (i + 1)
                            + ",width=" + image.width
                            + ",height=" + image.height
                            + ",bytes=" + image.bytes.length
                            + ",path=" + item.getImagePath() + "]";
                    throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, ex.getMessage() + extra);
                }
            }
            items.add(item);
        }
        return items;
    }

    private List<ImagePayload> extractDocxImages(Long documentId, Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "文档文件不存在");
        }
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(filePath))) {
            List<ImagePayload> result = new ArrayList<>();
            Path dir = BASE_IMAGE_DIR.resolve(String.valueOf(documentId));
            Files.createDirectories(dir);
            for (XWPFPictureData picture : document.getAllPictures()) {
                ImagePayload payload = new ImagePayload();
                NormalizedImage normalized = normalizeToSparkPng(picture.getData());
                payload.bytes = normalized.bytes;
                payload.width = normalized.width;
                payload.height = normalized.height;
                String ext = picture.suggestFileExtension();
                payload.ext = ext == null ? "png" : ext.toLowerCase(Locale.ROOT);
                payload.savedPath = saveImageBytes(dir, payload.bytes, payload.ext);
                result.add(payload);
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "提取docx图片失败：" + e.getMessage());
        }
    }

    private List<ImagePayload> extractPdfImages(Long documentId, Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "文档文件不存在");
        }
        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            List<ImagePayload> result = new ArrayList<>();
            Path dir = BASE_IMAGE_DIR.resolve(String.valueOf(documentId));
            Files.createDirectories(dir);
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources == null) {
                    continue;
                }
                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(name);
                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xObject;
                        byte[] pngBytes = toPngBytes(image);
                        ImagePayload payload = new ImagePayload();
                        NormalizedImage normalized = normalizeToSparkPng(pngBytes);
                        payload.bytes = normalized.bytes;
                        payload.width = normalized.width;
                        payload.height = normalized.height;
                        payload.ext = "png";
                        payload.savedPath = saveImageBytes(dir, payload.bytes, payload.ext);
                        result.add(payload);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "提取pdf图片失败：" + e.getMessage());
        }
    }

    private byte[] toPngBytes(PDImageXObject image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image.getImage(), "png", baos);
        return baos.toByteArray();
    }

    private NormalizedImage normalizeToSparkPng(byte[] source) {
        try {
            BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(source));
            if (original == null) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片格式无法识别，暂不支持该图片");
            }

            BufferedImage normalized = scaleToSparkLimits(original);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", baos);
            NormalizedImage result = new NormalizedImage();
            result.bytes = baos.toByteArray();
            result.width = normalized.getWidth();
            result.height = normalized.getHeight();
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片预处理失败：" + ex.getMessage());
        }
    }

    private BufferedImage scaleToSparkLimits(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片尺寸异常");
        }

        double scale = 1.0D;
        double pixels = (double) width * (double) height;
        if (pixels > MAX_PIXEL_COUNT) {
            scale = Math.min(scale, Math.sqrt(MAX_PIXEL_COUNT / pixels));
        }
        if (pixels < MIN_PIXEL_COUNT) {
            scale = Math.max(scale, Math.sqrt(MIN_PIXEL_COUNT / pixels));
        }
        if (width > MAX_SIDE || height > MAX_SIDE) {
            scale = Math.min(scale, Math.min((double) MAX_SIDE / width, (double) MAX_SIDE / height));
        }

        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        // 星火要求分辨率满足：50x50 < 总像素 < 6000x6000。这里同时兜底最小边长。
        if (newWidth < MIN_SIDE || newHeight < MIN_SIDE) {
            double sideScale = Math.max((double) MIN_SIDE / newWidth, (double) MIN_SIDE / newHeight);
            newWidth = (int) Math.round(newWidth * sideScale);
            newHeight = (int) Math.round(newHeight * sideScale);
        }
        if (newWidth > MAX_SIDE || newHeight > MAX_SIDE) {
            double down = Math.min((double) MAX_SIDE / newWidth, (double) MAX_SIDE / newHeight);
            newWidth = Math.max(MIN_SIDE, (int) Math.round(newWidth * down));
            newHeight = Math.max(MIN_SIDE, (int) Math.round(newHeight * down));
        }

        if (newWidth == width && newHeight == height) {
            return source;
        }

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }

    private Path saveImageBytes(Path dir, byte[] bytes, String ext) {
        try {
            String safeExt = (ext == null || ext.trim().isEmpty()) ? "png" : ext.trim().toLowerCase(Locale.ROOT);
            String filename = System.currentTimeMillis() + "-" + Math.abs(Base64.getEncoder().encodeToString(bytes).hashCode()) + "." + safeExt;
            Path target = dir.resolve(filename);
            Files.write(target, bytes);
            return target;
        } catch (Exception e) {
            // 落盘失败不影响后续理解调用
            return null;
        }
    }

    private String callSparkImageUnderstand(byte[] imageBytes) {
        try {
            String url = buildAuthUrl();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            String requestJson = buildRequestJson(imageBase64);

            // 星火侧偶发 500，做一次轻量重试（只对 internal server error 触发）
            int attempts = 2;
            BusinessException last = null;
            for (int i = 1; i <= attempts; i++) {
                try {
                    return doCallOnce(url, requestJson);
                } catch (BusinessException ex) {
                    last = ex;
                    String msg = ex.getMessage() == null ? "" : ex.getMessage();
                    boolean retryable = msg.toLowerCase(Locale.ROOT).contains("internal server error");
                    if (i < attempts && retryable) {
                        try {
                            Thread.sleep(800L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                    throw ex;
                }
            }
            if (last != null) {
                throw last;
            }
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片理解失败：未知错误");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            String target = (isBlank(host) ? DEFAULT_HOST : host.trim()) + (isBlank(path) ? DEFAULT_PATH : path.trim());
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "调用图片理解失败（" + target + "）：" + ex.getMessage());
        }
    }

    private String doCallOnce(String url, String requestJson) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder responseText = new StringBuilder();
        StringBuilder errorMessage = new StringBuilder();
        StringBuilder wsCloseInfo = new StringBuilder();
        StringBuilder sidHolder = new StringBuilder();
        AtomicInteger lastCode = new AtomicInteger(0);
        StringBuilder lastMessage = new StringBuilder();

        int ct = connectTimeoutSeconds == null ? 20 : Math.max(5, connectTimeoutSeconds);
        CompletableFuture<WebSocket> future = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(ct))
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocket.sendText(requestJson, true);
                        webSocket.request(1);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        boolean finished = handleFrame(data.toString(), responseText, errorMessage, sidHolder, lastCode, lastMessage, latch);
                        if (finished) {
                            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
                        } else {
                            webSocket.request(1);
                        }
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        if (statusCode != WebSocket.NORMAL_CLOSURE) {
                            wsCloseInfo.append("status=").append(statusCode).append(", reason=").append(reason == null ? "" : reason);
                        }
                        latch.countDown();
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        if (error != null) {
                            errorMessage.append(error.getClass().getSimpleName()).append(": ").append(error.getMessage());
                        } else {
                            errorMessage.append("WS错误：unknown");
                        }
                        latch.countDown();
                    }
                });
        future.join();

        int at = awaitTimeoutSeconds == null ? 90 : Math.max(10, awaitTimeoutSeconds);
        boolean done = latch.await(at, TimeUnit.SECONDS);
        if (!done) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片理解超时（" + at + "s）");
        }
        if (errorMessage.length() > 0) {
            String extra = buildExtra(sidHolder, lastCode, lastMessage, wsCloseInfo);
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片理解失败：" + errorMessage + extra);
        }
        if (wsCloseInfo.length() > 0 && responseText.length() == 0) {
            String extra = buildExtra(sidHolder, lastCode, lastMessage, wsCloseInfo);
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "图片理解失败：WS关闭异常 " + wsCloseInfo + extra);
        }
        return responseText.toString().trim();
    }

    private String buildExtra(StringBuilder sidHolder, AtomicInteger lastCode, StringBuilder lastMessage, StringBuilder wsCloseInfo) {
        String sid = sidHolder == null ? "" : sidHolder.toString();
        int code = lastCode == null ? 0 : lastCode.get();
        String msg = lastMessage == null ? "" : lastMessage.toString();
        String close = wsCloseInfo == null ? "" : wsCloseInfo.toString();
        if ((sid == null || sid.isEmpty()) && code == 0 && (msg == null || msg.isEmpty()) && (close == null || close.isEmpty())) {
            return "";
        }
        return "（sid=" + sid + " code=" + code + " message=" + msg + " close=" + close + "）";
    }

    private boolean handleFrame(String json,
                                StringBuilder responseText,
                                StringBuilder errorMessage,
                                StringBuilder sidHolder,
                                AtomicInteger lastCode,
                                StringBuilder lastMessage,
                                CountDownLatch latch) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode header = root.path("header");
            int code = header.path("code").asInt(-1);
            if (lastCode != null) {
                lastCode.set(code);
            }
            String sid = header.path("sid").asText("");
            if (sidHolder != null && sidHolder.length() == 0 && sid != null && !sid.isEmpty()) {
                sidHolder.append(sid);
            }
            String message = header.path("message").asText("");
            if (lastMessage != null && message != null && !message.isEmpty()) {
                lastMessage.setLength(0);
                lastMessage.append(message);
            }
            if (code != 0) {
                errorMessage.append(message == null || message.isEmpty() ? "图片理解服务异常" : message);
                latch.countDown();
                return true;
            }
            JsonNode textNodes = root.path("payload").path("choices").path("text");
            if (textNodes.isArray()) {
                for (JsonNode node : textNodes) {
                    String content = node.path("content").asText("");
                    if (!content.isEmpty()) {
                        responseText.append(content);
                    }
                }
            }
            int status = root.path("payload").path("choices").path("status").asInt(-1);
            if (status == 2) {
                latch.countDown();
                return true;
            }
            return false;
        } catch (Exception ex) {
            errorMessage.append("响应解析失败: ").append(ex.getMessage());
            latch.countDown();
            return true;
        }
    }

    private String buildRequestJson(String imageBase64) throws Exception {
        JsonNode root = OBJECT_MAPPER.createObjectNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).putObject("header")
                .put("app_id", appId)
                .put("uid", "kb-admin");
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).putObject("parameter")
                .putObject("chat")
                .put("domain", domain)
                .put("temperature", 0.1)
                .put("top_k", 4)
                .put("max_tokens", 2028);
        com.fasterxml.jackson.databind.node.ArrayNode textArray = OBJECT_MAPPER.createArrayNode();
        textArray.add(OBJECT_MAPPER.createObjectNode()
                .put("role", "user")
                .put("content", imageBase64)
                .put("content_type", "image"));
        textArray.add(OBJECT_MAPPER.createObjectNode()
                .put("role", "user")
                .put("content", prompt)
                .put("content_type", "text"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) root).putObject("payload")
                .putObject("message")
                .set("text", textArray);
        return OBJECT_MAPPER.writeValueAsString(root);
    }

    private String buildAuthUrl() throws Exception {
        String targetHost = isBlank(host) ? DEFAULT_HOST : host.trim();
        String targetPath = isBlank(path) ? DEFAULT_PATH : path.trim();
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        String requestLine = "GET " + targetPath + " HTTP/1.1";
        String signatureOrigin = "host: " + targetHost + "\n" + "date: " + date + "\n" + requestLine;
        String signature = hmacSha256Base64(apiSecret, signatureOrigin);
        String authorizationOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));
        return "wss://" + targetHost + targetPath
                + "?authorization=" + URLEncoder.encode(authorization, StandardCharsets.UTF_8)
                + "&date=" + URLEncoder.encode(date, StandardCharsets.UTF_8)
                + "&host=" + URLEncoder.encode(targetHost, StandardCharsets.UTF_8);
    }

    private String hmacSha256Base64(String secret, String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private void validateConfig() {
        if (isBlank(appId) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "未配置星火图片理解参数：xf.image.app-id/api-key/api-secret");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ImagePayload {
        private byte[] bytes;
        private String ext;
        private Path savedPath;
        private int width;
        private int height;
    }

    private static class NormalizedImage {
        private byte[] bytes;
        private int width;
        private int height;
    }
}

