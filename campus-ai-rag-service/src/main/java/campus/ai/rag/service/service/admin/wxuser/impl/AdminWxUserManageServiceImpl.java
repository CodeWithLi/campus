package campus.ai.rag.service.service.admin.wxuser.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.wxuser.AdminWxUserPowerPointsDTO;
import campus.ai.rag.service.dto.admin.wxuser.AdminWxUserUpdateDTO;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.admin.wxuser.AdminWxUserManageService;
import campus.ai.rag.service.vo.admin.wxuser.AdminWxUserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminWxUserManageServiceImpl implements AdminWxUserManageService {

    private final WxUserMapper wxUserMapper;

    public AdminWxUserManageServiceImpl(WxUserMapper wxUserMapper) {
        this.wxUserMapper = wxUserMapper;
    }

    @Override
    public List<AdminWxUserVO> listAll() {
        List<WxUserEntity> users = wxUserMapper.selectList(new LambdaQueryWrapper<WxUserEntity>()
                .orderByDesc(WxUserEntity::getStatus)
                .orderByDesc(WxUserEntity::getId));
        return users.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AdminWxUserVO getById(Long id) {
        WxUserEntity user = getRequiredUser(id);
        return toVO(user);
    }

    @Override
    public AdminWxUserVO update(Long id, AdminWxUserUpdateDTO dto) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "更新参数不能为空");
        }
        WxUserEntity user = getRequiredUser(id);

        if (dto.getNickname() != null) {
            user.setNickname(trimOrNull(dto.getNickname()));
        }
        if (dto.getEmail() != null) {
            String email = trimOrNull(dto.getEmail());
            if (email != null && (!email.contains("@") || !email.contains("."))) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱格式不正确");
            }
            user.setEmail(email);
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        if (dto.getPowerPoints() != null) {
            if (dto.getPowerPoints() < 0) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不能小于0");
            }
            user.setPowerPoints(dto.getPowerPoints());
        }
        wxUserMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public AdminWxUserVO updatePowerPoints(Long id, AdminWxUserPowerPointsDTO dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getPowerPoints())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不能为空");
        }
        if (dto.getPowerPoints() < 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不能小于0");
        }
        WxUserEntity user = getRequiredUser(id);
        user.setPowerPoints(dto.getPowerPoints());
        wxUserMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void freeze(Long id) {
        WxUserEntity user = getRequiredUser(id);
        user.setStatus(0);
        wxUserMapper.updateById(user);
    }

    @Override
    public void unfreeze(Long id) {
        WxUserEntity user = getRequiredUser(id);
        user.setStatus(1);
        wxUserMapper.updateById(user);
    }

    private WxUserEntity getRequiredUser(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户ID不能为空");
        }
        WxUserEntity user = wxUserMapper.selectById(id);
        if (Objects.isNull(user)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "微信用户不存在");
        }
        return user;
    }

    private AdminWxUserVO toVO(WxUserEntity user) {
        AdminWxUserVO vo = new AdminWxUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setStatus(user.getStatus());
        vo.setRegisterSource(user.getRegisterSource());
        vo.setPowerPoints(user.getPowerPoints() == null ? 0 : user.getPowerPoints());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trim = value.trim();
        return trim.isEmpty() ? null : trim;
    }
}

