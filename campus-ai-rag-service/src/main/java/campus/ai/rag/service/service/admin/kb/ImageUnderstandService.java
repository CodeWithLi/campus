package campus.ai.rag.service.service.admin.kb;

import campus.ai.rag.service.vo.admin.kb.KbDocumentImagePreviewVO;

import java.nio.file.Path;
import java.util.List;

public interface ImageUnderstandService {

    /**
     * 抽取文档中的图片并调用模型理解，返回逐图结果。
     */
    List<KbDocumentImagePreviewVO.Item> understandImages(Long documentId, Path filePath, String fileExt);
}

