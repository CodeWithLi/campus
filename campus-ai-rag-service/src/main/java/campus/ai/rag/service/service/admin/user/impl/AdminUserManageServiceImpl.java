package campus.ai.rag.service.service.admin.user.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.user.AdminUserCreateDTO;
import campus.ai.rag.service.dto.admin.user.AdminUserResetPasswordDTO;
import campus.ai.rag.service.dto.admin.user.AdminUserUpdateDTO;
import campus.ai.rag.service.entity.rbac.RbacRoleEntity;
import campus.ai.rag.service.entity.rbac.RbacUserEntity;
import campus.ai.rag.service.entity.rbac.RbacUserRoleEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.rbac.RbacRoleMapper;
import campus.ai.rag.service.mapper.rbac.RbacUserMapper;
import campus.ai.rag.service.mapper.rbac.RbacUserRoleMapper;
import campus.ai.rag.service.service.admin.user.AdminUserManageService;
import campus.ai.rag.service.vo.admin.user.AdminUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminUserManageServiceImpl implements AdminUserManageService {

    private final RbacUserMapper rbacUserMapper;
    private final RbacRoleMapper rbacRoleMapper;
    private final RbacUserRoleMapper rbacUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserManageServiceImpl(RbacUserMapper rbacUserMapper,
                                      RbacRoleMapper rbacRoleMapper,
                                      RbacUserRoleMapper rbacUserRoleMapper,
                                      PasswordEncoder passwordEncoder) {
        this.rbacUserMapper = rbacUserMapper;
        this.rbacRoleMapper = rbacRoleMapper;
        this.rbacUserRoleMapper = rbacUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AdminUserVO create(AdminUserCreateDTO dto) {
        validateCreateDto(dto);
        String username = dto.getUsername().trim();

        RbacUserEntity exists = rbacUserMapper.selectOne(new LambdaQueryWrapper<RbacUserEntity>()
                .eq(RbacUserEntity::getUsername, username)
                .last("limit 1"));
        if (Objects.nonNull(exists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "管理员用户名已存在");
        }

        RbacRoleEntity roleEntity = getRequiredRole(dto.getRoleId());

        RbacUserEntity userEntity = new RbacUserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        userEntity.setNickname(dto.getNickname().trim());
        userEntity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userEntity.setSuperAdminMarker("N");
        rbacUserMapper.insert(userEntity);

        bindSingleRole(userEntity.getId(), roleEntity.getId());
        return buildUserVO(userEntity.getId());
    }

    @Override
    public AdminUserVO update(Long id, AdminUserUpdateDTO dto) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员ID不能为空");
        }
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "更新参数不能为空");
        }

        RbacUserEntity userEntity = getRequiredUser(id);
        if (!isBlank(dto.getNickname())) {
            userEntity.setNickname(dto.getNickname().trim());
        }
        if (Objects.nonNull(dto.getStatus())) {
            if (Objects.equals(userEntity.getSuperAdminMarker(), "Y") && Objects.equals(dto.getStatus(), 0)) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "超级管理员账号不允许禁用");
            }
            userEntity.setStatus(dto.getStatus());
        }
        rbacUserMapper.updateById(userEntity);

        if (Objects.nonNull(dto.getRoleId())) {
            RbacRoleEntity roleEntity = getRequiredRole(dto.getRoleId());
            bindSingleRole(userEntity.getId(), roleEntity.getId());
        }

        return buildUserVO(userEntity.getId());
    }

    @Override
    public void resetPassword(Long id, AdminUserResetPasswordDTO dto) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员ID不能为空");
        }
        if (Objects.isNull(dto) || isBlank(dto.getNewPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "新密码不能为空");
        }

        RbacUserEntity userEntity = getRequiredUser(id);
        userEntity.setPassword(passwordEncoder.encode(dto.getNewPassword().trim()));
        rbacUserMapper.updateById(userEntity);
    }

    @Override
    public void delete(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员ID不能为空");
        }
        RbacUserEntity userEntity = getRequiredUser(id);
        if (Objects.equals(userEntity.getSuperAdminMarker(), "Y")) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "超级管理员账号不允许删除");
        }

        rbacUserRoleMapper.delete(new LambdaQueryWrapper<RbacUserRoleEntity>()
                .eq(RbacUserRoleEntity::getUserId, id));
        rbacUserMapper.deleteById(id);
    }

    @Override
    public AdminUserVO getById(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员ID不能为空");
        }
        getRequiredUser(id);
        return buildUserVO(id);
    }

    @Override
    public List<AdminUserVO> listAll() {
        List<RbacUserEntity> users = rbacUserMapper.selectList(new LambdaQueryWrapper<RbacUserEntity>()
                .orderByDesc(RbacUserEntity::getStatus)
                .orderByDesc(RbacUserEntity::getId));
        return users.stream().map(user -> buildUserVO(user.getId())).collect(Collectors.toList());
    }

    private void validateCreateDto(AdminUserCreateDTO dto) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "创建参数不能为空");
        }
        if (isBlank(dto.getUsername())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员用户名不能为空");
        }
        if (isBlank(dto.getPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员密码不能为空");
        }
        if (isBlank(dto.getNickname())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "管理员昵称不能为空");
        }
        if (Objects.isNull(dto.getRoleId())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "角色ID不能为空");
        }
    }

    private RbacUserEntity getRequiredUser(Long id) {
        RbacUserEntity entity = rbacUserMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "管理员不存在");
        }
        return entity;
    }

    private RbacRoleEntity getRequiredRole(Long roleId) {
        RbacRoleEntity roleEntity = rbacRoleMapper.selectById(roleId);
        if (Objects.isNull(roleEntity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "角色不存在");
        }
        return roleEntity;
    }

    private void bindSingleRole(Long userId, Long roleId) {
        rbacUserRoleMapper.delete(new LambdaQueryWrapper<RbacUserRoleEntity>()
                .eq(RbacUserRoleEntity::getUserId, userId));

        RbacUserRoleEntity userRole = new RbacUserRoleEntity();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        rbacUserRoleMapper.insert(userRole);
    }

    private AdminUserVO buildUserVO(Long userId) {
        RbacUserEntity userEntity = rbacUserMapper.selectById(userId);
        if (Objects.isNull(userEntity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "管理员不存在");
        }

        RbacUserRoleEntity userRoleEntity = rbacUserRoleMapper.selectOne(new LambdaQueryWrapper<RbacUserRoleEntity>()
                .eq(RbacUserRoleEntity::getUserId, userId)
                .last("limit 1"));
        RbacRoleEntity roleEntity = null;
        if (Objects.nonNull(userRoleEntity)) {
            roleEntity = rbacRoleMapper.selectById(userRoleEntity.getRoleId());
        }

        AdminUserVO vo = new AdminUserVO();
        vo.setId(userEntity.getId());
        vo.setUsername(userEntity.getUsername());
        vo.setNickname(userEntity.getNickname());
        vo.setStatus(userEntity.getStatus());
        vo.setSuperAdminMarker(userEntity.getSuperAdminMarker());
        if (Objects.nonNull(userRoleEntity)) {
            vo.setRoleId(userRoleEntity.getRoleId());
        }
        if (Objects.nonNull(roleEntity)) {
            vo.setRoleCode(roleEntity.getRoleCode());
            vo.setRoleName(roleEntity.getRoleName());
        }
        vo.setCreatedAt(userEntity.getCreatedAt());
        vo.setUpdatedAt(userEntity.getUpdatedAt());
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

