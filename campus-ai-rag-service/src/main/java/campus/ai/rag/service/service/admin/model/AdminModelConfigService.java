package campus.ai.rag.service.service.admin.model;

import campus.ai.rag.service.dto.admin.model.AiModelConfigCreateDTO;
import campus.ai.rag.service.dto.admin.model.AiModelConfigUpdateDTO;
import campus.ai.rag.service.dto.admin.model.AiModelTestChatDTO;
import campus.ai.rag.service.vo.admin.model.AiModelConfigVO;
import campus.ai.rag.service.vo.admin.model.AiModelTestChatVO;

import java.util.List;

public interface AdminModelConfigService {

    AiModelConfigVO create(AiModelConfigCreateDTO dto);

    AiModelConfigVO update(Long id, AiModelConfigUpdateDTO dto);

    void delete(Long id);

    AiModelConfigVO getById(Long id);

    List<AiModelConfigVO> listAll();

    AiModelTestChatVO testChat(AiModelTestChatDTO dto);
}

