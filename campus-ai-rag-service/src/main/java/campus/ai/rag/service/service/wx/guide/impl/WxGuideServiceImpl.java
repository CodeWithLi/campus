package campus.ai.rag.service.service.wx.guide.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.forum.ForumGuideEntity;
import campus.ai.rag.service.entity.forum.ForumGuideStepEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumGuideMapper;
import campus.ai.rag.service.mapper.forum.ForumGuideStepMapper;
import campus.ai.rag.service.service.wx.guide.WxGuideService;
import campus.ai.rag.service.vo.wx.WxGuideListItemVO;
import campus.ai.rag.service.vo.wx.WxGuideStepVO;
import campus.ai.rag.service.vo.wx.WxGuideVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WxGuideServiceImpl implements WxGuideService {

    private final ForumGuideMapper forumGuideMapper;
    private final ForumGuideStepMapper forumGuideStepMapper;

    public WxGuideServiceImpl(ForumGuideMapper forumGuideMapper, ForumGuideStepMapper forumGuideStepMapper) {
        this.forumGuideMapper = forumGuideMapper;
        this.forumGuideStepMapper = forumGuideStepMapper;
    }

    @Override
    public List<WxGuideListItemVO> listPublished(String keyword, String category) {
        LambdaQueryWrapper<ForumGuideEntity> qw = new LambdaQueryWrapper<ForumGuideEntity>()
                .eq(ForumGuideEntity::getDeleted, 0)
                .eq(ForumGuideEntity::getStatus, "PUBLISHED")
                .orderByAsc(ForumGuideEntity::getSortNo)
                .orderByDesc(ForumGuideEntity::getId);
        if (!isBlank(keyword)) {
            qw.and(w -> w.like(ForumGuideEntity::getTitle, keyword.trim())
                    .or()
                    .like(ForumGuideEntity::getSummary, keyword.trim()));
        }
        if (!isBlank(category)) {
            qw.eq(ForumGuideEntity::getCategory, category.trim());
        }
        return forumGuideMapper.selectList(qw).stream().map(this::toListItem).toList();
    }

    @Override
    public List<String> listPublishedCategories() {
        return forumGuideMapper.selectList(new LambdaQueryWrapper<ForumGuideEntity>()
                        .select(ForumGuideEntity::getCategory)
                        .eq(ForumGuideEntity::getDeleted, 0)
                        .eq(ForumGuideEntity::getStatus, "PUBLISHED")
                        .isNotNull(ForumGuideEntity::getCategory)
                        .ne(ForumGuideEntity::getCategory, "")
                        .orderByAsc(ForumGuideEntity::getCategory))
                .stream()
                .map(ForumGuideEntity::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public WxGuideVO detail(Long guideId) {
        if (guideId == null || guideId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "指南ID不能为空");
        }
        ForumGuideEntity entity = forumGuideMapper.selectOne(new LambdaQueryWrapper<ForumGuideEntity>()
                .eq(ForumGuideEntity::getId, guideId)
                .eq(ForumGuideEntity::getDeleted, 0)
                .eq(ForumGuideEntity::getStatus, "PUBLISHED")
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "办事指南不存在或未上架");
        }
        WxGuideVO vo = new WxGuideVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setCategory(entity.getCategory());
        vo.setTag(entity.getTag());
        vo.setSummary(entity.getSummary());
        vo.setCoverUrl(entity.getCoverUrl());

        List<WxGuideStepVO> steps = forumGuideStepMapper.selectList(new LambdaQueryWrapper<ForumGuideStepEntity>()
                        .eq(ForumGuideStepEntity::getGuideId, guideId)
                        .eq(ForumGuideStepEntity::getStatus, 1)
                        .orderByAsc(ForumGuideStepEntity::getStepNo)
                        .orderByAsc(ForumGuideStepEntity::getId))
                .stream()
                .filter(Objects::nonNull)
                .map(this::toStepVO)
                .toList();
        vo.setSteps(steps);
        return vo;
    }

    private WxGuideListItemVO toListItem(ForumGuideEntity entity) {
        WxGuideListItemVO vo = new WxGuideListItemVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setCategory(entity.getCategory());
        vo.setTag(entity.getTag());
        vo.setSummary(entity.getSummary());
        vo.setCoverUrl(entity.getCoverUrl());
        return vo;
    }

    private WxGuideStepVO toStepVO(ForumGuideStepEntity entity) {
        WxGuideStepVO vo = new WxGuideStepVO();
        vo.setId(entity.getId());
        vo.setStepNo(entity.getStepNo());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setActionType(entity.getActionType());
        vo.setActionPayload(entity.getActionPayload());
        vo.setRequiredMaterials(entity.getRequiredMaterials());
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
