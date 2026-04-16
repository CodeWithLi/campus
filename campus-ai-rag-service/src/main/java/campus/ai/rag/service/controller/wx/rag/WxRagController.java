package campus.ai.rag.service.controller.wx.rag;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxRagDTO;
import campus.ai.rag.service.entity.kb.WxKbDocumentEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.wx.rag.WxRagService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxRagVO;
import campus.ai.rag.service.vo.wx.WxRagConversationDetailVO;
import campus.ai.rag.service.vo.wx.WxRagConversationFileVO;
import campus.ai.rag.service.vo.wx.WxRagConversationSummaryVO;
import campus.ai.rag.service.vo.wx.WxChatConversationDetailVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import campus.ai.rag.service.mapper.kb.WxKbDocumentMapper;
import campus.ai.rag.service.common.StatusCodeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/wx/rag")
@Api(tags = "微信端RAG接口")
@Tag(name = "微信端RAG")
public class WxRagController {

    private final WxRagService wxRagService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WxKbDocumentMapper wxKbDocumentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WxRagController(WxRagService wxRagService,
                            RedisTemplate<String, Object> redisTemplate,
                            WxKbDocumentMapper wxKbDocumentMapper) {
        this.wxRagService = wxRagService;
        this.redisTemplate = redisTemplate;
        this.wxKbDocumentMapper = wxKbDocumentMapper;
    }

    @PostMapping
    @ApiOperation(value = "RAG问答", notes = "向量检索kb_chunk并注入上下文，拼接 BOTH+RAG 提示词后进行回答")
    @ApiImplicitParam(name = "dto", value = "RAG请求参数", required = true, dataType = "WxRagDTO", paramType = "body")
    @Operation(summary = "RAG问答")
    public Result<WxRagVO> rag(@RequestBody WxRagDTO dto) {
        return Result.success(wxRagService.rag(dto));
    }

    @GetMapping("/conversations")
    @Operation(summary = "RAG会话列表")
    public Result<List<WxRagConversationSummaryVO>> conversations() {
        String userId = WxAuthContextUtils.requireWxUserId();

        String prefix = "campus:wx:rag:conv:" + userId + ":";
        String suffix = ":meta";
        String pattern = prefix + "*:meta";

        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Result.success(List.of());
        }

        List<WxRagConversationSummaryVO> out = new ArrayList<>();
        for (String key : keys) {
            if (key == null) continue;
            if (!key.startsWith(prefix) || !key.endsWith(suffix)) continue;

            String conversationId = key.substring(prefix.length(), key.length() - suffix.length());
            String metaKey = prefix + conversationId + ":meta";

            Object titleObj = redisTemplate.opsForHash().get(metaKey, "title");
            Object updatedAtObj = redisTemplate.opsForHash().get(metaKey, "updatedAt");

            WxRagConversationSummaryVO vo = new WxRagConversationSummaryVO();
            vo.setConversationId(conversationId);
            vo.setConversationTitle(titleObj == null ? null : String.valueOf(titleObj));
            if (updatedAtObj == null) {
                vo.setUpdatedAt(null);
            } else {
                try {
                    vo.setUpdatedAt(Long.parseLong(String.valueOf(updatedAtObj)));
                } catch (Exception ignored) {
                    vo.setUpdatedAt(null);
                }
            }
            out.add(vo);
        }

        out.sort(Comparator.comparing(WxRagConversationSummaryVO::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return Result.success(out);
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "RAG会话详情（返回消息 + 文件）")
    public Result<WxRagConversationDetailVO> conversation(@PathVariable("conversationId") String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "conversationId不能为空");
        }

        String userId = WxAuthContextUtils.requireWxUserId();
        String historyKey = "campus:wx:rag:conv:" + userId + ":" + conversationId + ":history";
        String metaKey = "campus:wx:rag:conv:" + userId + ":" + conversationId + ":meta";
        String filesKey = "campus:wx:rag:conv:" + userId + ":" + conversationId + ":files";

        Object titleObj = redisTemplate.opsForHash().get(metaKey, "title");
        String conversationTitle = titleObj == null ? "历史会话" : String.valueOf(titleObj);

        List<Object> raw = redisTemplate.opsForList().range(historyKey, 0, -1);
        if (raw == null) raw = List.of();

        List<WxChatConversationDetailVO.WxChatConversationMessageVO> messages = new ArrayList<>();
        for (Object item : raw) {
            if (item == null) continue;

            // 兼容 RedisTemplate 反序列化结果（可能是 WxMsg，也可能是 Map）
            Map<?, ?> map = null;
            if (item instanceof Map) {
                map = (Map<?, ?>) item;
            } else {
                try {
                    map = objectMapper.convertValue(item, Map.class);
                } catch (Exception ignored) {
                    // ignore
                }
            }
            if (map == null) continue;

            Object roleObj = map.get("role");
            Object contentObj = map.get("content");

            String role = roleObj == null ? "" : String.valueOf(roleObj).toLowerCase();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                role = "assistant";
            }

            WxChatConversationDetailVO.WxChatConversationMessageVO msg = new WxChatConversationDetailVO.WxChatConversationMessageVO();
            msg.setRole(role);
            msg.setContent(contentObj == null ? "" : String.valueOf(contentObj));
            messages.add(msg);
        }

        List<WxRagConversationFileVO> files = new ArrayList<>();
        Set<Object> docIdObjs = redisTemplate.opsForSet().members(filesKey);
        if (docIdObjs != null && !docIdObjs.isEmpty()) {
            List<Long> docIds = docIdObjs.stream()
                    .filter(Objects::nonNull)
                    .map(o -> {
                        try {
                            return Long.parseLong(String.valueOf(o));
                        } catch (Exception ignored) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!docIds.isEmpty()) {
                List<WxKbDocumentEntity> docs = wxKbDocumentMapper.selectList(
                        new LambdaQueryWrapper<WxKbDocumentEntity>()
                                .select(WxKbDocumentEntity::getId,
                                        WxKbDocumentEntity::getTitle,
                                        WxKbDocumentEntity::getOriginalFileName,
                                        WxKbDocumentEntity::getFileExt,
                                        WxKbDocumentEntity::getProcessStatus,
                                        WxKbDocumentEntity::getChunkCount)
                                .eq(WxKbDocumentEntity::getUserId, Long.parseLong(userId))
                                .in(WxKbDocumentEntity::getId, docIds)
                );

                Map<Long, WxKbDocumentEntity> docMap = (docs == null ? List.<WxKbDocumentEntity>of() : docs).stream()
                        .filter(d -> d.getId() != null)
                        .collect(java.util.stream.Collectors.toMap(WxKbDocumentEntity::getId, d -> d, (a, b) -> a));

                for (Long docId : docIds) {
                    WxKbDocumentEntity d = docMap.get(docId);
                    if (d == null) continue;
                    WxRagConversationFileVO vo = new WxRagConversationFileVO();
                    vo.setDocumentId(d.getId());
                    vo.setTitle(d.getTitle());
                    vo.setOriginalFileName(d.getOriginalFileName());
                    vo.setFileExt(d.getFileExt());
                    vo.setProcessStatus(d.getProcessStatus());
                    vo.setChunkCount(d.getChunkCount());
                    files.add(vo);
                }
            }
        }

        WxRagConversationDetailVO vo = new WxRagConversationDetailVO();
        vo.setConversationTitle(conversationTitle);
        vo.setMessages(messages);
        vo.setFiles(files);
        return Result.success(vo);
    }
}

