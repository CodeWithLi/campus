package campus.ai.rag.service.controller.wx.kb;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxKbUploadDTO;
import campus.ai.rag.service.service.wx.kb.WxKbDocumentService;
import campus.ai.rag.service.vo.wx.WxKbDocumentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "微信端-个人知识库")
@RestController
@RequestMapping("/wx/kb")
public class WxKbController {

    private final WxKbDocumentService wxKbDocumentService;

    public WxKbController(WxKbDocumentService wxKbDocumentService) {
        this.wxKbDocumentService = wxKbDocumentService;
    }

    @ApiOperation("上传个人文档（后台异步自动解析+切片+向量化）")
    @PostMapping("/documents/upload")
    public Result<WxKbDocumentVO> upload(@ModelAttribute WxKbUploadDTO dto,
                                         @RequestPart("file") MultipartFile file) {
        return Result.success(wxKbDocumentService.upload(dto, file));
    }
}

