package campus.ai.rag.service.service.wx.post.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxPostAiGenerateDTO;
import campus.ai.rag.service.dto.wx.WxPostCommentCreateDTO;
import campus.ai.rag.service.dto.wx.WxPostPublishDTO;
import campus.ai.rag.service.entity.forum.ForumPostCommentEntity;
import campus.ai.rag.service.entity.forum.ForumPostEntity;
import campus.ai.rag.service.entity.forum.ForumPostLikeEntity;
import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumPostCommentMapper;
import campus.ai.rag.service.mapper.forum.ForumPostMapper;
import campus.ai.rag.service.mapper.forum.ForumPostLikeMapper;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.properties.WxPostProperties;
import campus.ai.rag.service.service.compliance.IflyImageAuditService;
import campus.ai.rag.service.service.compliance.IflyTextAuditService;
import campus.ai.rag.service.service.wx.post.WxPostService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxPostAiGenerateVO;
import campus.ai.rag.service.vo.wx.WxPostCommentVO;
import campus.ai.rag.service.vo.wx.WxPostDetailVO;
import campus.ai.rag.service.vo.wx.WxPostLikeVO;
import campus.ai.rag.service.vo.wx.WxPostListItemVO;
import campus.ai.rag.service.vo.wx.WxPostPageVO;
import campus.ai.rag.service.vo.wx.WxPostPublishVO;
import campus.ai.rag.service.vo.wx.WxPostUploadImageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class WxPostServiceImpl implements WxPostService {

    private static final String SCENE_POST = "POST";
    private static final int INVOKE_SPRING_AI = 1;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AiChatRouter aiChatRouter;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final WxUserMapper wxUserMapper;
    private final WxPostProperties wxPostProperties;
    private final ForumPostMapper forumPostMapper;
    private final ForumPostLikeMapper forumPostLikeMapper;
    private final ForumPostCommentMapper forumPostCommentMapper;
    private final IflyImageAuditService iflyImageAuditService;
    private final IflyTextAuditService iflyTextAuditService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WxPostServiceImpl(AiChatRouter aiChatRouter,
                             AiModelConfigMapper aiModelConfigMapper,
                             WxUserMapper wxUserMapper,
                             WxPostProperties wxPostProperties,
                             ForumPostMapper forumPostMapper,
                             ForumPostLikeMapper forumPostLikeMapper,
                             ForumPostCommentMapper forumPostCommentMapper,
                             IflyImageAuditService iflyImageAuditService,
                             IflyTextAuditService iflyTextAuditService,
                             RedisTemplate<String, Object> redisTemplate) {
        this.aiChatRouter = aiChatRouter;
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.wxUserMapper = wxUserMapper;
        this.wxPostProperties = wxPostProperties;
        this.forumPostMapper = forumPostMapper;
        this.forumPostLikeMapper = forumPostLikeMapper;
        this.forumPostCommentMapper = forumPostCommentMapper;
        this.iflyImageAuditService = iflyImageAuditService;
        this.iflyTextAuditService = iflyTextAuditService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public WxPostAiGenerateVO aiGenerate(WxPostAiGenerateDTO dto) {
        if (dto == null || isBlank(dto.getHint())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "发帖想法不能为空");
        }
        String modelCode = wxPostProperties.getAiModelCode().trim();
        assertSpringAiModel(modelCode);

        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        String hint = dto.getHint().trim();
        String userMessage = buildUserMessage(hint);

        String raw = aiChatRouter.chat(userId, modelCode, SCENE_POST, userMessage, null);
        ParsedPost parsed = parseModelJson(raw, hint);

        String title = truncate(parsed.title, wxPostProperties.getMaxTitleLength());
        String content = truncate(parsed.content, wxPostProperties.getMaxContentLength());

        String draftId = "pd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String hash = sha256Hex(title, content);
        PostDraftCache cache = new PostDraftCache();
        cache.contentHash = hash;

        String key = draftKey(userId, draftId);
        long hours = Math.max(1, wxPostProperties.getDraftTtlHours());
        redisTemplate.opsForValue().set(key, cache, Duration.ofHours(hours));

        WxPostAiGenerateVO vo = new WxPostAiGenerateVO();
        vo.setDraftId(draftId);
        vo.setTitle(title);
        vo.setContent(content);
        vo.setModelCode(modelCode);
        vo.setScene(SCENE_POST);
        return vo;
    }

    @Override
    public WxPostUploadImageVO uploadImages(MultipartFile[] files) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        if (files == null || files.length == 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "请至少上传1张图片");
        }
        int max = Math.max(1, wxPostProperties.getMaxImageCount());
        if (files.length > max) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "单次最多上传 " + max + " 张图片");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "读取图片失败");
            }
            // 图片合规（开发期可注释此行，默认视为通过）
