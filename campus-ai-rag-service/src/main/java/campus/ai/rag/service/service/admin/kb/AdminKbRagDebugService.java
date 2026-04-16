package campus.ai.rag.service.service.admin.kb;

import campus.ai.rag.service.dto.admin.kb.KbRagDebugDTO;
import campus.ai.rag.service.vo.admin.kb.KbRagDebugVO;

public interface AdminKbRagDebugService {

    KbRagDebugVO debug(KbRagDebugDTO dto);
}

