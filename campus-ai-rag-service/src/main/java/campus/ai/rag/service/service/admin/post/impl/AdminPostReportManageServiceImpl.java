package campus.ai.rag.service.service.admin.post.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.admin.post.AdminPostReportHandleDTO;
import campus.ai.rag.service.entity.forum.ForumPostCommentEntity;
import campus.ai.rag.service.entity.forum.ForumPostEntity;
import campus.ai.rag.service.entity.forum.ForumPostFavoriteEntity;
import campus.ai.rag.service.entity.forum.ForumPostInteractionNoticeEntity;
import campus.ai.rag.service.entity.forum.ForumPostLikeEntity;
import campus.ai.rag.service.entity.forum.ForumPostReportEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumPostCommentMapper;
import campus.ai.rag.service.mapper.forum.ForumPostFavoriteMapper;
import campus.ai.rag.service.mapper.forum.ForumPostInteractionNoticeMapper;
import campus.ai.rag.service.mapper.forum.ForumPostLikeMapper;
import campus.ai.rag.service.mapper.forum.ForumPostMapper;
import campus.ai.rag.service.mapper.forum.ForumPostReportMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.admin.post.AdminPostReportManageService;
import campus.ai.rag.service.vo.admin.post.AdminPostReportPageVO;
import campus.ai.rag.service.vo.admin.post.AdminPostReportVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminPostReportManageServiceImpl implements AdminPostReportManageService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "RESOLVED_VALID", "RESOLVED_INVALID");

    private final ForumPostReportMapper forumPostReportMapper;
    private final ForumPostMapper forumPostMapper;
    private final ForumPostLikeMapper forumPostLikeMapper;
    private final ForumPostCommentMapper forumPostCommentMapper;
    private final ForumPostFavoriteMapper forumPostFavoriteMapper;
    private final ForumPostInteractionNoticeMapper forumPostInteractionNoticeMapper;
    private final WxUserMapper wxUserMapper;

    public AdminPostReportManageServiceImpl(ForumPostReportMapper forumPostReportMapper,
                                            ForumPostMapper forumPostMapper,
                                            ForumPostLikeMapper forumPostLikeMapper,
                                            ForumPostCommentMapper forumPostCommentMapper,
                                            ForumPostFavoriteMapper forumPostFavoriteMapper,
                                            ForumPostInteractionNoticeMapper forumPostInteractionNoticeMapper,
                                            WxUserMapper wxUserMapper) {
        this.forumPostReportMapper = forumPostReportMapper;
        this.forumPostMapper = forumPostMapper;
        this.forumPostLikeMapper = forumPostLikeMapper;
        this.forumPostCommentMapper = forumPostCommentMapper;
        this.forumPostFavoriteMapper = forumPostFavoriteMapper;
        this.forumPostInteractionNoticeMapper = forumPostInteractionNoticeMapper;
        this.wxUserMapper = wxUserMapper;
    }

    @Override
    public AdminPostReportPageVO listPage(Long pageNum, Long pageSize, String status, Long postId, Long reporterUserId, String startTime, String endTime) {
        long pNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long pSize = pageSize == null || pageSize < 1 ? 20L : Math.min(pageSize, 200L);
        LambdaQueryWrapper<ForumPostReportEntity> qw = new LambdaQueryWrapper<ForumPostReportEntity>()
                .orderByDesc(ForumPostReportEntity::getId);
        if (!isBlank(status)) {
            qw.eq(ForumPostReportEntity::getStatus, status.trim().toUpperCase());
        }
        if (postId != null) {
            qw.eq(ForumPostReportEntity::getPostId, postId);
        }
        if (reporterUserId != null) {
            qw.eq(ForumPostReportEntity::getReporterUserId, reporterUserId);
        }
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        if (st != null && et != null) {
            qw.between(ForumPostReportEntity::getCreatedAt, st, et);
        } else if (st != null) {
            qw.ge(ForumPostReportEntity::getCreatedAt, st);
        } else if (et != null) {
            qw.le(ForumPostReportEntity::getCreatedAt, et);
        }
        Page<ForumPostReportEntity> page = forumPostReportMapper.selectPage(new Page<>(pNum, pSize), qw);
        List<ForumPostReportEntity> records = page.getRecords();
        Map<Long, ForumPostEntity> postMap = loadPostMap(records);
        Map<Long, WxUserEntity> userMap = loadUserMap(records, postMap);

        AdminPostReportPageVO vo = new AdminPostReportPageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(records.stream().map(r -> toVO(r, postMap.get(r.getPostId()), userMap)).toList());
        return vo;
    }

    @Override
    public AdminPostReportVO detail(Long reportId) {
        ForumPostReportEntity report = getRequiredReport(reportId);
        Map<Long, ForumPostEntity> postMap = loadPostMap(List.of(report));
        Map<Long, WxUserEntity> userMap = loadUserMap(List.of(report), postMap);
        return toVO(report, postMap.get(report.getPostId()), userMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminPostReportVO handle(Long reportId, AdminPostReportHandleDTO dto) {
        ForumPostReportEntity report = getRequiredReport(reportId);
        if (!Objects.equals(report.getStatus(), "PENDING")) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "该举报已处理");
        }
        String status = normalizeStatus(dto == null ? null : dto.getStatus());
        report.setStatus(status);
        report.setHandleRemark(trimToNull(dto == null ? null : dto.getHandleRemark()));
        report.setHandledBy(currentAdminName());
        report.setHandledAt(LocalDateTime.now());
        forumPostReportMapper.updateById(report);

        if (Objects.equals(status, "RESOLVED_VALID") && Boolean.TRUE.equals(dto.getDeletePost())) {
            deletePostCascade(report.getPostId());
        }
        return detail(reportId);
    }

    private void deletePostCascade(Long postId) {
        if (postId == null) {
            return;
        }
        forumPostMapper.deleteById(postId);
        forumPostLikeMapper.delete(new LambdaQueryWrapper<ForumPostLikeEntity>().eq(ForumPostLikeEntity::getPostId, postId));
        forumPostCommentMapper.delete(new LambdaQueryWrapper<ForumPostCommentEntity>().eq(ForumPostCommentEntity::getPostId, postId));
        forumPostFavoriteMapper.delete(new LambdaQueryWrapper<ForumPostFavoriteEntity>().eq(ForumPostFavoriteEntity::getPostId, postId));
        forumPostInteractionNoticeMapper.delete(new LambdaQueryWrapper<ForumPostInteractionNoticeEntity>().eq(ForumPostInteractionNoticeEntity::getPostId, postId));
    }

    private ForumPostReportEntity getRequiredReport(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "举报ID不能为空");
        }
        ForumPostReportEntity report = forumPostReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "举报记录不存在");
        }
        return report;
    }

    private Map<Long, ForumPostEntity> loadPostMap(List<ForumPostReportEntity> reports) {
        Set<Long> postIds = reports.stream().map(ForumPostReportEntity::getPostId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (postIds.isEmpty()) return Map.of();
        return forumPostMapper.selectBatchIds(postIds).stream().filter(Objects::nonNull).collect(Collectors.toMap(ForumPostEntity::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, WxUserEntity> loadUserMap(List<ForumPostReportEntity> reports, Map<Long, ForumPostEntity> postMap) {
        Set<Long> userIds = reports.stream().map(ForumPostReportEntity::getReporterUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        userIds.addAll(postMap.values().stream().map(ForumPostEntity::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        if (userIds.isEmpty()) return Map.of();
        return wxUserMapper.selectBatchIds(userIds).stream().filter(Objects::nonNull).collect(Collectors.toMap(WxUserEntity::getId, u -> u, (a, b) -> a));
    }

    private AdminPostReportVO toVO(ForumPostReportEntity report, ForumPostEntity post, Map<Long, WxUserEntity> userMap) {
        WxUserEntity reporter = userMap.get(report.getReporterUserId());
        WxUserEntity postUser = post == null ? null : userMap.get(post.getUserId());
        AdminPostReportVO vo = new AdminPostReportVO();
        vo.setId(report.getId());
        vo.setPostId(report.getPostId());
        vo.setPostTitle(post == null ? "[帖子已删除]" : post.getTitle());
        vo.setPostUserId(post == null ? null : post.getUserId());
        vo.setPostUserNickname(postUser == null ? null : postUser.getNickname());
        vo.setReporterUserId(report.getReporterUserId());
        vo.setReporterNickname(reporter == null ? null : reporter.getNickname());
        vo.setReason(report.getReason());
        vo.setStatus(report.getStatus());
        vo.setHandleRemark(report.getHandleRemark());
        vo.setHandledBy(report.getHandledBy());
        vo.setHandledAt(report.getHandledAt());
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }

    private String normalizeStatus(String status) {
        String s = isBlank(status) ? "" : status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(s) || Objects.equals(s, "PENDING")) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "处理状态必须是 RESOLVED_VALID 或 RESOLVED_INVALID");
        }
        return s;
    }

    private String currentAdminName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "admin";
        }
        return authentication.getName();
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) return null;
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException ignore) {
            return null;
        }
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
