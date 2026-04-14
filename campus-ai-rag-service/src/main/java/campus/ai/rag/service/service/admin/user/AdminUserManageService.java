package campus.ai.rag.service.service.admin.user;

import campus.ai.rag.service.dto.admin.user.AdminUserCreateDTO;
import campus.ai.rag.service.dto.admin.user.AdminUserResetPasswordDTO;
import campus.ai.rag.service.dto.admin.user.AdminUserUpdateDTO;
import campus.ai.rag.service.vo.admin.user.AdminUserVO;

import java.util.List;

public interface AdminUserManageService {

    AdminUserVO create(AdminUserCreateDTO dto);

    AdminUserVO update(Long id, AdminUserUpdateDTO dto);

    void resetPassword(Long id, AdminUserResetPasswordDTO dto);

    void delete(Long id);

    AdminUserVO getById(Long id);

    List<AdminUserVO> listAll();
}

