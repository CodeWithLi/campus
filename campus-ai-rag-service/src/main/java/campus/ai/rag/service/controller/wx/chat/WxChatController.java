package campus.ai.rag.service.controller.wx.chat;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxChatDTO;
import campus.ai.rag.service.service.wx.chat.WxChatService;
import campus.ai.rag.service.vo.wx.WxChatVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wx/chat")
@Api(tags = "微信端聊天接口")
@Tag(name = "微信端聊天")
public class WxChatController {

    private final WxChatService wxChatService;

    public WxChatController(WxChatService wxChatService) {
        this.wxChatService = wxChatService;
    }

    @PostMapping
    @ApiOperation(value = "单轮对话", notes = "根据前端传入的模型编码和消息内容进行单轮对话")
    @ApiImplicitParam(name = "dto", value = "聊天请求参数", required = true, dataType = "WxChatDTO", paramType = "body")
    @Operation(summary = "单轮对话")
    public Result<WxChatVO> chat(@RequestBody WxChatDTO dto) {
        return Result.success(wxChatService.chat(dto));
    }
}

