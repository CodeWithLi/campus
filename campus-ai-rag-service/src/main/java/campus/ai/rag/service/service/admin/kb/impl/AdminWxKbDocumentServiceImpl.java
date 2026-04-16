package campus.ai.rag.service.service.admin.kb.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.kb.WxKbDocumentEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.kb.WxKbDocumentMapper;
import campus.ai.rag.service.service.admin.kb.AdminWxKbDocumentService;
import campus.ai.rag.service.service.admin.kb.DocumentParseService;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentPageVO;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentPreviewVO;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminWxKbDocumentServiceImpl implements AdminWxKbDocumentService {

    private static final int PREVIEW_MAX_CHARS = 12000;

    private final WxKbDocumentMapper wxKbDocumentMapper;
    private final DocumentParseService documentParseService;

    public AdminWxKbDocumentServiceImpl(WxKbDocumentMapper wxKbDocumentMapper,
                                        DocumentParseService documentParseService) {
        this.wxKbDocumentMapper = wxKbDocumentMapper;
        this.documentParseService = documentParseService;
    }

    @Override
    public AdminWxKbDocumentPageVO listPage(Long pageNum, Long pageSize, Long userId) {
        long pNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long pSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100L);

        LambdaQueryWrapper<WxKbDocumentEntity> qw = new LambdaQueryWrapper<WxKbDocumentEntity>()
                .orderByDesc(WxKbDocumentEntity::getId);
        if (userId != null) {
            qw.eq(WxKbDocumentEntity::getUserId, userId);
        }

        Page<WxKbDocumentEntity> page = wxKbDocumentMapper.selectPage(new Page<>(pNum, pSize), qw);
        List<AdminWxKbDocumentVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());

        AdminWxKbDocumentPageVO vo = new AdminWxKbDocumentPageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(list);
        return vo;
    }

    @Override
    public AdminWxKbDocumentVO getById(Long id) {
        return toVO(getRequired(id));
    }

    @Override
    public AdminWxKbDocumentPreviewVO preview(Long id) {
        WxKbDocumentEntity entity = getRequired(id);
        String content = trimOrNull(entity.getParsedContent());
        if (content == null) {
            Path path = Paths.get(entity.getStoragePath());
            content = trimOrNull(documentParseService.parseToText(path, entity.getFileExt()));
        }
        if (content != null && content.length() > PREVIEW_MAX_CHARS) {
            content = content.substring(0, PREVIEW_MAX_CHARS) + "\n\n...（预览已截断）";
        }

        AdminWxKbDocumentPreviewVO vo = new AdminWxKbDocumentPreviewVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setOriginalFileName(entity.getOriginalFileName());
        vo.setFileExt(entity.getFileExt());
        vo.setProcessStatus(entity.getProcessStatus());
        vo.setPreviewContent(content == null ? "" : content);
        return vo;
    }

    private WxKbDocumentEntity getRequired(Long id) {
        if (id == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文档ID不能为空");
        }
        WxKbDocumentEntity entity = wxKbDocumentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "用户文档不存在");
        }
        return entity;
    }

    private AdminWxKbDocumentVO toVO(WxKbDocumentEntity entity) {
        AdminWxKbDocumentVO vo = new AdminWxKbDocumentVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setOriginalFileName(entity.getOriginalFileName());
        vo.setFileExt(entity.getFileExt());
        vo.setContentType(entity.getContentType());
        vo.setFileSize(entity.getFileSize());
        vo.setProcessStatus(entity.getProcessStatus());
        vo.setFailReason(entity.getFailReason());
        vo.setChunkCount(entity.getChunkCount());
        vo.setProcessedAt(entity.getProcessedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trim = value.trim();
        return trim.isEmpty() ? null : trim;
    }
}

