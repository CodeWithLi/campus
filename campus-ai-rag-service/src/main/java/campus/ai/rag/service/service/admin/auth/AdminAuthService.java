package campus.ai.rag.service.service.admin.auth;

import campus.ai.rag.service.dto.admin.AdminLoginDTO;
import campus.ai.rag.service.vo.admin.AdminLoginVO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginDTO dto);
}

