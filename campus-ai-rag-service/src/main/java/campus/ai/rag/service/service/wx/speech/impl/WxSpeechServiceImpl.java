package campus.ai.rag.service.service.wx.speech.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.wx.speech.WxSpeechService;
import campus.ai.rag.service.vo.wx.WxSpeechTranscribeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class WxSpeechServiceImpl implements WxSpeechService {

    private static final long MAX_AUDIO_SIZE = 10L * 1024L * 1024L;
    private static final List<String> SUPPORTED_SUFFIX = List.of(".mp3", ".wav", ".m4a", ".aac", ".pcm");
    private static final int FRAME_BYTES = 1280;
    private static final int FRAME_INTERVAL_MS = 40;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${xf.rtasr.url:wss://office-api-ast-dx.iflyaisol.com/ast/communicate/v1}")
    private String astUrl;

    @Value("${xf.rtasr.app-id:}")
    private String appId;

    @Value("${xf.rtasr.api-key:}")
    private String accessKeyId;

    @Value("${xf.rtasr.api-secret:}")
    private String accessKeySecret;

    @Value("${xf.rtasr.lang:autodialect}")
    private String lang;

    @Value("${xf.rtasr.audio-encode:pcm_s16le}")
    private String audioEncode;

    @Value("${xf.rtasr.samplerate:16000}")
    private Integer sampleRate;

    @Value("${xf.iat.url:wss://iat-api.xfyun.cn/v2/iat}")
    private String iatUrl;

    @Value("${xf.iat.app-id:6346119f}")
    private String iatAppId;

    @Value("${xf.iat.api-key:caba4cc5c2e634ec94313d2f2c8d0c16}")
    private String iatApiKey;

    @Value("${xf.iat.api-secret:ZTgzYmRjMjg2MmY3OWViODMwMzIyNDdj}")
    private String iatApiSecret;

    @Value("${xf.iat.language:zh_cn}")
    private String iatLanguage;

    @Value("${xf.iat.domain:iat}")
    private String iatDomain;

    @Value("${xf.iat.accent:mandarin}")
    private String iatAccent;

    @Value("${xf.iat.dwa:wpgs}")
    private String iatDwa;

    @Override
    public WxSpeechTranscribeVO transcribe(MultipartFile file, String scene, Long durationMs) {
        validateInput(file);
        if (isBlank(astUrl) || isBlank(appId) || isBlank(accessKeyId) || isBlank(accessKeySecret)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "实时语音转写大模型配置不完整，请检查 xf.rtasr.*");
        }
        try {
            String text = transcribeByRealtimeWs(file.getBytes());
            if (isBlank(text)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "语音转写结果为空");
            }
            WxSpeechTranscribeVO vo = new WxSpeechTranscribeVO();
            vo.setText(text.trim());
            vo.setScene(isBlank(scene) ? "chat" : scene.trim());
            vo.setDurationMs(durationMs == null ? 0L : Math.max(durationMs, 0L));
            vo.setProvider("xf-realtime-ast");
            return vo;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "语音转写失败：" + ex.getMessage());
        }
    }

    @Override
    public WxSpeechTranscribeVO transcribeNew(MultipartFile file, String scene, Long durationMs) {
        return transcribeNewStream(file, scene, durationMs, null);
    }

    @Override
    public WxSpeechTranscribeVO transcribeNewStream(MultipartFile file,
                                                    String scene,
                                                    Long durationMs,
                                                    Consumer<String> partialConsumer) {
        validateInput(file);
        if (isBlank(iatUrl) || isBlank(iatAppId) || isBlank(iatApiKey) || isBlank(iatApiSecret)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "IAT配置不完整，请检查 xf.iat.*");
        }
        try {
            String text = transcribeByIatWs(file.getBytes(), file.getOriginalFilename(), partialConsumer);
            if (isBlank(text)) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "语音转写结果为空");
            }
            WxSpeechTranscribeVO vo = new WxSpeechTranscribeVO();
            vo.setText(text.trim());
            vo.setScene(isBlank(scene) ? "chat" : scene.trim());
            vo.setDurationMs(durationMs == null ? 0L : Math.max(durationMs, 0L));
            vo.setProvider("xf-iat-ws");
            return vo;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "语音转写（新）失败：" + ex.getMessage());
        }
    }

    private String transcribeByRealtimeWs(byte[] audioBytes) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        String signedUrl = buildSignedUrl(sessionId);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> mergedText = new AtomicReference<>("");
        AtomicReference<String> error = new AtomicReference<>("");

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();
            private final java.util.concurrent.ConcurrentHashMap<Integer, String> segMap = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    String payload = buffer.toString();
                    buffer.setLength(0);
                    try {
                        Map<String, Object> root = objectMapper.readValue(payload, Map.class);
                        String msgType = String.valueOf(root.getOrDefault("msg_type", root.getOrDefault("action", "")));
                        String resType = String.valueOf(root.getOrDefault("res_type", ""));
                        if ("error".equalsIgnoreCase(msgType)) {
                            error.set(String.valueOf(root.getOrDefault("desc", "实时转写失败")));
                            done.countDown();
                        } else if ("frc".equalsIgnoreCase(resType)) {
                            error.set(String.valueOf(root.getOrDefault("data", "实时转写失败")));
                            done.countDown();
                        } else if ("result".equalsIgnoreCase(msgType)) {
                            Object dataObj = root.get("data");
                            Map<String, Object> dataMap = dataObj instanceof Map<?, ?> ? (Map<String, Object>) dataObj : objectMapper.readValue(String.valueOf(dataObj), Map.class);
                            Integer segId = toInt(dataMap.get("seg_id"), segMap.size());
                            String text = extractSentence(dataMap);
                            if (!isBlank(text)) segMap.put(segId, text);
                            mergedText.set(mergeSegments(segMap));
                            if (Boolean.TRUE.equals(dataMap.get("ls"))) {
                                done.countDown();
                            }
                        }
                    } catch (Exception e) {
                        error.set("解析转写响应失败: " + e.getMessage());
                        done.countDown();
                    }
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                done.countDown();
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable throwable) {
                error.set("实时转写连接异常: " + throwable.getMessage());
                done.countDown();
            }
        };

        WebSocket ws = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(signedUrl), listener)
                .join();

        int offset = 0;
        while (offset < audioBytes.length) {
            int len = Math.min(FRAME_BYTES, audioBytes.length - offset);
            byte[] frame = new byte[len];
            System.arraycopy(audioBytes, offset, frame, 0, len);
            ws.sendBinary(java.nio.ByteBuffer.wrap(frame), true).join();
            offset += len;
            Thread.sleep(FRAME_INTERVAL_MS);
        }

        String endPayload = "{\"end\": true, \"sessionId\": \"" + sessionId + "\"}";
        ws.sendBinary(java.nio.ByteBuffer.wrap(endPayload.getBytes(StandardCharsets.UTF_8)), true).join();
        done.await(15, TimeUnit.SECONDS);
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");

        if (!isBlank(error.get())) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, error.get());
        }
        return mergedText.get();
    }

    private String transcribeByIatWs(byte[] audioBytes, String filename, Consumer<String> partialConsumer) throws Exception {
        String signedUrl = buildIatSignedUrl();
        String encoding = resolveIatEncoding(filename);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>("");
        AtomicReference<String> mergedText = new AtomicReference<>("");

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder msgBuffer = new StringBuilder();
            private final ConcurrentHashMap<Integer, String> snTextMap = new ConcurrentHashMap<>();
            private final java.util.Set<Integer> deletedSn = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                msgBuffer.append(data);
                if (last) {
                    String payload = msgBuffer.toString();
                    msgBuffer.setLength(0);
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> root = objectMapper.readValue(payload, Map.class);
                        Integer code = toInt(root.get("code"), 0);
                        if (code != null && code != 0) {
                            error.set(String.valueOf(root.getOrDefault("message", "IAT识别失败(code=" + code + ")")));
                            done.countDown();
                            webSocket.request(1);
                            return null;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) root.get("data");
                        if (dataMap != null) {
                            Integer status = toInt(dataMap.get("status"), 1);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> resultMap = (Map<String, Object>) dataMap.get("result");
                            if (resultMap != null) {
                                int sn = toInt(resultMap.get("sn"), snTextMap.size());
                                String text = extractIatText(resultMap);
                                String pgs = String.valueOf(resultMap.getOrDefault("pgs", ""));
                                if ("rpl".equalsIgnoreCase(pgs)) {
                                    Object rgObj = resultMap.get("rg");
                                    if (rgObj instanceof List<?> rg && rg.size() >= 2) {
                                        int from = toInt(rg.get(0), sn);
                                        int to = toInt(rg.get(1), sn);
                                        for (int i = from; i <= to; i++) {
                                            deletedSn.add(i);
                                        }
                                    }
                                }
                                if (!isBlank(text)) {
                                    snTextMap.put(sn, text);
                                    deletedSn.remove(sn);
                                }
                                mergedText.set(mergeIatSnTexts(snTextMap, deletedSn));
                                if (partialConsumer != null) {
                                    try {
                                        partialConsumer.accept(mergedText.get());
                                    } catch (Exception ignore) {
                                    }
                                }
                            }
                            if (status != null && status == 2) {
                                done.countDown();
                            }
                        }
                    } catch (Exception e) {
                        error.set("解析IAT响应失败: " + e.getMessage());
                        done.countDown();
                    }
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                done.countDown();
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable throwable) {
                error.set("IAT连接异常: " + throwable.getMessage());
                done.countDown();
            }
        };

        WebSocket ws = httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(signedUrl), listener)
                .join();

        int offset = 0;
        int status = 0;
        while (offset < audioBytes.length) {
            int len = Math.min(FRAME_BYTES, audioBytes.length - offset);
            byte[] frameBytes = new byte[len];
            System.arraycopy(audioBytes, offset, frameBytes, 0, len);
            String json = buildIatFrame(status, frameBytes, encoding);
            ws.sendText(json, true).join();
            offset += len;
            status = 1;
            Thread.sleep(FRAME_INTERVAL_MS);
        }
        String lastFrame = "{\"data\":{\"status\":2,\"format\":\"audio/L16;rate=16000\",\"encoding\":\"" + encoding + "\",\"audio\":\"\"}}";
        ws.sendText(lastFrame, true).join();

        done.await(20, TimeUnit.SECONDS);
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");

        if (!isBlank(error.get())) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, error.get());
        }
        return mergedText.get();
    }

    private String buildIatFrame(int status, byte[] frameBytes, String encoding) throws Exception {
        String audioBase64 = Base64.getEncoder().encodeToString(frameBytes == null ? new byte[0] : frameBytes);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (status == 0) {
            sb.append("\"common\":{\"app_id\":\"").append(iatAppId).append("\"},");
            sb.append("\"business\":{")
                    .append("\"language\":\"").append(nullToDefault(iatLanguage, "zh_cn")).append("\",")
                    .append("\"domain\":\"").append(nullToDefault(iatDomain, "iat")).append("\",")
                    .append("\"accent\":\"").append(nullToDefault(iatAccent, "mandarin")).append("\"");
            if (!isBlank(iatDwa)) {
                sb.append(",\"dwa\":\"").append(iatDwa.trim()).append("\"");
            }
            sb.append("},");
        }
        sb.append("\"data\":{")
                .append("\"status\":").append(status).append(",")
                .append("\"format\":\"audio/L16;rate=16000\",")
                .append("\"encoding\":\"").append(encoding).append("\",")
                .append("\"audio\":\"").append(audioBase64).append("\"")
                .append("}}");
        return sb.toString();
    }

    private String buildIatSignedUrl() throws Exception {
        URI uri = URI.create(iatUrl.trim().replace("wss://", "https://").replace("ws://", "http://"));
        String host = uri.getHost();
        String path = isBlank(uri.getPath()) ? "/v2/iat" : uri.getPath();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        String date = formatter.format(ZonedDateTime.now(ZoneOffset.UTC));

        String signatureOrigin = "host: " + host + "\n" +
                "date: " + date + "\n" +
                "GET " + path + " HTTP/1.1";
        String signature = hmacSha256Base64(signatureOrigin, iatApiSecret);
        String authorizationOrigin = "api_key=\"" + iatApiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        String query = "authorization=" + urlEncode(authorization) +
                "&date=" + urlEncode(date) +
                "&host=" + urlEncode(host);

        String wsBase = iatUrl.trim();
        if (wsBase.startsWith("https://")) {
            wsBase = wsBase.replace("https://", "wss://");
        } else if (wsBase.startsWith("http://")) {
            wsBase = wsBase.replace("http://", "ws://");
        }
        return wsBase + "?" + query;
    }

    private String hmacSha256Base64(String value, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signBytes);
    }

    private String extractIatText(Map<String, Object> resultMap) {
        Object wsObj = resultMap.get("ws");
        if (!(wsObj instanceof List<?> wsList)) return "";
        StringBuilder sb = new StringBuilder();
        for (Object ws : wsList) {
            if (!(ws instanceof Map<?, ?> wsMap)) continue;
            Object cwObj = wsMap.get("cw");
            if (!(cwObj instanceof List<?> cwList) || cwList.isEmpty()) continue;
            Object cw0 = cwList.get(0);
            if (!(cw0 instanceof Map<?, ?> cwMap)) continue;
            Object word = cwMap.get("w");
            if (word != null) sb.append(word);
        }
        return sb.toString();
    }

    private String mergeIatSnTexts(ConcurrentHashMap<Integer, String> snTextMap, java.util.Set<Integer> deletedSn) {
        return snTextMap.entrySet().stream()
                .filter(e -> !deletedSn.contains(e.getKey()))
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.joining());
    }

    private String resolveIatEncoding(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".mp3")) return "lame";
        return "raw";
    }

    private void validateInput(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "音频文件不能为空");
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "音频文件不能超过10MB");
        }
        String filename = file.getOriginalFilename();
        if (isBlank(filename)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "音频文件名不能为空");
        }
        String lower = filename.toLowerCase();
        boolean supported = SUPPORTED_SUFFIX.stream().anyMatch(lower::endsWith);
        if (!supported) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持 mp3/wav/m4a/aac/pcm 格式");
        }
    }

    private String buildSignedUrl(String uuid) throws Exception {
        String utc = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                .format(ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));

        java.util.Map<String, String> params = new java.util.TreeMap<>();
        params.put("accessKeyId", accessKeyId);
        params.put("appId", appId);
        params.put("audio_encode", isBlank(audioEncode) ? "pcm_s16le" : audioEncode.trim());
        params.put("lang", isBlank(lang) ? "autodialect" : lang.trim());
        params.put("samplerate", String.valueOf(sampleRate == null ? 16000 : sampleRate));
        params.put("utc", utc);
        params.put("uuid", uuid);

        String baseString = buildBaseString(params);
        String signature = hmacSha1Base64(baseString, accessKeySecret);
        params.put("signature", signature);

        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) query.append("&");
            query.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            first = false;
        }
        return astUrl + "?" + query;
    }

    private String hmacSha1Base64(String value, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] signBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signBytes);
    }

    private String buildBaseString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractSentence(Map<String, Object> data) {
        Object cnObj = data.get("cn");
        if (!(cnObj instanceof Map<?, ?> cn)) return "";
        Object stObj = cn.get("st");
        if (!(stObj instanceof Map<?, ?> st)) return "";
        Object rtObj = st.get("rt");
        if (!(rtObj instanceof List<?> rtList) || rtList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object rt : rtList) {
            if (!(rt instanceof Map<?, ?> rtMap)) continue;
            Object wsObj = rtMap.get("ws");
            if (!(wsObj instanceof List<?> wsList)) continue;
            for (Object ws : wsList) {
                if (!(ws instanceof Map<?, ?> wsMap)) continue;
                Object cwObj = wsMap.get("cw");
                if (!(cwObj instanceof List<?> cwList) || cwList.isEmpty()) continue;
                Object cw0 = cwList.get(0);
                if (!(cw0 instanceof Map<?, ?> cwMap)) continue;
                Object w = cwMap.get("w");
                if (w != null) sb.append(w);
            }
        }
        return sb.toString();
    }

    private String mergeSegments(java.util.concurrent.ConcurrentHashMap<Integer, String> segMap) {
        java.util.List<Map.Entry<Integer, String>> entries = new java.util.ArrayList<>(segMap.entrySet());
        entries.sort(java.util.Comparator.comparingInt(Map.Entry::getKey));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> e : entries) {
            if (e.getValue() != null) sb.append(e.getValue());
        }
        return sb.toString();
    }

    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String nullToDefault(String value, String defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
