package campus.ai.rag.service.controller.wx.resume;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxResumeChatDTO;
import campus.ai.rag.service.dto.wx.WxResumeGenerateDTO;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.wx.resume.WxResumeService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxChatConversationDetailVO;
import campus.ai.rag.service.vo.wx.WxChatConversationSummaryVO;
import campus.ai.rag.service.vo.wx.WxResumeChatVO;
import campus.ai.rag.service.vo.wx.WxResumeGenerateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/wx/resume")
@Api(tags = "微信端简历生成接口")
@Tag(name = "微信端简历生成")
public class WxResumeController {

    private final WxResumeService wxResumeService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WxResumeController(WxResumeService wxResumeService,
                              RedisTemplate<String, Object> redisTemplate) {
        this.wxResumeService = wxResumeService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/chat")
    @ApiOperation(value = "简历对话助手", notes = "用于生成/润色简历信息，提示词场景：RESUME")
    @ApiImplicitParam(name = "dto", value = "简历对话参数", required = true, dataType = "WxResumeChatDTO", paramType = "body")
    @Operation(summary = "简历对话助手")
    public Result<WxResumeChatVO> chat(@RequestBody WxResumeChatDTO dto) {
        return Result.success(wxResumeService.chat(dto));
    }

    @PostMapping("/generate")
    @ApiOperation(value = "调用星火简历生成API", notes = "第二步：将简历文本提交星火简历生成，返回下载链接")
    @ApiImplicitParam(name = "dto", value = "简历生成参数", required = true, dataType = "WxResumeGenerateDTO", paramType = "body")
    @Operation(summary = "调用星火简历生成")
    public Result<WxResumeGenerateVO> generate(@RequestBody WxResumeGenerateDTO dto) {
        return Result.success(wxResumeService.generate(dto));
    }

    @GetMapping("/conversations")
    @ApiOperation(value = "简历对话历史列表", notes = "返回会话标题列表（RESUME）")
    @Operation(summary = "简历对话历史列表")
    public Result<List<WxChatConversationSummaryVO>> conversations() {
        String userId = WxAuthContextUtils.requireWxUserId();
        String prefix = "campus:wx:resume:conv:" + userId + ":";
        String suffix = ":meta";
        String pattern = prefix + "*:meta";

        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Result.success(List.of());
        }

        List<WxChatConversationSummaryVO> out = new ArrayList<>();
        for (String key : keys) {
            if (key == null || !key.startsWith(prefix) || !key.endsWith(suffix)) continue;
            String conversationId = key.substring(prefix.length(), key.length() - suffix.length());
            String metaKey = prefix + conversationId + ":meta";
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
                } catch (Exception ignore) {
                    vo.setUpdatedAt(null);
                }
            }
            out.add(vo);
        }
        out.sort(Comparator.comparing(WxChatConversationSummaryVO::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return Result.success(out);
    }

    @GetMapping("/conversations/{conversationId}")
    @ApiOperation(value = "简历对话历史详情", notes = "返回会话消息明细（RESUME）")
    @Operation(summary = "简历对话历史详情")
    public Result<WxChatConversationDetailVO> conversation(@PathVariable("conversationId") String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "conversationId不能为空");
        }
        String userId = WxAuthContextUtils.requireWxUserId();
        String historyKey = "campus:wx:resume:conv:" + userId + ":" + conversationId + ":history";
        String metaKey = "campus:wx:resume:conv:" + userId + ":" + conversationId + ":meta";

        Object titleObj = redisTemplate.opsForHash().get(metaKey, "title");
        String conversationTitle = titleObj == null ? "简历历史会话" : String.valueOf(titleObj);

        List<Object> raw = redisTemplate.opsForList().range(historyKey, 0, -1);
        if (raw == null) raw = List.of();

        List<WxChatConversationDetailVO.WxChatConversationMessageVO> messages = new ArrayList<>();
        for (Object item : raw) {
            if (item == null) continue;
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
            String role = map.get("role") == null ? "" : String.valueOf(map.get("role")).toLowerCase();
            if (!"user".equals(role) && !"assistant".equals(role)) role = "assistant";
            String content = map.get("content") == null ? "" : String.valueOf(map.get("content"));

            WxChatConversationDetailVO.WxChatConversationMessageVO msg = new WxChatConversationDetailVO.WxChatConversationMessageVO();
            msg.setRole(role);
            msg.setContent(content);
            messages.add(msg);
        }

        WxChatConversationDetailVO vo = new WxChatConversationDetailVO();
        vo.setConversationTitle(conversationTitle);
        vo.setMessages(messages);
        return Result.success(vo);
    }
}

