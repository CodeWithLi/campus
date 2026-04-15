package campus.ai.rag.service.service.admin.kb;

import campus.ai.rag.service.dto.admin.kb.KbDocumentUpdateStatusDTO;
import campus.ai.rag.service.dto.admin.kb.KbDocumentUploadDTO;
import campus.ai.rag.service.dto.admin.kb.KbImageUnderstandIndexDTO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentImagePreviewVO;
import campus.ai.rag.service.vo.admin.kb.KbImageUnderstandTaskVO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentPageVO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminKbDocumentService {

    KbDocumentVO upload(KbDocumentUploadDTO dto, MultipartFile file);

    /**
     * 上传后立即执行：解析 -> 切片向量化入库。
     * 适用于“一步到位”场景。
     */
    KbDocumentVO uploadAndIndex(KbDocumentUploadDTO dto, MultipartFile file);

    KbDocumentVO updateStatus(Long id, KbDocumentUpdateStatusDTO dto);

    KbDocumentVO parse(Long id);

    KbDocumentVO index(Long id);

    /**
     * 预览文档中图片理解结果（不执行向量化）。
     */
    KbDocumentImagePreviewVO previewImageUnderstand(Long id);

    /**
     * 一键执行：图片理解 -> 合并文本 -> 切片向量化入库。
     */
    KbDocumentVO imageUnderstandAndIndex(Long id);

    /**
     * 异步提交图片理解预览任务。
     */
    KbImageUnderstandTaskVO submitImageUnderstandPreviewTask(Long id);

    /**
     * 异步提交图片理解后一键向量化任务。
     */
    KbImageUnderstandTaskVO submitImageUnderstandIndexTask(Long id, KbImageUnderstandIndexDTO dto);

    /**
     * 基于已存在的图片理解任务，异步提交一键向量化任务。
     */
    KbImageUnderstandTaskVO submitImageUnderstandIndexTaskByTaskId(String taskId, KbImageUnderstandIndexDTO dto);

    /**
     * 查询图片理解异步任务状态与结果。
     */
    KbImageUnderstandTaskVO getImageUnderstandTask(String taskId);

    /**
     * 查询图片理解任务列表（可按文档ID过滤）。
     */
    List<KbImageUnderstandTaskVO> listImageUnderstandTasks(Long documentId);

    KbDocumentVO retry(Long id);

    KbDocumentVO getById(Long id);

    KbDocumentPageVO listPage(Long pageNum, Long pageSize);

    List<KbDocumentVO> listAll();
}