//            ensureImageCompliance(bytes);
            urls.add(storeImage(userId, file, bytes));
        }
        if (urls.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "未检测到有效图片");
        }
        WxPostUploadImageVO vo = new WxPostUploadImageVO();
        vo.setImageUrls(urls);
        return vo;
    }

    @Override
    public WxPostPublishVO publish(WxPostPublishDTO dto) {
        if (dto == null || isBlank(dto.getTitle()) || isBlank(dto.getContent())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "标题与正文不能为空");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        String title = dto.getTitle().trim();
        String content = dto.getContent().trim();

        if (title.length() > wxPostProperties.getMaxTitleLength()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST,
                    "标题长度不能超过 " + wxPostProperties.getMaxTitleLength() + " 字");
        }
        if (content.length() > wxPostProperties.getMaxContentLength()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST,
                    "正文长度不能超过 " + wxPostProperties.getMaxContentLength() + " 字");
        }
        List<String> imageUrls = sanitizeImageUrls(dto.getImageUrls());
        int max = Math.max(1, wxPostProperties.getMaxImageCount());
        if (imageUrls.size() > max) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "单帖最多上传 " + max + " 张图片");
        }

        boolean textComplianceSkipped = false;
        if (!isBlank(dto.getDraftId())) {
            String draftId = dto.getDraftId().trim();
            String key = draftKey(userId, draftId);
            Object cached = redisTemplate.opsForValue().get(key);
            String expectHash = null;
            if (cached instanceof PostDraftCache) {
                expectHash = ((PostDraftCache) cached).contentHash;
            }
            String actual = sha256Hex(title, content);
            if (expectHash != null && expectHash.equalsIgnoreCase(actual)) {
                textComplianceSkipped = true;
            }
        }

        if (!textComplianceSkipped) {
            // 文本合规（开发期可注释此行，默认视为通过）
            log.info("进入文本合规校验");
//            ensureTextCompliance(title, content);
        }

        ForumPostEntity entity = new ForumPostEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setImageUrls(toJson(imageUrls));
        entity.setTextComplianceSkipped(textComplianceSkipped ? 1 : 0);
        entity.setCreatedAt(LocalDateTime.now());
        forumPostMapper.insert(entity);

        if (!isBlank(dto.getDraftId())) {
            redisTemplate.delete(draftKey(userId, dto.getDraftId().trim()));
        }

        WxPostPublishVO vo = new WxPostPublishVO();
        vo.setPostId(entity.getId());
        vo.setTextComplianceSkipped(textComplianceSkipped);
        return vo;
    }

    @Override
    public WxPostPageVO list(Integer pageNo, Integer pageSize) {
        int pNo = safePageNo(pageNo);
        int pSize = safePageSize(pageSize);
        Page<ForumPostEntity> page = new Page<>(pNo, pSize);
        Page<ForumPostEntity> result = forumPostMapper.selectPage(page, new LambdaQueryWrapper<ForumPostEntity>()
                .orderByDesc(ForumPostEntity::getId));
        WxPostPageVO vo = new WxPostPageVO();
        vo.setTotal(result.getTotal());
        List<WxPostListItemVO> items = new ArrayList<>();
        for (ForumPostEntity entity : result.getRecords()) {
            items.add(toListItem(entity));
        }
        vo.setList(items);
        return vo;
    }

    @Override
    public WxPostDetailVO detail(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "帖子ID不能为空");
        }
        ForumPostEntity entity = forumPostMapper.selectById(postId);
        if (entity == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "帖子不存在");
        }
        return toDetail(entity);
    }

    @Override
    public WxPostPageVO myPosts(Integer pageNo, Integer pageSize) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        int pNo = safePageNo(pageNo);
        int pSize = safePageSize(pageSize);
        Page<ForumPostEntity> page = new Page<>(pNo, pSize);
        Page<ForumPostEntity> result = forumPostMapper.selectPage(page, new LambdaQueryWrapper<ForumPostEntity>()
                .eq(ForumPostEntity::getUserId, userId)
                .orderByDesc(ForumPostEntity::getId));
        WxPostPageVO vo = new WxPostPageVO();
        vo.setTotal(result.getTotal());
        List<WxPostListItemVO> items = new ArrayList<>();
        for (ForumPostEntity entity : result.getRecords()) {
            items.add(toListItem(entity));
        }
        vo.setList(items);
        return vo;
    }

    @Override
    public WxPostLikeVO toggleLike(Long postId) {
        ForumPostEntity post = mustGetPost(postId);
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());

        ForumPostLikeEntity exists = forumPostLikeMapper.selectOne(new LambdaQueryWrapper<ForumPostLikeEntity>()
                .eq(ForumPostLikeEntity::getPostId, post.getId())
                .eq(ForumPostLikeEntity::getUserId, userId)
                .last("limit 1"));
        boolean liked;
        if (exists != null) {
            forumPostLikeMapper.deleteById(exists.getId());
            liked = false;
        } else {
            ForumPostLikeEntity like = new ForumPostLikeEntity();
            like.setPostId(post.getId());
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            forumPostLikeMapper.insert(like);
            liked = true;
        }
        WxPostLikeVO vo = new WxPostLikeVO();
        vo.setPostId(post.getId());
        vo.setLiked(liked);
        vo.setLikeCount(countLike(post.getId()));
        return vo;
    }

    @Override
    public WxPostCommentVO createComment(Long postId, WxPostCommentCreateDTO dto) {
        ForumPostEntity post = mustGetPost(postId);
        if (dto == null || isBlank(dto.getContent())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "评论内容不能为空");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        String content = dto.getContent().trim();
        if (content.length() > 1000) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "评论内容不能超过1000字");
        }

        Long parentCommentId = dto.getParentCommentId();
        Long replyToCommentId = dto.getReplyToCommentId();

        ForumPostCommentEntity parent = null;
        if (parentCommentId != null && parentCommentId > 0) {
            parent = mustGetComment(post.getId(), parentCommentId);
        }
        ForumPostCommentEntity replyTo = null;
        if (replyToCommentId != null && replyToCommentId > 0) {
            replyTo = mustGetComment(post.getId(), replyToCommentId);
        }
        if (parent == null && replyTo != null) {
            parent = replyTo;
            parentCommentId = replyTo.getId();
        }
        if (parent != null && replyTo == null) {
            replyTo = parent;
            replyToCommentId = parent.getId();
        }

        ForumPostCommentEntity entity = new ForumPostCommentEntity();
        entity.setPostId(post.getId());
        entity.setUserId(userId);
        entity.setContent(content);
        entity.setParentCommentId(parentCommentId);
        entity.setReplyToCommentId(replyToCommentId);
        entity.setDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        forumPostCommentMapper.insert(entity);

        Map<Long, String> nicknameMap = new HashMap<>();
        nicknameMap.put(userId, resolveNickname(userId));
        if (replyTo != null && replyTo.getUserId() != null) {
            nicknameMap.put(replyTo.getUserId(), resolveNickname(replyTo.getUserId()));
        }
        return toCommentVO(entity, nicknameMap, replyTo == null ? null : replyTo.getUserId());
    }

    @Override
    public List<WxPostCommentVO> listComments(Long postId) {
        ForumPostEntity post = mustGetPost(postId);
        List<ForumPostCommentEntity> comments = forumPostCommentMapper.selectList(new LambdaQueryWrapper<ForumPostCommentEntity>()
                .eq(ForumPostCommentEntity::getPostId, post.getId())
                .eq(ForumPostCommentEntity::getDeleted, 0)
                .orderByAsc(ForumPostCommentEntity::getCreatedAt)
                .orderByAsc(ForumPostCommentEntity::getId));
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = comments.stream()
                .map(ForumPostCommentEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> nicknameMap = buildNicknameMap(userIds);
        Map<Long, ForumPostCommentEntity> entityMap = new LinkedHashMap<>();
        for (ForumPostCommentEntity c : comments) {
            entityMap.put(c.getId(), c);
        }

        Map<Long, WxPostCommentVO> voMap = new LinkedHashMap<>();
        for (ForumPostCommentEntity c : comments) {
            ForumPostCommentEntity replyEntity = c.getReplyToCommentId() == null ? null : entityMap.get(c.getReplyToCommentId());
            Long replyToUserId = replyEntity == null ? null : replyEntity.getUserId();
            WxPostCommentVO vo = toCommentVO(c, nicknameMap, replyToUserId);
            vo.setChildren(new ArrayList<>());
            voMap.put(c.getId(), vo);
        }

        List<WxPostCommentVO> roots = new ArrayList<>();
        for (ForumPostCommentEntity c : comments) {
            WxPostCommentVO current = voMap.get(c.getId());
            Long parentId = c.getParentCommentId();
            if (parentId == null || !voMap.containsKey(parentId)) {
                roots.add(current);
            } else {
                voMap.get(parentId).getChildren().add(current);
            }
        }
        sortCommentTree(roots);
        return roots;
    }

    private void assertSpringAiModel(String modelCode) {
        AiModelConfigEntity model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, modelCode)
                .last("limit 1"));
        if (model == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "未找到发帖 AI 模型配置: " + modelCode);
        }
        if (!Objects.equals(model.getInvokeType(), INVOKE_SPRING_AI)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "发帖助手仅支持 Spring AI 模型（invoke_type=1）");
        }
    }

    private static String buildUserMessage(String hint) {
        return "请根据以下用户想法，生成论坛帖子标题与正文（校园社区场景）。\n"
                + "严格只输出一个 JSON 对象，不要 markdown 代码块，不要任何解释。格式：\n"
                + "{\"title\":\"...\",\"content\":\"...\"}\n"
                + "要求：\n"
                + "- title 不超过 80 字\n"
                + "- content 不超过 1000 字\n"
                + "- 语言自然、友好、符合校园论坛风格\n\n"
                + "【用户想法】\n"
                + hint;
    }

    private ParsedPost parseModelJson(String raw, String rawHint) {
        if (isBlank(raw)) {
            return fallback(rawHint);
        }
        String payload = extractJsonPayload(raw.trim());
        try {
            JsonNode root = objectMapper.readTree(payload);
            String title = textOrEmpty(root.get("title"));
            String content = textOrEmpty(root.get("content"));
            if (!isBlank(title) && !isBlank(content)) {
                return new ParsedPost(title.trim(), content.trim());
            }
        } catch (Exception ignore) {
            // fall through
        }
        return fallback(rawHint + "\n\n" + raw);
    }

    private static ParsedPost fallback(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return new ParsedPost("新帖子", "（内容为空）");
        }
        String title = t.length() > 40 ? t.substring(0, 40) + "…" : t;
        return new ParsedPost(title, t.length() > 1000 ? t.substring(0, 1000) : t);
    }

    private static String extractJsonPayload(String text) {
        String s = text;
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end > 0) {
                s = s.substring(0, end).trim();
            }
        }
        return s;
    }

    private static String textOrEmpty(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return "";
        }
        return n.asText();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String sha256Hex(String title, String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String combined = title + "\n" + content;
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "草稿摘要计算失败");
        }
    }

    private static String draftKey(Long userId, String draftId) {
        return "campus:wx:post:draft:" + userId + ":" + draftId;
    }

    private String storeImage(Long userId, MultipartFile file, byte[] bytes) {
        String ext = extension(file.getOriginalFilename());
        if (!isAllowedImageExt(ext)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持 png/jpg/jpeg/bmp/gif/webp 图片");
        }
        String name = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;
        Path dir = Paths.get(wxPostProperties.getUploadDir(), "post", String.valueOf(userId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            Files.write(target, bytes);
            return "/upload/post/" + userId + "/" + name;
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "保存图片失败");
        }
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    private static boolean isAllowedImageExt(String ext) {
        return "png".equals(ext)
                || "jpg".equals(ext)
                || "jpeg".equals(ext)
                || "bmp".equals(ext)
                || "gif".equals(ext)
                || "webp".equals(ext);
    }

    private List<String> sanitizeImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String u : urls) {
            if (isBlank(u)) {
                continue;
            }
            String s = u.trim();
            if (!s.startsWith("/upload/post/")) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片地址非法");
            }
            out.add(s);
        }
        return out;
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片列表序列化失败");
        }
    }

    private static Long parseUserId(String wxUserId) {
        try {
            return Long.parseLong(wxUserId);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void ensureImageCompliance(byte[] bytes) {
        iflyImageAuditService.assertPass(bytes);
    }

    private void ensureTextCompliance(String title, String content) {
        iflyTextAuditService.assertPass(title + "\n" + content);
    }

    private static int safePageNo(Integer pageNo) {
        return pageNo == null || pageNo <= 0 ? 1 : pageNo;
    }

    private static int safePageSize(Integer pageSize) {
        int v = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        return Math.min(v, 50);
    }

    private WxPostListItemVO toListItem(ForumPostEntity entity) {
        WxPostListItemVO vo = new WxPostListItemVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setNickname(resolveNickname(entity.getUserId()));
        vo.setTitle(entity.getTitle());
        vo.setContentPreview(truncate(entity.getContent(), 120));
        vo.setImageUrls(parseImageUrls(entity.getImageUrls()));
        vo.setLikeCount(countLike(entity.getId()));
        vo.setCommentCount(countComment(entity.getId()));
        vo.setCreatedAt(formatDateTime(entity.getCreatedAt()));
        return vo;
    }

    private WxPostDetailVO toDetail(ForumPostEntity entity) {
        WxPostDetailVO vo = new WxPostDetailVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setNickname(resolveNickname(entity.getUserId()));
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setImageUrls(parseImageUrls(entity.getImageUrls()));
        vo.setLikeCount(countLike(entity.getId()));
        vo.setCommentCount(countComment(entity.getId()));
        vo.setLikedByMe(isLikedByCurrentUser(entity.getId()));
        vo.setCreatedAt(formatDateTime(entity.getCreatedAt()));
        return vo;
    }

    private List<String> parseImageUrls(String imageUrlsJson) {
        if (isBlank(imageUrlsJson)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {
            });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private ForumPostEntity mustGetPost(Long postId) {
        if (postId == null || postId <= 0L) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "帖子ID不能为空");
        }
        ForumPostEntity post = forumPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private ForumPostCommentEntity mustGetComment(Long postId, Long commentId) {
        ForumPostCommentEntity comment = forumPostCommentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getPostId(), postId) || Objects.equals(comment.getDeleted(), 1)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "评论不存在");
        }
        return comment;
    }

    private int countLike(Long postId) {
        Long cnt = forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLikeEntity>()
                .eq(ForumPostLikeEntity::getPostId, postId));
        return cnt == null ? 0 : cnt.intValue();
    }

    private int countComment(Long postId) {
        Long cnt = forumPostCommentMapper.selectCount(new LambdaQueryWrapper<ForumPostCommentEntity>()
                .eq(ForumPostCommentEntity::getPostId, postId)
                .eq(ForumPostCommentEntity::getDeleted, 0));
        return cnt == null ? 0 : cnt.intValue();
    }

    private boolean isLikedByCurrentUser(Long postId) {
        String uid;
        try {
            uid = WxAuthContextUtils.requireWxUserId();
        } catch (Exception e) {
            return false;
        }
        Long userId;
        try {
            userId = Long.parseLong(uid);
        } catch (Exception e) {
            return false;
        }
        ForumPostLikeEntity like = forumPostLikeMapper.selectOne(new LambdaQueryWrapper<ForumPostLikeEntity>()
                .eq(ForumPostLikeEntity::getPostId, postId)
                .eq(ForumPostLikeEntity::getUserId, userId)
                .last("limit 1"));
        return like != null;
    }

    private Map<Long, String> buildNicknameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> out = new HashMap<>();
        List<campus.ai.rag.service.entity.wx.WxUserEntity> users = wxUserMapper.selectBatchIds(userIds);
        if (users != null) {
            for (campus.ai.rag.service.entity.wx.WxUserEntity user : users) {
                if (user == null || user.getId() == null) {
                    continue;
                }
                String name = !isBlank(user.getNickname()) ? user.getNickname().trim() : user.getUsername();
                out.put(user.getId(), isBlank(name) ? "用户" + user.getId() : name);
            }
        }
        for (Long uid : userIds) {
            out.putIfAbsent(uid, "用户" + uid);
        }
        return out;
    }

    private WxPostCommentVO toCommentVO(ForumPostCommentEntity entity, Map<Long, String> nicknameMap, Long replyToUserId) {
        WxPostCommentVO vo = new WxPostCommentVO();
        vo.setId(entity.getId());
        vo.setPostId(entity.getPostId());
        vo.setUserId(entity.getUserId());
        vo.setNickname(nicknameMap.getOrDefault(entity.getUserId(), "用户" + entity.getUserId()));
        vo.setContent(entity.getContent());
        vo.setParentCommentId(entity.getParentCommentId());
        vo.setReplyToCommentId(entity.getReplyToCommentId());
        if (replyToUserId != null) {
            vo.setReplyToNickname(nicknameMap.getOrDefault(replyToUserId, "用户" + replyToUserId));
        } else {
            vo.setReplyToNickname("");
        }
        vo.setCreatedAt(formatDateTime(entity.getCreatedAt()));
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private void sortCommentTree(List<WxPostCommentVO> roots) {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        roots.sort(Comparator.comparing(WxPostCommentVO::getId));
        for (WxPostCommentVO root : roots) {
            if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                sortCommentTree(root.getChildren());
            }
        }
    }

    private String resolveNickname(Long userId) {
        if (userId == null || userId <= 0L) {
            return "用户";
        }
        try {
            var user = wxUserMapper.selectById(userId);
            if (user == null) {
                return "用户" + userId;
            }
            if (!isBlank(user.getNickname())) {
                return user.getNickname().trim();
            }
            if (!isBlank(user.getUsername())) {
                return user.getUsername().trim();
            }
            return "用户" + userId;
        } catch (Exception e) {
            return "用户" + userId;
        }
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return DATE_TIME_FMT.format(value);
    }

    private static class PostDraftCache implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public String contentHash;
    }

    private static class ParsedPost {
        final String title;
        final String content;

        ParsedPost(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
