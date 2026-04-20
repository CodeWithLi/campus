package campus.ai.rag.service.service.prompt;

/**
 * 运行时系统提示词拼接服务：
 * - CHAT：拼接 BOTH + CHAT
 * - RAG：拼接 BOTH + RAG，并注入检索上下文
 * - RESUME：拼接 BOTH + RESUME
 */
public interface SystemPromptRuntimeService {

    /**
     * 构建最终发送给大模型的消息内容（单条 userMessage）。
     *
     * @param scene      CHAT / RAG / RESUME / POST
     * @param userMessage 用户输入
     * @param ragContext  检索上下文（仅 RAG 时有效）
     */
    String buildPromptedMessage(String scene, String userMessage, String ragContext);
}

