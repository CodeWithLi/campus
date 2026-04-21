package campus.ai.rag.service.controller.wx.post;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxPostAiGenerateDTO;
import campus.ai.rag.service.dto.wx.WxPostCommentCreateDTO;
import campus.ai.rag.service.dto.wx.WxPostPublishDTO;
import campus.ai.rag.service.service.wx.post.WxPostService;
import campus.ai.rag.service.vo.wx.WxPostAiGenerateVO;
import campus.ai.rag.service.vo.wx.WxPostCommentVO;
import campus.ai.rag.service.vo.wx.WxPostDetailVO;
import campus.ai.rag.service.vo.wx.WxPostFavoriteVO;
import campus.ai.rag.service.vo.wx.WxPostInteractionPageVO;
import campus.ai.rag.service.vo.wx.WxPostLikeVO;
import campus.ai.rag.service.vo.wx.WxPostPageVO;
import campus.ai.rag.service.vo.wx.WxPostPublishVO;
import campus.ai.rag.service.vo.wx.WxPostUploadImageVO;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/wx/post")
@Api(tags = "微信端发帖")
@Tag(name = "微信端发帖")
public class WxPostController {

    private final WxPostService wxPostService;

    public WxPostController(WxPostService wxPostService) {
        this.wxPostService = wxPostService;
    }

    @PostMapping("/ai-generate")
    @Operation(summary = "AI 生成标题与正文（POST 场景提示词 + qwen_plus SpringAI）")
    public Result<WxPostAiGenerateVO> aiGenerate(@RequestBody WxPostAiGenerateDTO dto) {
        return Result.success(wxPostService.aiGenerate(dto));
    }

    @PostMapping("/upload-images")
    @Operation(summary = "上传帖子图片（最多5张，逐张图片合规）")
    public Result<WxPostUploadImageVO> uploadImages(@RequestParam("files") MultipartFile[] files) {
        return Result.success(wxPostService.uploadImages(files));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布帖子（草稿一致可跳过文本合规）")
    public Result<WxPostPublishVO> publish(@RequestBody WxPostPublishDTO dto) {
        return Result.success(wxPostService.publish(dto));
    }

    @GetMapping("/list")
    @Operation(summary = "帖子列表")
    public Result<WxPostPageVO> list(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                     @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(wxPostService.list(pageNo, pageSize));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "帖子详情")
    public Result<WxPostDetailVO> detail(@PathVariable("postId") Long postId) {
        return Result.success(wxPostService.detail(postId));
    }

    @GetMapping("/my")
    @Operation(summary = "我的帖子")
    public Result<WxPostPageVO> my(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(wxPostService.myPosts(pageNo, pageSize));
    }

    @GetMapping("/my-favorites")
    @Operation(summary = "我的收藏")
    public Result<WxPostPageVO> myFavorites(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(wxPostService.myFavorites(pageNo, pageSize));
    }

    @GetMapping("/my-interactions")
    @Operation(summary = "我的互动（收到的点赞/评论）")
    public Result<WxPostInteractionPageVO> myInteractions(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                          @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(wxPostService.myInteractions(pageNo, pageSize));
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "帖子点赞/取消点赞")
    public Result<WxPostLikeVO> toggleLike(@PathVariable("postId") Long postId) {
        return Result.success(wxPostService.toggleLike(postId));
    }

    @PostMapping("/{postId}/favorite")
    @Operation(summary = "帖子收藏/取消收藏")
    public Result<WxPostFavoriteVO> toggleFavorite(@PathVariable("postId") Long postId) {
        return Result.success(wxPostService.toggleFavorite(postId));
    }

    @PostMapping("/{postId}/comments")
    @Operation(summary = "发表评论/回复评论")
    public Result<WxPostCommentVO> createComment(@PathVariable("postId") Long postId,
                                                 @RequestBody WxPostCommentCreateDTO dto) {
        return Result.success(wxPostService.createComment(postId, dto));
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "评论列表（多级）")
    public Result<List<WxPostCommentVO>> comments(@PathVariable("postId") Long postId) {
        return Result.success(wxPostService.listComments(postId));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "删除我的帖子")
    public Result<Boolean> delete(@PathVariable("postId") Long postId) {
        return Result.success(wxPostService.deleteMyPost(postId));
    }
}
