package campus.ai.rag.service.service.admin.post.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.forum.ForumPostCommentEntity;
import campus.ai.rag.service.entity.forum.ForumPostEntity;
import campus.ai.rag.service.entity.forum.ForumPostFavoriteEntity;
import campus.ai.rag.service.entity.forum.ForumPostInteractionNoticeEntity;
import campus.ai.rag.service.entity.forum.ForumPostLikeEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumPostCommentMapper;
import campus.ai.rag.service.mapper.forum.ForumPostFavoriteMapper;
import campus.ai.rag.service.mapper.forum.ForumPostInteractionNoticeMapper;
import campus.ai.rag.service.mapper.forum.ForumPostLikeMapper;
import campus.ai.rag.service.mapper.forum.ForumPostMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.admin.post.AdminPostManageService;
import campus.ai.rag.service.vo.admin.post.AdminPostPageVO;
import campus.ai.rag.service.vo.admin.post.AdminPostVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminPostManageServiceImpl implements AdminPostManageService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ForumPostMapper forumPostMapper;
    private final ForumPostLikeMapper forumPostLikeMapper;
    private final ForumPostCommentMapper forumPostCommentMapper;
    private final ForumPostFavoriteMapper forumPostFavoriteMapper;
    private final ForumPostInteractionNoticeMapper forumPostInteractionNoticeMapper;
    private final WxUserMapper wxUserMapper;

    public AdminPostManageServiceImpl(ForumPostMapper forumPostMapper,
                                      ForumPostLikeMapper forumPostLikeMapper,
                                      ForumPostCommentMapper forumPostCommentMapper,
                                      ForumPostFavoriteMapper forumPostFavoriteMapper,
                                      ForumPostInteractionNoticeMapper forumPostInteractionNoticeMapper,
                                      WxUserMapper wxUserMapper) {
        this.forumPostMapper = forumPostMapper;
        this.forumPostLikeMapper = forumPostLikeMapper;
        this.forumPostCommentMapper = forumPostCommentMapper;
        this.forumPostFavoriteMapper = forumPostFavoriteMapper;
        this.forumPostInteractionNoticeMapper = forumPostInteractionNoticeMapper;
        this.wxUserMapper = wxUserMapper;
    }

    @Override
    public AdminPostPageVO listPage(Long pageNum,
                                    Long pageSize,
                                    String keyword,
                                    Long userId,
                                    Integer isAnonymous,
                                    String startTime,
                                    String endTime) {
        long pNum = (pageNum == null || pageNum < 1) ? 1L : pageNum;
        long pSize = (pageSize == null || pageSize < 1) ? 20L : pageSize;
        pSize = Math.min(pSize, 200L);

        QueryWrapper<ForumPostEntity> qw = new QueryWrapper<>();
        if (!isBlank(keyword)) {
            String kw = keyword.trim();
            qw.and(w -> w.like("title", kw).or().like("content", kw));
        }
        if (userId != null) {
            qw.eq("user_id", userId);
        }
        if (isAnonymous != null) {
            qw.eq("is_anonymous", isAnonymous);
        }
        LocalDateTime st = parseDateTime(startTime);
        LocalDateTime et = parseDateTime(endTime);
        if (st != null && et != null) {
            qw.between("created_at", st, et);
        } else if (st != null) {
            qw.ge("created_at", st);
        } else if (et != null) {
            qw.le("created_at", et);
        }
        qw.orderByDesc("id");

        Page<ForumPostEntity> page = forumPostMapper.selectPage(new Page<>(pNum, pSize), qw);
        List<ForumPostEntity> records = page.getRecords();
        Map<Long, WxUserEntity> userMap = buildUserMap(records);
        Map<Long, Long> likeMap = countLikeByPostIds(records);
        Map<Long, Long> commentMap = countCommentByPostIds(records);

        List<AdminPostVO> list = records.stream()
                .map(post -> toVO(post, userMap.get(post.getUserId()), likeMap.get(post.getId()), commentMap.get(post.getId())))
                .toList();

        AdminPostPageVO vo = new AdminPostPageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(list);
        return vo;
    }

    @Override
    public AdminPostVO detail(Long postId) {
        ForumPostEntity post = getRequiredPost(postId);
        WxUserEntity user = post.getUserId() == null ? null : wxUserMapper.selectById(post.getUserId());
        long likeCount = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLikeEntity>()
                .eq(ForumPostLikeEntity::getPostId, post.getId()));
        long commentCount = forumPostCommentMapper.selectCount(new LambdaQueryWrapper<ForumPostCommentEntity>()
                .eq(ForumPostCommentEntity::getPostId, post.getId())
                .eq(ForumPostCommentEntity::getDeleted, 0));
        return toVO(post, user, likeCount, commentCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        ForumPostEntity post = getRequiredPost(postId);
        forumPostMapper.deleteById(post.getId());
        forumPostLikeMapper.delete(new LambdaQueryWrapper<ForumPostLikeEntity>()
                .eq(ForumPostLikeEntity::getPostId, post.getId()));
        forumPostCommentMapper.delete(new LambdaQueryWrapper<ForumPostCommentEntity>()
                .eq(ForumPostCommentEntity::getPostId, post.getId()));
        forumPostFavoriteMapper.delete(new LambdaQueryWrapper<ForumPostFavoriteEntity>()
                .eq(ForumPostFavoriteEntity::getPostId, post.getId()));
        forumPostInteractionNoticeMapper.delete(new LambdaQueryWrapper<ForumPostInteractionNoticeEntity>()
                .eq(ForumPostInteractionNoticeEntity::getPostId, post.getId()));
    }

    private ForumPostEntity getRequiredPost(Long postId) {
        if (postId == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "帖子ID不能为空");
        }
        ForumPostEntity post = forumPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private Map<Long, WxUserEntity> buildUserMap(List<ForumPostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = posts.stream()
                .map(ForumPostEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return wxUserMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(WxUserEntity::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Long> countLikeByPostIds(List<ForumPostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> postIds = posts.stream().map(ForumPostEntity::getId).collect(Collectors.toSet());
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<ForumPostLikeEntity> qw = new QueryWrapper<>();
        qw.select("post_id AS postId", "COUNT(1) AS cnt")
                .in("post_id", postIds)
                .groupBy("post_id");
        List<Map<String, Object>> rows = forumPostLikeMapper.selectMaps(qw);
        return rows.stream().collect(Collectors.toMap(
                row -> toLong(row.get("postId")),
                row -> toLong(row.get("cnt")),
                (a, b) -> a
        ));
    }

    private Map<Long, Long> countCommentByPostIds(List<ForumPostEntity> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> postIds = posts.stream().map(ForumPostEntity::getId).collect(Collectors.toSet());
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<ForumPostCommentEntity> qw = new QueryWrapper<>();
        qw.select("post_id AS postId", "COUNT(1) AS cnt")
                .in("post_id", postIds)
                .eq("deleted", 0)
                .groupBy("post_id");
        List<Map<String, Object>> rows = forumPostCommentMapper.selectMaps(qw);
        return rows.stream().collect(Collectors.toMap(
                row -> toLong(row.get("postId")),
                row -> toLong(row.get("cnt")),
                (a, b) -> a
        ));
    }

    private AdminPostVO toVO(ForumPostEntity post, WxUserEntity user, Long likeCount, Long commentCount) {
        AdminPostVO vo = new AdminPostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setImageUrls(parseImageUrls(post.getImageUrls()));
        vo.setIsAnonymous(post.getIsAnonymous());
        vo.setTextComplianceSkipped(post.getTextComplianceSkipped());
        vo.setLikeCount(likeCount == null ? 0L : likeCount);
        vo.setCommentCount(commentCount == null ? 0L : commentCount);
        vo.setCreatedAt(post.getCreatedAt());
        return vo;
    }

    private List<String> parseImageUrls(String imageUrls) {
        if (isBlank(imageUrls)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imageUrls, new TypeReference<>() {
            });
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return 0L;
        }
    }
}
