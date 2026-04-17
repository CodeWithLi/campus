package campus.ai.rag.service.service.admin.prompt.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.prompt.AiSystemPromptCreateDTO;
import campus.ai.rag.service.dto.admin.prompt.AiSystemPromptUpdateDTO;
import campus.ai.rag.service.entity.prompt.AiSystemPromptEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.prompt.AiSystemPromptMapper;
import campus.ai.rag.service.service.admin.prompt.AdminSystemPromptService;
import campus.ai.rag.service.vo.admin.prompt.AiSystemPromptVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminSystemPromptServiceImpl implements AdminSystemPromptService {

    private static final Set<String> SUPPORTED_SCENES = Set.of("CHAT", "RAG", "RESUME", "BOTH");

    private final AiSystemPromptMapper aiSystemPromptMapper;

    public AdminSystemPromptServiceImpl(AiSystemPromptMapper aiSystemPromptMapper) {
        this.aiSystemPromptMapper = aiSystemPromptMapper;
    }

    @Override
    public AiSystemPromptVO create(AiSystemPromptCreateDTO dto) {
        validateCreateDto(dto);

        AiSystemPromptEntity exists = aiSystemPromptMapper.selectOne(new LambdaQueryWrapper<AiSystemPromptEntity>()
                .eq(AiSystemPromptEntity::getPromptCode, dto.getPromptCode().trim())
                .last("limit 1"));
        if (Objects.nonNull(exists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "提示词编码已存在");
        }

        AiSystemPromptEntity entity = new AiSystemPromptEntity();
        entity.setPromptCode(dto.getPromptCode().trim());
        entity.setPromptName(dto.getPromptName().trim());
        entity.setScene(dto.getScene().trim().toUpperCase());
        entity.setContent(dto.getContent().trim());
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        entity.setPriority(dto.getPriority() == null ? 100 : dto.getPriority());
        entity.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
        entity.setRemark(trimOrNull(dto.getRemark()));
        entity.setVersion(1);

        validateDefaultAndEnabled(entity.getEnabled(), entity.getIsDefault());

        aiSystemPromptMapper.insert(entity);
        if (Objects.equals(entity.getIsDefault(), 1)) {
            unsetOtherDefaults(entity.getScene(), entity.getId());
        }
        return toVO(aiSystemPromptMapper.selectById(entity.getId()));
    }

    @Override
    public AiSystemPromptVO update(Long id, AiSystemPromptUpdateDTO dto) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提示词ID不能为空");
        }
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "更新参数不能为空");
        }

        AiSystemPromptEntity entity = aiSystemPromptMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "系统提示词不存在");
        }

        if (dto.getScene() != null) {
            String scene = trimOrNull(dto.getScene());
            if (scene != null) {
                validateScene(scene);
                entity.setScene(scene.toUpperCase());
            }
        }
        if (dto.getPromptName() != null && !isBlank(dto.getPromptName())) {
            entity.setPromptName(dto.getPromptName().trim());
        }
        if (dto.getContent() != null && !isBlank(dto.getContent())) {
            entity.setContent(dto.getContent().trim());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        if (dto.getPriority() != null) {
            entity.setPriority(dto.getPriority());
        }
        if (dto.getIsDefault() != null) {
            entity.setIsDefault(dto.getIsDefault());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(trimOrNull(dto.getRemark()));
        }

        validateDefaultAndEnabled(entity.getEnabled(), entity.getIsDefault());
        entity.setVersion(entity.getVersion() == null ? 1 : (entity.getVersion() + 1));

        aiSystemPromptMapper.updateById(entity);
        if (Objects.equals(entity.getIsDefault(), 1)) {
            unsetOtherDefaults(entity.getScene(), entity.getId());
        }
        return toVO(aiSystemPromptMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提示词ID不能为空");
        }
        AiSystemPromptEntity entity = aiSystemPromptMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "系统提示词不存在");
        }
        aiSystemPromptMapper.deleteById(id);
    }

    @Override
    public AiSystemPromptVO getById(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提示词ID不能为空");
        }
        AiSystemPromptEntity entity = aiSystemPromptMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "系统提示词不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<AiSystemPromptVO> listAll() {
        List<AiSystemPromptEntity> entities = aiSystemPromptMapper.selectList(new LambdaQueryWrapper<AiSystemPromptEntity>()
                .orderByDesc(AiSystemPromptEntity::getEnabled)
                .orderByAsc(AiSystemPromptEntity::getPriority)
                .orderByDesc(AiSystemPromptEntity::getId));
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    private void validateCreateDto(AiSystemPromptCreateDTO dto) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "创建参数不能为空");
        }
        if (isBlank(dto.getPromptCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提示词编码不能为空");
        }
        if (isBlank(dto.getPromptName())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提示词名称不能为空");
        }
        if (isBlank(dto.getScene())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "适用场景不能为空");
        }
        validateScene(dto.getScene());
        if (isBlank(dto.getContent())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提示词内容不能为空");
        }
        Integer enabled = dto.getEnabled() == null ? 1 : dto.getEnabled();
        Integer isDefault = dto.getIsDefault() == null ? 0 : dto.getIsDefault();
        validateDefaultAndEnabled(enabled, isDefault);
    }

    private void validateScene(String scene) {
        String normalized = scene.trim().toUpperCase();
        if (!SUPPORTED_SCENES.contains(normalized)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "scene 仅支持 CHAT/RAG/RESUME/BOTH");
        }
    }

    private void validateDefaultAndEnabled(Integer enabled, Integer isDefault) {
        if (enabled != null && isDefault != null && Objects.equals(enabled, 0) && Objects.equals(isDefault, 1)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "禁用状态的提示词不能设置为默认");
        }
    }

    private void unsetOtherDefaults(String scene, Long selfId) {
        aiSystemPromptMapper.update(null, new LambdaUpdateWrapper<AiSystemPromptEntity>()
                .set(AiSystemPromptEntity::getIsDefault, 0)
                .eq(AiSystemPromptEntity::getScene, scene)
                .eq(AiSystemPromptEntity::getIsDefault, 1)
                .ne(AiSystemPromptEntity::getId, selfId));
    }

    private AiSystemPromptVO toVO(AiSystemPromptEntity entity) {
        AiSystemPromptVO vo = new AiSystemPromptVO();
        vo.setId(entity.getId());
        vo.setPromptCode(entity.getPromptCode());
        vo.setPromptName(entity.getPromptName());
        vo.setScene(entity.getScene());
        vo.setContent(entity.getContent());
        vo.setEnabled(entity.getEnabled());
        vo.setPriority(entity.getPriority());
        vo.setIsDefault(entity.getIsDefault());
        vo.setVersion(entity.getVersion());
        vo.setRemark(entity.getRemark());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trim = value.trim();
        return trim.isEmpty() ? null : trim;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

