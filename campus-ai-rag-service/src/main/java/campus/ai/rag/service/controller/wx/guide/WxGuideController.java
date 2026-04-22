package campus.ai.rag.service.controller.wx.guide;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.wx.guide.WxGuideService;
import campus.ai.rag.service.vo.wx.WxGuideListItemVO;
import campus.ai.rag.service.vo.wx.WxGuideVO;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wx/guides")
@Api(tags = "微信端办事指南")
@Tag(name = "微信端办事指南")
public class WxGuideController {

    private final WxGuideService wxGuideService;

    public WxGuideController(WxGuideService wxGuideService) {
        this.wxGuideService = wxGuideService;
    }

    @GetMapping
    @Operation(summary = "办事指南列表（仅上架）")
    public Result<List<WxGuideListItemVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category) {
        return Result.success(wxGuideService.listPublished(keyword, category));
    }

    @GetMapping("/categories")
    @Operation(summary = "办事指南分类列表（仅上架）")
    public Result<List<String>> categories() {
        return Result.success(wxGuideService.listPublishedCategories());
    }

    @GetMapping("/{guideId}")
    @Operation(summary = "办事指南详情（仅上架）")
    public Result<WxGuideVO> detail(@PathVariable("guideId") Long guideId) {
        return Result.success(wxGuideService.detail(guideId));
    }
}
