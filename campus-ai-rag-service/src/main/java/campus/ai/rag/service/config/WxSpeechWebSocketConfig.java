package campus.ai.rag.service.config;

import campus.ai.rag.service.ws.WxRealtimeSpeechWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WxSpeechWebSocketConfig implements WebSocketConfigurer {

    private final WxRealtimeSpeechWebSocketHandler wxRealtimeSpeechWebSocketHandler;

    public WxSpeechWebSocketConfig(WxRealtimeSpeechWebSocketHandler wxRealtimeSpeechWebSocketHandler) {
        this.wxRealtimeSpeechWebSocketHandler = wxRealtimeSpeechWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wxRealtimeSpeechWebSocketHandler, "/ws/wx/speech/realtime")
                .setAllowedOriginPatterns("*");
    }
}
