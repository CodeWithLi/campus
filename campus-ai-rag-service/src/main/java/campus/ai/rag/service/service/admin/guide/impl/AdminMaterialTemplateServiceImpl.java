package campus.ai.rag.service.service.admin.guide.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.guide.AdminMaterialTemplateSaveDTO;
import campus.ai.rag.service.entity.forum.ForumMaterialTemplateEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumMaterialTemplateMapper;
import campus.ai.rag.service.service.admin.guide.AdminMaterialTemplateService;
import campus.ai.rag.service.vo.admin.guide.AdminMaterialTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AdminMaterialTemplateServiceImpl implements AdminMaterialTemplateService {

    private final ForumMaterialTemplateMapper forumMaterialTemplateMapper;

    public AdminMaterialTemplateServiceImpl(ForumMaterialTemplateMapper forumMaterialTemplateMapper) {
        this.forumMaterialTemplateMapper = forumMaterialTemplateMapper;
    }

    @Override
    public List<AdminMaterialTemplateVO> listAll() {
        List<ForumMaterialTemplateEntity> list = forumMaterialTemplateMapper.selectList(new LambdaQueryWrapper<ForumMaterialTemplateEntity>()
                .orderByDesc(ForumMaterialTemplateEntity::getEnabled)
                .orderByAsc(ForumMaterialTemplateEntity::getSortNo)
                .orderByDesc(ForumMaterialTemplateEntity::getId));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public AdminMaterialTemplateVO getById(Long id) {
        ForumMaterialTemplateEntity entity = getRequired(id);
        return toVO(entity);
    }

    @Override
    public AdminMaterialTemplateVO create(AdminMaterialTemplateSaveDTO dto) {
        validate(dto);
        ForumMaterialTemplateEntity exists = forumMaterialTemplateMapper.selectOne(new LambdaQueryWrapper<ForumMaterialTemplateEntity>()
                .eq(ForumMaterialTemplateEntity::getTemplateName, dto.getTemplateName().trim())
                .last("limit 1"));
        if (exists != null) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "模板名称已存在");
        }
        ForumMaterialTemplateEntity entity = new ForumMaterialTemplateEntity();
        entity.setTemplateName(dto.getTemplateName().trim());
        entity.setItemsJson(dto.getItemsJson().trim());
        entity.setEnabled(dto.getEnabled() == null ? 1 : (dto.getEnabled() == 1 ? 1 : 0));
        entity.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        entity.setRemark(trimToNull(dto.getRemark()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        forumMaterialTemplateMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public AdminMaterialTemplateVO update(Long id, AdminMaterialTemplateSaveDTO dto) {
        validate(dto);
        ForumMaterialTemplateEntity entity = getRequired(id);
        if (!Objects.equals(entity.getTemplateName(), dto.getTemplateName().trim())) {
            ForumMaterialTemplateEntity exists = forumMaterialTemplateMapper.selectOne(new LambdaQueryWrapper<ForumMaterialTemplateEntity>()
                    .eq(ForumMaterialTemplateEntity::getTemplateName, dto.getTemplateName().trim())
                    .last("limit 1"));
            if (exists != null) {
                throw new BusinessException(StatusCodeEnum.CONFLICT, "模板名称已存在");
            }
        }
        entity.setTemplateName(dto.getTemplateName().trim());
        entity.setItemsJson(dto.getItemsJson().trim());
        entity.setEnabled(dto.getEnabled() == null ? entity.getEnabled() : (dto.getEnabled() == 1 ? 1 : 0));
        entity.setSortNo(dto.getSortNo() == null ? entity.getSortNo() : dto.getSortNo());
        entity.setRemark(trimToNull(dto.getRemark()));
        entity.setUpdatedAt(LocalDateTime.now());
        forumMaterialTemplateMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public void delete(Long id) {
        ForumMaterialTemplateEntity entity = getRequired(id);
        forumMaterialTemplateMapper.deleteById(entity.getId());
    }

    private ForumMaterialTemplateEntity getRequired(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模板ID不能为空");
        }
        ForumMaterialTemplateEntity entity = forumMaterialTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "材料模板不存在");
        }
        return entity;
    }

    private void validate(AdminMaterialTemplateSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "参数不能为空");
        }
        if (isBlank(dto.getTemplateName())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模板名称不能为空");
        }
        if (isBlank(dto.getItemsJson())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "材料项不能为空");
        }
    }

    private AdminMaterialTemplateVO toVO(ForumMaterialTemplateEntity entity) {
        AdminMaterialTemplateVO vo = new AdminMaterialTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setItemsJson(entity.getItemsJson());
        vo.setEnabled(entity.getEnabled());
        vo.setSortNo(entity.getSortNo());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

