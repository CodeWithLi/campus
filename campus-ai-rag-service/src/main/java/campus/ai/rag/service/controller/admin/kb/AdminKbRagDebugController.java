package campus.ai.rag.service.controller.admin.kb;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.kb.KbRagDebugDTO;
import campus.ai.rag.service.service.admin.kb.AdminKbRagDebugService;
import campus.ai.rag.service.vo.admin.kb.KbRagDebugVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/kb/rag")
@Api(tags = "后台RAG调试接口")
@Tag(name = "后台RAG调试")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminKbRagDebugController {

    private final AdminKbRagDebugService adminKbRagDebugService;

    public AdminKbRagDebugController(AdminKbRagDebugService adminKbRagDebugService) {
        this.adminKbRagDebugService = adminKbRagDebugService;
    }

    @PostMapping("/debug")
    @ApiOperation(value = "RAG调试/测试", notes = "输入问题、可选分类与TopK，返回召回片段与最终回答")
    @Operation(summary = "RAG调试/测试")
    public Result<KbRagDebugVO> debug(@RequestBody KbRagDebugDTO dto) {
        return Result.success(adminKbRagDebugService.debug(dto));
    }
}

