package campus.ai.rag.service.service.admin.kb;

import campus.ai.rag.service.dto.admin.kb.KbCategoryCreateDTO;
import campus.ai.rag.service.dto.admin.kb.KbCategoryUpdateDTO;
import campus.ai.rag.service.vo.admin.kb.KbCategoryVO;

import java.util.List;

public interface AdminKbCategoryService {

    KbCategoryVO create(KbCategoryCreateDTO dto);

    KbCategoryVO update(Long id, KbCategoryUpdateDTO dto);

    void delete(Long id);

    KbCategoryVO getById(Long id);

    List<KbCategoryVO> listAll();
}

