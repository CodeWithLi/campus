package campus.ai.rag.service.service.compliance;

/**
 * 讯飞文本合规 syncText。
 */
public interface IflyTextAuditService {

    /**
     * @return true 表示通过或未启用合规
     */
    boolean isPass(String text);

    /**
     * 与 {@link #isPass(String)} 相同，不合规时抛出业务异常（便于统一错误信息）。
     */
    void assertPass(String text);
}
