package campus.ai.rag.service.service.admin.kb.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.admin.kb.DocumentParseService;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    private final Tika tika = new Tika();

    @Override
    public String parseToText(Path path, String fileExt) {
        if (path == null || !Files.exists(path)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "文档文件不存在");
        }
        String ext = fileExt == null ? "" : fileExt.trim().toLowerCase(Locale.ROOT);
        try {
            // txt/md 文本文件优先按UTF-8读取，避免被误识别为二进制类型
            if ("txt".equals(ext) || "md".equals(ext)) {
                return Files.readString(path, StandardCharsets.UTF_8).trim();
            }
            // pdf/doc/docx 统一使用 Tika 解析文本内容
            return tika.parseToString(path).trim();
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档解析失败：" + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "文档解析异常：" + e.getMessage());
        }
    }
}

