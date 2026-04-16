package campus.ai.rag.service.controller.wx.chat;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxChatDTO;
import campus.ai.rag.service.service.wx.chat.WxChatService;
import campus.ai.rag.service.utils.WxAuthContextUtils;

import campus.ai.rag.service.vo.wx.WxChatConversationSummaryVO;
import campus.ai.rag.service.vo.wx.WxChatConversationDetailVO;
import campus.ai.rag.service.vo.wx.WxChatVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/wx/chat")
@Api(tags = "微信端聊天接口")
@Tag(name = "微信端聊天")
public class WxChatController {

    private final WxChatService wxChatService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WxChatController(WxChatService wxChatService,
                             RedisTemplate<String, Object> redisTemplate) {
        this.wxChatService = wxChatService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    @ApiOperation(value = "单轮对话", notes = "根据前端传入的模型编码和消息内容进行单轮对话")
    @ApiImplicitParam(name = "dto", value = "聊天请求参数", required = true, dataType = "WxChatDTO", paramType = "body")
    @Operation(summary = "单轮对话")
    public Result<WxChatVO> chat(@RequestBody WxChatDTO dto) {
        return Result.success(wxChatService.chat(dto));
    }

    @GetMapping("/conversations")
    public Result<List<WxChatConversationSummaryVO>> conversations() {
        String userId = WxAuthContextUtils.requireWxUserId();

        String prefix = "campus:wx:chat:conv:" + userId + ":";
        String suffix = ":meta";
        String pattern = prefix + "*:meta";

        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Result.success(List.of());
        }

        List<WxChatConversationSummaryVO> out = new ArrayList<>();
        for (String key : keys) {
            if (key == null) continue;
            if (!key.startsWith(prefix) || !key.endsWith(suffix)) continue;

            String conversationId = key.substring(prefix.length(), key.length() - suffix.length());
            String metaKey = "campus:wx:chat:conv:" + userId + ":" + conversationId + ":meta";

            Object titleObj = redisTemplate.opsForHash().get(metaKey, "title");
            Object updatedAtObj = redisTemplate.opsForHash().get(metaKey, "updatedAt");

            WxChatConversationSummaryVO vo = new WxChatConversationSummaryVO();
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

        out.sort(Comparator.comparing(WxChatConversationSummaryVO::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return Result.success(out);
    }

    @GetMapping("/conversations/{conversationId}")
    public Result<WxChatConversationDetailVO> conversation(@PathVariable("conversationId") String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new campus.ai.rag.service.exception.BusinessException(
                    campus.ai.rag.service.common.StatusCodeEnum.BAD_REQUEST,
                    "conversationId不能为空"
            );
        }

        String userId = WxAuthContextUtils.requireWxUserId();
        String historyKey = "campus:wx:chat:conv:" + userId + ":" + conversationId + ":history";
        String metaKey = "campus:wx:chat:conv:" + userId + ":" + conversationId + ":meta";

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

        WxChatConversationDetailVO vo = new WxChatConversationDetailVO();
        vo.setConversationTitle(conversationTitle);
        vo.setMessages(messages);
        return Result.success(vo);
    }
}

