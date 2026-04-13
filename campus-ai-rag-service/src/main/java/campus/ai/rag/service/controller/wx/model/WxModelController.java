package campus.ai.rag.service.controller.wx.model;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.wx.model.WxModelService;
import campus.ai.rag.service.vo.wx.WxModelOptionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wx/models")
@Api(tags = "微信端模型接口")
@Tag(name = "微信端模型")
public class WxModelController {

    private final WxModelService wxModelService;

    public WxModelController(WxModelService wxModelService) {
        this.wxModelService = wxModelService;
    }

    @GetMapping("/enabled")
    @ApiOperation(value = "查询可用模型列表", notes = "只返回启用状态模型，供前端渲染模型名称")
    @Operation(summary = "查询可用模型列表")
    public Result<List<WxModelOptionVO>> listEnabled() {
        return Result.success(wxModelService.listEnabledModels());
    }
}

