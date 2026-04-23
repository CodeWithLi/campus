package campus.ai.rag.service.service.admin.guide;

import campus.ai.rag.service.dto.admin.guide.AdminMaterialTemplateSaveDTO;
import campus.ai.rag.service.vo.admin.guide.AdminMaterialTemplateVO;

import java.util.List;

public interface AdminMaterialTemplateService {

    List<AdminMaterialTemplateVO> listAll();

    AdminMaterialTemplateVO getById(Long id);

    AdminMaterialTemplateVO create(AdminMaterialTemplateSaveDTO dto);

    AdminMaterialTemplateVO update(Long id, AdminMaterialTemplateSaveDTO dto);

    void delete(Long id);
}

