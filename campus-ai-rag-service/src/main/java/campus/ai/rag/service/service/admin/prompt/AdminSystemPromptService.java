package campus.ai.rag.service.service.admin.prompt;

import campus.ai.rag.service.dto.admin.prompt.AiSystemPromptCreateDTO;
import campus.ai.rag.service.dto.admin.prompt.AiSystemPromptUpdateDTO;
import campus.ai.rag.service.vo.admin.prompt.AiSystemPromptVO;

import java.util.List;

public interface AdminSystemPromptService {

    AiSystemPromptVO create(AiSystemPromptCreateDTO dto);

    AiSystemPromptVO update(Long id, AiSystemPromptUpdateDTO dto);

    void delete(Long id);

    AiSystemPromptVO getById(Long id);

    List<AiSystemPromptVO> listAll();
}

