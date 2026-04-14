package campus.ai.rag.service.service.admin.wxuser;

import campus.ai.rag.service.dto.admin.wxuser.AdminWxUserPowerPointsDTO;
import campus.ai.rag.service.dto.admin.wxuser.AdminWxUserUpdateDTO;
import campus.ai.rag.service.vo.admin.wxuser.AdminWxUserVO;

import java.util.List;

public interface AdminWxUserManageService {

    List<AdminWxUserVO> listAll();

    AdminWxUserVO getById(Long id);

    AdminWxUserVO update(Long id, AdminWxUserUpdateDTO dto);

    AdminWxUserVO updatePowerPoints(Long id, AdminWxUserPowerPointsDTO dto);

    void freeze(Long id);

    void unfreeze(Long id);
}

