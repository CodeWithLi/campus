package campus.ai.rag.service.service.admin.guide.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.guide.AdminGuideSaveDTO;
import campus.ai.rag.service.dto.admin.guide.AdminGuideStepSaveDTO;
import campus.ai.rag.service.entity.forum.ForumGuideEntity;
import campus.ai.rag.service.entity.forum.ForumGuideStepEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumGuideMapper;
import campus.ai.rag.service.mapper.forum.ForumGuideStepMapper;
import campus.ai.rag.service.service.admin.guide.AdminGuideManageService;
import campus.ai.rag.service.vo.admin.guide.AdminGuidePageVO;
import campus.ai.rag.service.vo.admin.guide.AdminGuideStepVO;
import campus.ai.rag.service.vo.admin.guide.AdminGuideVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminGuideManageServiceImpl implements AdminGuideManageService {

    private static final Set<String> ALLOWED_STATUS = Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    private static final Set<String> ALLOWED_ACTION_TYPE = Set.of("NONE", "LINK", "REMINDER", "ASK_AI");

    private final ForumGuideMapper forumGuideMapper;
    private final ForumGuideStepMapper forumGuideStepMapper;

    public AdminGuideManageServiceImpl(ForumGuideMapper forumGuideMapper, ForumGuideStepMapper forumGuideStepMapper) {
        this.forumGuideMapper = forumGuideMapper;
        this.forumGuideStepMapper = forumGuideStepMapper;
    }

    @Override
    public AdminGuidePageVO listPage(Long pageNum, Long pageSize, String keyword, String status, String category) {
        long pNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long pSize = pageSize == null || pageSize < 1 ? 20L : Math.min(pageSize, 200L);
        LambdaQueryWrapper<ForumGuideEntity> qw = new LambdaQueryWrapper<ForumGuideEntity>()
                .eq(ForumGuideEntity::getDeleted, 0)
                .orderByAsc(ForumGuideEntity::getSortNo)
                .orderByDesc(ForumGuideEntity::getId);
        if (!isBlank(keyword)) {
            qw.and(w -> w.like(ForumGuideEntity::getTitle, keyword.trim())
                    .or()
                    .like(ForumGuideEntity::getSummary, keyword.trim()));
        }
        if (!isBlank(status)) {
            qw.eq(ForumGuideEntity::getStatus, normalizeStatus(status, false));
        }
        if (!isBlank(category)) {
            qw.eq(ForumGuideEntity::getCategory, category.trim());
        }
        Page<ForumGuideEntity> page = forumGuideMapper.selectPage(new Page<>(pNum, pSize), qw);

        AdminGuidePageVO vo = new AdminGuidePageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(page.getRecords().stream().map(this::toVOWithoutSteps).toList());
        return vo;
    }

    @Override
    public AdminGuideVO detail(Long guideId) {
        ForumGuideEntity guide = getRequiredGuide(guideId);
        AdminGuideVO vo = toVOWithoutSteps(guide);
        vo.setSteps(loadStepVOs(guide.getId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminGuideVO create(AdminGuideSaveDTO dto) {
        validateSaveDTO(dto);
        ForumGuideEntity entity = new ForumGuideEntity();
        entity.setTitle(dto.getTitle().trim());
        entity.setCategory(trimToNull(dto.getCategory()));
        entity.setTag(trimToNull(dto.getTag()));
        entity.setSummary(trimToNull(dto.getSummary()));
        entity.setCoverUrl(trimToNull(dto.getCoverUrl()));
        entity.setStatus("DRAFT");
        entity.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        entity.setDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        forumGuideMapper.insert(entity);
        replaceSteps(entity.getId(), dto.getSteps());
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminGuideVO update(Long guideId, AdminGuideSaveDTO dto) {
        validateSaveDTO(dto);
        ForumGuideEntity entity = getRequiredGuide(guideId);
        entity.setTitle(dto.getTitle().trim());
        entity.setCategory(trimToNull(dto.getCategory()));
        entity.setTag(trimToNull(dto.getTag()));
        entity.setSummary(trimToNull(dto.getSummary()));
        entity.setCoverUrl(trimToNull(dto.getCoverUrl()));
        entity.setSortNo(dto.getSortNo() == null ? 100 : dto.getSortNo());
        entity.setUpdatedAt(LocalDateTime.now());
        forumGuideMapper.updateById(entity);
        replaceSteps(guideId, dto.getSteps());
        return detail(guideId);
    }

    @Override
    public void publish(Long guideId) {
        switchStatus(guideId, "PUBLISHED");
    }

    @Override
    public void offline(Long guideId) {
        switchStatus(guideId, "OFFLINE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long guideId) {
        ForumGuideEntity entity = getRequiredGuide(guideId);
        forumGuideMapper.update(null, new LambdaUpdateWrapper<ForumGuideEntity>()
                .eq(ForumGuideEntity::getId, entity.getId())
                .set(ForumGuideEntity::getDeleted, 1)
                .set(ForumGuideEntity::getUpdatedAt, LocalDateTime.now()));
        forumGuideStepMapper.delete(new LambdaQueryWrapper<ForumGuideStepEntity>()
                .eq(ForumGuideStepEntity::getGuideId, entity.getId()));
    }

    @Override
    public String uploadCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "请选择封面图文件");
        }
        String contentType = String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持图片文件");
        }
        String ext = resolveImageExt(file.getOriginalFilename(), contentType);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get("upload", "guide").toAbsolutePath().normalize();
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.INTERNAL_SERVER_ERROR, "封面图上传失败");
        }
        return "/campus/upload/guide/" + fileName;
    }

    private void switchStatus(Long guideId, String status) {
        ForumGuideEntity entity = getRequiredGuide(guideId);
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        forumGuideMapper.updateById(entity);
    }

    private ForumGuideEntity getRequiredGuide(Long guideId) {
        if (guideId == null || guideId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "指南ID不能为空");
        }
        ForumGuideEntity entity = forumGuideMapper.selectById(guideId);
        if (entity == null || Objects.equals(entity.getDeleted(), 1)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "办事指南不存在");
        }
        return entity;
    }

    private void validateSaveDTO(AdminGuideSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "参数不能为空");
        }
        if (isBlank(dto.getTitle())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "指南标题不能为空");
        }
        List<AdminGuideStepSaveDTO> steps = dto.getSteps();
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "请至少配置一个步骤");
        }
        for (int i = 0; i < steps.size(); i++) {
            AdminGuideStepSaveDTO step = steps.get(i);
            if (step == null || isBlank(step.getTitle())) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "步骤标题不能为空");
            }
            normalizeActionType(step.getActionType());
        }
    }

    private void replaceSteps(Long guideId, List<AdminGuideStepSaveDTO> steps) {
        forumGuideStepMapper.delete(new LambdaQueryWrapper<ForumGuideStepEntity>()
                .eq(ForumGuideStepEntity::getGuideId, guideId));
        List<ForumGuideStepEntity> entities = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            AdminGuideStepSaveDTO step = steps.get(i);
            ForumGuideStepEntity entity = new ForumGuideStepEntity();
            entity.setGuideId(guideId);
            entity.setStepNo(step.getStepNo() == null ? (i + 1) : step.getStepNo());
            entity.setTitle(step.getTitle().trim());
            entity.setContent(trimToNull(step.getContent()));
            entity.setActionType(normalizeActionType(step.getActionType()));
            entity.setActionPayload(trimToNull(step.getActionPayload()));
            entity.setRequiredMaterials(trimToNull(step.getRequiredMaterials()));
            entity.setStatus(step.getStatus() == null ? 1 : (step.getStatus() == 1 ? 1 : 0));
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entities.add(entity);
        }
        for (ForumGuideStepEntity entity : entities) {
            forumGuideStepMapper.insert(entity);
        }
    }

    private List<AdminGuideStepVO> loadStepVOs(Long guideId) {
        List<ForumGuideStepEntity> steps = forumGuideStepMapper.selectList(new LambdaQueryWrapper<ForumGuideStepEntity>()
                .eq(ForumGuideStepEntity::getGuideId, guideId)
                .orderByAsc(ForumGuideStepEntity::getStepNo)
                .orderByAsc(ForumGuideStepEntity::getId));
        return steps.stream().map(this::toStepVO).collect(Collectors.toList());
    }

    private AdminGuideVO toVOWithoutSteps(ForumGuideEntity entity) {
        AdminGuideVO vo = new AdminGuideVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setCategory(entity.getCategory());
        vo.setTag(entity.getTag());
        vo.setSummary(entity.getSummary());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setStatus(normalizeStatus(entity.getStatus(), true));
        vo.setSortNo(entity.getSortNo());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AdminGuideStepVO toStepVO(ForumGuideStepEntity entity) {
        AdminGuideStepVO vo = new AdminGuideStepVO();
        vo.setId(entity.getId());
        vo.setStepNo(entity.getStepNo());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setActionType(entity.getActionType());
        vo.setActionPayload(entity.getActionPayload());
        vo.setRequiredMaterials(entity.getRequiredMaterials());
        vo.setStatus(entity.getStatus());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String normalizeStatus(String value, boolean fallbackDraft) {
        if (isBlank(value)) {
            if (fallbackDraft) {
                return "DRAFT";
            }
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "指南状态不能为空");
        }
        String status = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "指南状态非法");
        }
        return status;
    }

    private String normalizeActionType(String value) {
        if (isBlank(value)) {
            return "NONE";
        }
        String actionType = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ACTION_TYPE.contains(actionType)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "步骤动作类型非法");
        }
        return actionType;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trim = value.trim();
        return trim.isEmpty() ? null : trim;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveImageExt(String originalFileName, String contentType) {
        String ext = "";
        if (!isBlank(originalFileName) && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (Set.of("png", "jpg", "jpeg", "gif", "webp").contains(ext)) {
            return "jpg".equals(ext) ? "jpeg" : ext;
        }
        if (contentType.contains("png")) return "png";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("webp")) return "webp";
        return "jpeg";
    }
}
