package campus.ai.rag.service.controller.wx.rag;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxRagDTO;
import campus.ai.rag.service.service.wx.rag.WxRagService;
import campus.ai.rag.service.vo.wx.WxRagVO;
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
@RequestMapping("/wx/rag")
@Api(tags = "微信端RAG接口")
@Tag(name = "微信端RAG")
public class WxRagController {

    private final WxRagService wxRagService;

    public WxRagController(WxRagService wxRagService) {
        this.wxRagService = wxRagService;
    }

    @PostMapping
    @ApiOperation(value = "RAG问答", notes = "向量检索kb_chunk并注入上下文，拼接 BOTH+RAG 提示词后进行回答")
    @ApiImplicitParam(name = "dto", value = "RAG请求参数", required = true, dataType = "WxRagDTO", paramType = "body")
    @Operation(summary = "RAG问答")
    public Result<WxRagVO> rag(@RequestBody WxRagDTO dto) {
        return Result.success(wxRagService.rag(dto));
    }
}

