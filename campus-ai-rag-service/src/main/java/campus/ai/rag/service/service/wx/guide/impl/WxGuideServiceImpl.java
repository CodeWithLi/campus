package campus.ai.rag.service.service.wx.guide.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.forum.ForumGuideEntity;
import campus.ai.rag.service.entity.forum.ForumGuideFavoriteEntity;
import campus.ai.rag.service.entity.forum.ForumGuideProgressEntity;
import campus.ai.rag.service.entity.forum.ForumGuideStepEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumGuideMapper;
import campus.ai.rag.service.mapper.forum.ForumGuideFavoriteMapper;
import campus.ai.rag.service.mapper.forum.ForumGuideProgressMapper;
import campus.ai.rag.service.mapper.forum.ForumGuideStepMapper;
import campus.ai.rag.service.service.wx.guide.WxGuideService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxGuideListItemVO;
import campus.ai.rag.service.vo.wx.WxGuideStepVO;
import campus.ai.rag.service.vo.wx.WxGuideVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WxGuideServiceImpl implements WxGuideService {

    private final ForumGuideMapper forumGuideMapper;
    private final ForumGuideStepMapper forumGuideStepMapper;
    private final ForumGuideFavoriteMapper forumGuideFavoriteMapper;
    private final ForumGuideProgressMapper forumGuideProgressMapper;

    public WxGuideServiceImpl(ForumGuideMapper forumGuideMapper,
                              ForumGuideStepMapper forumGuideStepMapper,
                              ForumGuideFavoriteMapper forumGuideFavoriteMapper,
                              ForumGuideProgressMapper forumGuideProgressMapper) {
        this.forumGuideMapper = forumGuideMapper;
        this.forumGuideStepMapper = forumGuideStepMapper;
        this.forumGuideFavoriteMapper = forumGuideFavoriteMapper;
        this.forumGuideProgressMapper = forumGuideProgressMapper;
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

        // 浏览统计：每次打开详情 +1
        forumGuideMapper.update(null, new LambdaUpdateWrapper<ForumGuideEntity>()
                .eq(ForumGuideEntity::getId, guideId)
                .setSql("view_count = IFNULL(view_count,0) + 1"));

        WxGuideVO vo = new WxGuideVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setCategory(entity.getCategory());
        vo.setTag(entity.getTag());
        vo.setSummary(entity.getSummary());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setRagCategoryIdsJson(entity.getRagCategoryIdsJson());

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

        // 登录态增强：收藏与完成进度
        String wxUserId = WxAuthContextUtils.getOptionalWxUserId();
        Long userId = parseUserId(wxUserId);
        if (userId != null) {
            boolean favorite = forumGuideFavoriteMapper.selectCount(new LambdaQueryWrapper<ForumGuideFavoriteEntity>()
                    .eq(ForumGuideFavoriteEntity::getUserId, userId)
                    .eq(ForumGuideFavoriteEntity::getGuideId, guideId)) > 0;
            List<Long> stepIds = steps.stream().map(WxGuideStepVO::getId).filter(Objects::nonNull).toList();
            Set<Long> doneStepIds = stepIds.isEmpty()
                    ? Set.of()
                    : forumGuideProgressMapper.selectList(new LambdaQueryWrapper<ForumGuideProgressEntity>()
                            .select(ForumGuideProgressEntity::getStepId, ForumGuideProgressEntity::getDone)
                            .eq(ForumGuideProgressEntity::getUserId, userId)
                            .eq(ForumGuideProgressEntity::getGuideId, guideId)
                            .in(ForumGuideProgressEntity::getStepId, stepIds)
                            .eq(ForumGuideProgressEntity::getDone, 1))
                    .stream()
                    .map(ForumGuideProgressEntity::getStepId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            vo.setFavorite(favorite);
            vo.setDoneStepIds(doneStepIds.stream().toList());
            vo.setDoneCount(doneStepIds.size());
            vo.setTotalSteps(stepIds.size());
        }
        return vo;
    }

    @Override
    public Boolean toggleFavorite(Long guideId) {
        Long userId = requireUserId();
        ForumGuideEntity guide = getPublishedGuide(guideId);
        ForumGuideFavoriteEntity exists = forumGuideFavoriteMapper.selectOne(new LambdaQueryWrapper<ForumGuideFavoriteEntity>()
                .eq(ForumGuideFavoriteEntity::getUserId, userId)
                .eq(ForumGuideFavoriteEntity::getGuideId, guide.getId())
                .last("limit 1"));
        if (exists != null) {
            forumGuideFavoriteMapper.deleteById(exists.getId());
            return false;
        }
        ForumGuideFavoriteEntity entity = new ForumGuideFavoriteEntity();
        entity.setUserId(userId);
        entity.setGuideId(guide.getId());
        entity.setCreatedAt(java.time.LocalDateTime.now());
        forumGuideFavoriteMapper.insert(entity);
        return true;
    }

    @Override
    public void updateStepProgress(Long guideId, Long stepId, boolean done) {
        Long userId = requireUserId();
        ForumGuideEntity guide = getPublishedGuide(guideId);
        if (stepId == null || stepId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "步骤ID不能为空");
        }
        ForumGuideStepEntity step = forumGuideStepMapper.selectOne(new LambdaQueryWrapper<ForumGuideStepEntity>()
                .eq(ForumGuideStepEntity::getId, stepId)
                .eq(ForumGuideStepEntity::getGuideId, guide.getId())
                .last("limit 1"));
        if (step == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "步骤不存在");
        }
        ForumGuideProgressEntity exists = forumGuideProgressMapper.selectOne(new LambdaQueryWrapper<ForumGuideProgressEntity>()
                .eq(ForumGuideProgressEntity::getUserId, userId)
                .eq(ForumGuideProgressEntity::getStepId, stepId)
                .last("limit 1"));
        if (exists == null) {
            ForumGuideProgressEntity entity = new ForumGuideProgressEntity();
            entity.setUserId(userId);
            entity.setGuideId(guide.getId());
            entity.setStepId(stepId);
            entity.setDone(done ? 1 : 0);
            entity.setCreatedAt(java.time.LocalDateTime.now());
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            forumGuideProgressMapper.insert(entity);
            return;
        }
        exists.setDone(done ? 1 : 0);
        exists.setGuideId(guide.getId());
        exists.setUpdatedAt(java.time.LocalDateTime.now());
        forumGuideProgressMapper.updateById(exists);
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

    private Long requireUserId() {
        String wxUserId = WxAuthContextUtils.requireWxUserId();
        Long userId = parseUserId(wxUserId);
        if (userId == null) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
        return userId;
    }

    private Long parseUserId(String wxUserId) {
        if (wxUserId == null || wxUserId.trim().isEmpty()) return null;
        try {
            return Long.parseLong(wxUserId.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private ForumGuideEntity getPublishedGuide(Long guideId) {
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
        return entity;
    }
}
