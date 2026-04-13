package campus.ai.rag.service.service.wx.model.impl;

import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.service.wx.model.WxModelService;
import campus.ai.rag.service.vo.wx.WxModelOptionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WxModelServiceImpl implements WxModelService {

    private final AiModelConfigMapper aiModelConfigMapper;

    public WxModelServiceImpl(AiModelConfigMapper aiModelConfigMapper) {
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    @Override
    public List<WxModelOptionVO> listEnabledModels() {
        List<AiModelConfigEntity> entities = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .orderByAsc(AiModelConfigEntity::getSortNo)
                .orderByDesc(AiModelConfigEntity::getId));
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    private WxModelOptionVO toVO(AiModelConfigEntity entity) {
        WxModelOptionVO vo = new WxModelOptionVO();
        vo.setModelCode(entity.getModelCode());
        vo.setModelName(entity.getModelName());
        vo.setProvider(entity.getProvider());
        vo.setInvokeType(entity.getInvokeType());
        vo.setSortNo(entity.getSortNo());
        return vo;
    }
}

