package campus.ai.rag.service.mapper.rbac;

import campus.ai.rag.service.entity.rbac.RbacUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RbacUserMapper extends BaseMapper<RbacUserEntity> {

    @Select("SELECT id, username, password, nickname, status, super_admin_marker, created_at, updated_at "
            + "FROM rbac_user WHERE username = #{username} LIMIT 1")
    RbacUserEntity findByUsername(@Param("username") String username);

    @Select("SELECT r.role_code FROM rbac_role r "
            + "JOIN rbac_user_role ur ON ur.role_id = r.id "
            + "JOIN rbac_user u ON u.id = ur.user_id "
            + "WHERE u.username = #{username}")
    List<String> findRoleCodesByUsername(@Param("username") String username);

    @Update("UPDATE rbac_user SET super_admin_marker = NULL "
            + "WHERE super_admin_marker = 'Y' AND username <> #{username}")
    int clearSuperAdminMarkerExcept(@Param("username") String username);
}

