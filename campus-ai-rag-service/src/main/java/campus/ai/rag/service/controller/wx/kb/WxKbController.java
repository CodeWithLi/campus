package campus.ai.rag.service.controller.wx.kb;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxKbUploadDTO;
import campus.ai.rag.service.entity.kb.KbCategoryEntity;
import campus.ai.rag.service.mapper.kb.KbCategoryMapper;
import campus.ai.rag.service.service.wx.kb.WxKbDocumentService;
import campus.ai.rag.service.vo.wx.WxKbCategoryVO;
import campus.ai.rag.service.vo.wx.WxKbDocumentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Api(tags = "微信端-个人知识库")
@RestController
@RequestMapping("/wx/kb")
public class WxKbController {

    private final WxKbDocumentService wxKbDocumentService;
    private final KbCategoryMapper kbCategoryMapper;

    public WxKbController(WxKbDocumentService wxKbDocumentService,
                          KbCategoryMapper kbCategoryMapper) {
        this.wxKbDocumentService = wxKbDocumentService;
        this.kbCategoryMapper = kbCategoryMapper;
    }

    @ApiOperation("上传个人文档（后台异步自动解析+切片+向量化）")
    @PostMapping("/documents/upload")
    public Result<WxKbDocumentVO> upload(@ModelAttribute WxKbUploadDTO dto,
                                         @RequestPart("file") MultipartFile file) {
        return Result.success(wxKbDocumentService.upload(dto, file));
    }

    @ApiOperation("获取可用知识库分类")
    @GetMapping("/categories")
    public Result<List<WxKbCategoryVO>> categories() {
        List<KbCategoryEntity> list = kbCategoryMapper.selectList(new LambdaQueryWrapper<KbCategoryEntity>()
                .eq(KbCategoryEntity::getEnabled, 1)
                .orderByAsc(KbCategoryEntity::getSortNo)
                .orderByDesc(KbCategoryEntity::getId));
        List<WxKbCategoryVO> out = (list == null ? List.<KbCategoryEntity>of() : list).stream().map(item -> {
            WxKbCategoryVO vo = new WxKbCategoryVO();
            vo.setId(item.getId());
            vo.setCategoryCode(item.getCategoryCode());
            vo.setCategoryName(item.getCategoryName());
            return vo;
        }).toList();
        return Result.success(out);
    }
}

