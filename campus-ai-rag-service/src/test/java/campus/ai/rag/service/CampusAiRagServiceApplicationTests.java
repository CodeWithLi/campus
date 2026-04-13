package campus.ai.rag.service;

import campus.ai.rag.service.entity.rbac.RbacRoleEntity;
import campus.ai.rag.service.entity.rbac.RbacUserEntity;
import campus.ai.rag.service.entity.rbac.RbacUserRoleEntity;
import campus.ai.rag.service.mapper.rbac.RbacRoleMapper;
import campus.ai.rag.service.mapper.rbac.RbacUserMapper;
import campus.ai.rag.service.mapper.rbac.RbacUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;

@SpringBootTest
class CampusAiRagServiceApplicationTests {

    @Autowired
    private RbacUserMapper rbacUserMapper;

    @Autowired
    private RbacRoleMapper rbacRoleMapper;

    @Autowired
    private RbacUserRoleMapper rbacUserRoleMapper;

    @Test
    void contextLoads() {
    }

    /**
     * 通过 MyBatis-Plus 插入两个后台测试用户，密码使用 BCrypt 存储。
     */
    @Test
    void insertRbacTestUsersWithBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 避免重复执行导致 username 唯一索引冲突
        rbacUserMapper.delete(new LambdaQueryWrapper<RbacUserEntity>()
                .in(RbacUserEntity::getUsername, "admin", "super"));

        // 仅允许一个超级管理员标记：把其他账号上的 Y 清空
        rbacUserMapper.clearSuperAdminMarkerExcept("super");

        RbacUserEntity adminUser = new RbacUserEntity();
        adminUser.setUsername("admin");
        adminUser.setPassword(encoder.encode("admin123"));
        adminUser.setNickname("管理员");
        adminUser.setStatus(1);
        adminUser.setSuperAdminMarker(null);
        rbacUserMapper.insert(adminUser);

        RbacUserEntity superUser = new RbacUserEntity();
        superUser.setUsername("super");
        superUser.setPassword(encoder.encode("super123"));
        superUser.setNickname("超级管理员");
        superUser.setStatus(1);
        superUser.setSuperAdminMarker("Y");
        rbacUserMapper.insert(superUser);

        RbacRoleEntity adminRole = rbacRoleMapper.selectOne(new LambdaQueryWrapper<RbacRoleEntity>()
                .eq(RbacRoleEntity::getRoleCode, "ADMIN")
                .last("limit 1"));
        RbacRoleEntity superRole = rbacRoleMapper.selectOne(new LambdaQueryWrapper<RbacRoleEntity>()
                .eq(RbacRoleEntity::getRoleCode, "SUPER_ADMIN")
                .last("limit 1"));

        if (Objects.isNull(adminRole) || Objects.isNull(superRole)) {
            throw new IllegalStateException("缺少基础角色，请先初始化 rbac_role 表数据");
        }

        // 删除旧角色关系，避免重复
        rbacUserRoleMapper.delete(new LambdaQueryWrapper<RbacUserRoleEntity>()
                .in(RbacUserRoleEntity::getUserId, adminUser.getId(), superUser.getId()));

        RbacUserRoleEntity adminUserRole = new RbacUserRoleEntity();
        adminUserRole.setUserId(adminUser.getId());
        adminUserRole.setRoleId(adminRole.getId());
        rbacUserRoleMapper.insert(adminUserRole);

        RbacUserRoleEntity superUserRole = new RbacUserRoleEntity();
        superUserRole.setUserId(superUser.getId());
        superUserRole.setRoleId(superRole.getId());
        rbacUserRoleMapper.insert(superUserRole);
    }
}
