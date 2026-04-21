package campus.ai.rag.service.service.admin.model.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.model.AiModelConfigCreateDTO;
import campus.ai.rag.service.dto.admin.model.AiModelConfigUpdateDTO;
import campus.ai.rag.service.dto.admin.model.AiModelTestChatDTO;
import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.service.admin.model.AdminModelConfigService;
import campus.ai.rag.service.vo.admin.model.AiModelConfigVO;
import campus.ai.rag.service.vo.admin.model.AiModelTestChatVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminModelConfigServiceImpl implements AdminModelConfigService {

    private static final int INVOKE_TYPE_SPRING_AI = 1;
    private static final int INVOKE_TYPE_XF_HTTP = 2;
    private static final int INVOKE_TYPE_GENERIC_HTTP = 3;

    private final AiModelConfigMapper aiModelConfigMapper;
    private final AiChatRouter aiChatRouter;

    public AdminModelConfigServiceImpl(AiModelConfigMapper aiModelConfigMapper,
                                       AiChatRouter aiChatRouter) {
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.aiChatRouter = aiChatRouter;
    }

    @Override
    public AiModelConfigVO create(AiModelConfigCreateDTO dto) {
        validateCreateDto(dto);

        AiModelConfigEntity exists = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getModelCode, dto.getModelCode().trim())
                .last("limit 1"));
        if (Objects.nonNull(exists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "模型编码已存在");
        }

        AiModelConfigEntity entity = new AiModelConfigEntity();
        copyCreateFields(dto, entity);
        aiModelConfigMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public AiModelConfigVO update(Long id, AiModelConfigUpdateDTO dto) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模型ID不能为空");
        }
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "更新参数不能为空");
        }
        if (Objects.nonNull(dto.getInvokeType())) {
            validateInvokeType(dto.getInvokeType());
        }

        AiModelConfigEntity entity = aiModelConfigMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "模型配置不存在");
        }

        copyUpdateFields(dto, entity);
        aiModelConfigMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public void delete(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模型ID不能为空");
        }
        AiModelConfigEntity entity = aiModelConfigMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "模型配置不存在");
        }
        aiModelConfigMapper.deleteById(id);
    }

    @Override
    public AiModelConfigVO getById(Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模型ID不能为空");
        }
        AiModelConfigEntity entity = aiModelConfigMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "模型配置不存在");
        }
        return toVO(entity);
    }

    @Override
    public List<AiModelConfigVO> listAll() {
        List<AiModelConfigEntity> entities = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigEntity>()
                .orderByDesc(AiModelConfigEntity::getEnabled)
                .orderByAsc(AiModelConfigEntity::getSortNo)
                .orderByDesc(AiModelConfigEntity::getId));
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AiModelTestChatVO testChat(AiModelTestChatDTO dto) {
        if (dto == null || isBlank(dto.getModelCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模型编码不能为空");
        }
        String question = isBlank(dto.getMessage()) ? "你好" : dto.getMessage().trim();
        long start = System.currentTimeMillis();
        String answer = aiChatRouter.rawChat(dto.getModelCode().trim(), question);
        long elapsed = System.currentTimeMillis() - start;
        AiModelTestChatVO vo = new AiModelTestChatVO();
        vo.setModelCode(dto.getModelCode().trim());
        vo.setQuestion(question);
        vo.setAnswer(answer);
        vo.setElapsedMs(elapsed);
        return vo;
    }

    private void validateCreateDto(AiModelConfigCreateDTO dto) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "创建参数不能为空");
        }
        if (isBlank(dto.getModelCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模型编码不能为空");
        }
        if (isBlank(dto.getModelName())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "模型名称不能为空");
        }
        if (Objects.isNull(dto.getInvokeType())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "调用方式不能为空");
        }
        validateInvokeType(dto.getInvokeType());
    }

    private void validateInvokeType(Integer invokeType) {
        if (!Objects.equals(invokeType, INVOKE_TYPE_SPRING_AI)
                && !Objects.equals(invokeType, INVOKE_TYPE_XF_HTTP)
                && !Objects.equals(invokeType, INVOKE_TYPE_GENERIC_HTTP)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "调用方式仅支持 1(springai) / 2(xf_http) / 3(generic_http)");
        }
    }

    private void copyCreateFields(AiModelConfigCreateDTO dto, AiModelConfigEntity entity) {
        entity.setModelCode(dto.getModelCode().trim());
        entity.setModelName(dto.getModelName().trim());
        entity.setInvokeType(dto.getInvokeType());
        entity.setProvider(trimOrNull(dto.getProvider()));
        entity.setApiBaseUrl(trimOrNull(dto.getApiBaseUrl()));
        entity.setApiKey(trimOrNull(dto.getApiKey()));
        entity.setModelNameRemote(trimOrNull(dto.getModelNameRemote()));
        entity.setTemperature(dto.getTemperature());
        entity.setMaxTokens(dto.getMaxTokens());
        entity.setGenericReqType(trimOrNull(dto.getGenericReqType()));
        entity.setGenericReqField(trimOrNull(dto.getGenericReqField()));
        entity.setGenericRespPath(trimOrNull(dto.getGenericRespPath()));
        entity.setGenericHeadersJson(trimOrNull(dto.getGenericHeadersJson()));
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        entity.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        entity.setRemark(trimOrNull(dto.getRemark()));
    }

    private void copyUpdateFields(AiModelConfigUpdateDTO dto, AiModelConfigEntity entity) {
        if (!isBlank(dto.getModelName())) {
            entity.setModelName(dto.getModelName().trim());
        }
        if (Objects.nonNull(dto.getInvokeType())) {
            entity.setInvokeType(dto.getInvokeType());
        }
        if (Objects.nonNull(dto.getProvider())) {
            entity.setProvider(trimOrNull(dto.getProvider()));
        }
        if (Objects.nonNull(dto.getApiBaseUrl())) {
            entity.setApiBaseUrl(trimOrNull(dto.getApiBaseUrl()));
        }
        if (Objects.nonNull(dto.getApiKey())) {
            entity.setApiKey(trimOrNull(dto.getApiKey()));
        }
        if (Objects.nonNull(dto.getModelNameRemote())) {
            entity.setModelNameRemote(trimOrNull(dto.getModelNameRemote()));
        }
        if (Objects.nonNull(dto.getTemperature())) {
            entity.setTemperature(dto.getTemperature());
        }
        if (Objects.nonNull(dto.getMaxTokens())) {
            entity.setMaxTokens(dto.getMaxTokens());
        }
        if (Objects.nonNull(dto.getGenericReqType())) {
            entity.setGenericReqType(trimOrNull(dto.getGenericReqType()));
        }
        if (Objects.nonNull(dto.getGenericReqField())) {
            entity.setGenericReqField(trimOrNull(dto.getGenericReqField()));
        }
        if (Objects.nonNull(dto.getGenericRespPath())) {
            entity.setGenericRespPath(trimOrNull(dto.getGenericRespPath()));
        }
        if (Objects.nonNull(dto.getGenericHeadersJson())) {
            entity.setGenericHeadersJson(trimOrNull(dto.getGenericHeadersJson()));
        }
        if (Objects.nonNull(dto.getEnabled())) {
            entity.setEnabled(dto.getEnabled());
        }
        if (Objects.nonNull(dto.getSortNo())) {
            entity.setSortNo(dto.getSortNo());
        }
        if (Objects.nonNull(dto.getRemark())) {
            entity.setRemark(trimOrNull(dto.getRemark()));
        }
    }

    private AiModelConfigVO toVO(AiModelConfigEntity entity) {
        AiModelConfigVO vo = new AiModelConfigVO();
        vo.setId(entity.getId());
        vo.setModelCode(entity.getModelCode());
        vo.setModelName(entity.getModelName());
        vo.setInvokeType(entity.getInvokeType());
        vo.setProvider(entity.getProvider());
        vo.setApiBaseUrl(entity.getApiBaseUrl());
        vo.setApiKey(maskSecret(entity.getApiKey()));
        vo.setModelNameRemote(entity.getModelNameRemote());
        vo.setTemperature(entity.getTemperature());
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setGenericReqType(entity.getGenericReqType());
        vo.setGenericReqField(entity.getGenericReqField());
        vo.setGenericRespPath(entity.getGenericRespPath());
        vo.setGenericHeadersJson(entity.getGenericHeadersJson());
        vo.setEnabled(entity.getEnabled());
        vo.setSortNo(entity.getSortNo());
        vo.setRemark(entity.getRemark());
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

    private String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return secret;
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}

