package campus.ai.rag.service.ws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WxRealtimeSpeechWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(WxRealtimeSpeechWebSocketHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @Value("${xf.rtasr.role-type:0}")
    private Integer roleType;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ConcurrentHashMap<String, SessionBridge> sessionBridgeMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SessionBridge bridge = new SessionBridge(session);
        sessionBridgeMap.put(session.getId(), bridge);
        sendToClient(session, Map.of("event", "ready", "message", "realtime socket connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SessionBridge bridge = sessionBridgeMap.get(session.getId());
        if (bridge == null) return;
        Map<String, Object> payload = OBJECT_MAPPER.readValue(message.getPayload(), new TypeReference<>() {
        });
        String action = String.valueOf(payload.getOrDefault("action", ""));
        if ("start".equalsIgnoreCase(action)) {
            String langParam = String.valueOf(payload.getOrDefault("lang", lang));
            bridge.startUpstream(langParam);
            return;
        }
        if ("end".equalsIgnoreCase(action)) {
            bridge.endUpstream();
            return;
        }
        if ("ping".equalsIgnoreCase(action)) {
            session.sendMessage(new PongMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SessionBridge bridge = sessionBridgeMap.get(session.getId());
        if (bridge == null) return;
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        bridge.forwardAudio(bytes);
    }

    @Override
    public void handlePongMessage(WebSocketSession session, PongMessage message) {
        // keep-alive noop
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionBridge bridge = sessionBridgeMap.remove(session.getId());
        if (bridge != null) {
            bridge.shutdown();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        SessionBridge bridge = sessionBridgeMap.remove(session.getId());
        if (bridge != null) {
            bridge.shutdown();
        }
        log.warn("wx realtime speech transport error, sid={}", session.getId(), exception);
    }

    private void sendToClient(WebSocketSession session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("send client message failed", e);
        }
    }

    private String buildSignedUrl(String langParam, String uuid) throws Exception {
        if (isBlank(appId) || isBlank(accessKeyId) || isBlank(accessKeySecret)) {
            throw new IllegalStateException("xf.rtasr.app-id/api-key/api-secret 未完整配置");
        }
        String utc = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                .format(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")));
        String audio = isBlank(audioEncode) ? "pcm_s16le" : audioEncode.trim();
        int sr = sampleRate == null ? 16000 : sampleRate;
        String realLang = isBlank(langParam) ? "autodialect" : langParam.trim();

        java.util.Map<String, String> params = new java.util.TreeMap<>();
        params.put("accessKeyId", accessKeyId);
        params.put("appId", appId);
        params.put("audio_encode", audio);
        params.put("lang", realLang);
        params.put("samplerate", String.valueOf(sr));
        params.put("utc", utc);
        params.put("uuid", uuid);
        if (roleType != null) {
            params.put("role_type", String.valueOf(roleType));
        }

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
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signBytes);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(Objects.toString(value, ""), StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private final class SessionBridge {
        private final WebSocketSession clientSession;
        private final ConcurrentHashMap<Integer, String> segmentMap = new ConcurrentHashMap<>();
        private final AtomicBoolean ending = new AtomicBoolean(false);
        private final String sessionId = UUID.randomUUID().toString();
        private volatile WebSocket upstream;

        private SessionBridge(WebSocketSession clientSession) {
            this.clientSession = clientSession;
        }

        private void startUpstream(String langParam) {
            try {
                String signedUrl = buildSignedUrl(langParam, sessionId);
                WebSocket.Listener listener = new UpstreamListener(this);
                this.upstream = httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(signedUrl), listener)
                        .join();
                sendToClient(clientSession, Map.of("event", "started", "message", "upstream connected"));
            } catch (Exception e) {
                sendToClient(clientSession, Map.of("event", "error", "message", "连接实时转写服务失败: " + e.getMessage()));
            }
        }

        private void forwardAudio(byte[] audioBytes) {
            if (audioBytes == null || audioBytes.length == 0) return;
            if (upstream == null) return;
            upstream.sendBinary(ByteBuffer.wrap(audioBytes), true);
        }

        private void endUpstream() {
            if (ending.getAndSet(true)) return;
            try {
                if (upstream != null) {
                    String endPayload = "{\"end\": true, \"sessionId\": \"" + sessionId + "\"}";
                    upstream.sendBinary(ByteBuffer.wrap(endPayload.getBytes(StandardCharsets.UTF_8)), true);
                }
            } catch (Exception e) {
                sendToClient(clientSession, Map.of("event", "error", "message", "结束流失败: " + e.getMessage()));
            }
        }

        private void onUpstreamResult(Map<String, Object> root) {
            String msgType = String.valueOf(root.getOrDefault("msg_type", root.getOrDefault("action", "")));
            String resType = String.valueOf(root.getOrDefault("res_type", ""));
            if ("error".equalsIgnoreCase(msgType)) {
                sendToClient(clientSession, Map.of(
                        "event", "error",
                        "code", String.valueOf(root.getOrDefault("code", "")),
                        "message", String.valueOf(root.getOrDefault("desc", "实时转写错误"))
                ));
                return;
            }
            if (!"result".equalsIgnoreCase(msgType) && !"started".equalsIgnoreCase(msgType)) {
                return;
            }
            if ("frc".equalsIgnoreCase(resType)) {
                Object errData = root.get("data");
                sendToClient(clientSession, Map.of("event", "error", "message", Objects.toString(errData, "转写服务异常")));
                return;
            }

            Object dataObj = root.get("data");
            if (dataObj == null) return;
            try {
                Map<String, Object> data;
                if (dataObj instanceof Map<?, ?> m) {
                    data = OBJECT_MAPPER.convertValue(m, new TypeReference<>() {
                    });
                } else {
                    data = OBJECT_MAPPER.readValue(String.valueOf(dataObj), new TypeReference<>() {
                    });
                }
                Integer segId = toInt(data.get("seg_id"), segmentMap.size());
                String sentence = extractSentence(data);
                if (!isBlank(sentence)) {
                    segmentMap.put(segId, sentence);
                }
                String fullText = mergeSegments();
                boolean isFinal = "0".equals(findType(data)) || Boolean.TRUE.equals(data.get("ls"));
                sendToClient(clientSession, Map.of(
                        "event", "transcript",
                        "segId", segId,
                        "text", sentence == null ? "" : sentence,
                        "fullText", fullText,
                        "isFinal", isFinal
                ));
            } catch (Exception e) {
                log.warn("parse upstream result failed: {}", dataObj, e);
            }
        }

        private String findType(Map<String, Object> data) {
            try {
                Object cn = data.get("cn");
                if (!(cn instanceof Map<?, ?> cnMap)) return "";
                Object st = cnMap.get("st");
                if (!(st instanceof Map<?, ?> stMap)) return "";
                return Objects.toString(stMap.get("type"), "");
            } catch (Exception e) {
                return "";
            }
        }

        @SuppressWarnings("unchecked")
        private String extractSentence(Map<String, Object> data) {
            if (data.get("src") != null) {
                return String.valueOf(data.get("src"));
            }
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

        private String mergeSegments() {
            List<Map.Entry<Integer, String>> entries = new ArrayList<>(segmentMap.entrySet());
            entries.sort(Comparator.comparingInt(Map.Entry::getKey));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, String> e : entries) {
                if (e.getValue() != null) sb.append(e.getValue());
            }
            return sb.toString().trim();
        }

        private int toInt(Object v, int fallback) {
            if (v == null) return fallback;
            if (v instanceof Number n) return n.intValue();
            try {
                return Integer.parseInt(String.valueOf(v));
            } catch (Exception e) {
                return fallback;
            }
        }

        private void shutdown() {
            try {
                if (upstream != null) {
                    upstream.sendClose(WebSocket.NORMAL_CLOSURE, "client closed");
                }
            } catch (Exception ignored) {
            }
        }
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

    private final class UpstreamListener implements WebSocket.Listener {
        private final SessionBridge bridge;
        private final StringBuilder textBuffer = new StringBuilder();

        private UpstreamListener(SessionBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String payload = textBuffer.toString();
                textBuffer.setLength(0);
                try {
                    Map<String, Object> root = OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
                    });
                    bridge.onUpstreamResult(root);
                } catch (Exception e) {
                    log.warn("upstream text parse error: {}", payload, e);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            String fullText = bridge.mergeSegments();
            WxRealtimeSpeechWebSocketHandler.this.sendToClient(bridge.clientSession, Map.of(
                    "event", "completed",
                    "finalText", fullText,
                    "code", statusCode,
                    "reason", reason == null ? "" : reason
            ));
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            WxRealtimeSpeechWebSocketHandler.this.sendToClient(bridge.clientSession, Map.of("event", "error", "message", "实时转写连接异常: " + error.getMessage()));
        }
    }
}
