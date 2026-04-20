package campus.ai.rag.service.service.compliance;

public interface IflyImageAuditService {

    /**
     * @param imageBytes 原图字节
     * @return true 表示通过或未启用
     */
    boolean isPass(byte[] imageBytes);

    /**
     * 不通过时抛业务异常。
     */
    void assertPass(byte[] imageBytes);
}
