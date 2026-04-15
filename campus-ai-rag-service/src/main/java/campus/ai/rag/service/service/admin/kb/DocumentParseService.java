package campus.ai.rag.service.service.admin.kb;

import java.nio.file.Path;

public interface DocumentParseService {

    /**
     * 解析文档为纯文本，支持 pdf/txt/md/doc/docx。
     */
    String parseToText(Path path, String fileExt);
}

