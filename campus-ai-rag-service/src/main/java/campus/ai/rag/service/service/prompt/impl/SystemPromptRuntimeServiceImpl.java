package campus.ai.rag.service.service.prompt.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.prompt.AiSystemPromptEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.prompt.AiSystemPromptMapper;
import campus.ai.rag.service.service.prompt.SystemPromptRuntimeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SystemPromptRuntimeServiceImpl implements SystemPromptRuntimeService {

    private static final String SCENE_CHAT = "CHAT";
    private static final String SCENE_RAG = "RAG";
    private static final String SCENE_BOTH = "BOTH";

    private final AiSystemPromptMapper aiSystemPromptMapper;

    public SystemPromptRuntimeServiceImpl(AiSystemPromptMapper aiSystemPromptMapper) {
        this.aiSystemPromptMapper = aiSystemPromptMapper;
    }

    @Override
    public String buildPromptedMessage(String scene, String userMessage, String ragContext) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }
        String normalizedScene = normalizeScene(scene);

        String promptBlock = loadPromptBlock(normalizedScene);
        String ctx = ragContext == null ? "" : ragContext.trim();
        String q = userMessage.trim();

        StringBuilder sb = new StringBuilder();
        if (!promptBlock.isEmpty()) {
            sb.append(promptBlock).append("\n\n");
        }
        if (SCENE_RAG.equals(normalizedScene) && !ctx.isEmpty()) {
            sb.append("【资料】\n").append(ctx).append("\n\n");
        }
        sb.append("【用户问题】\n").append(q);
        return sb.toString();
    }

    private String loadPromptBlock(String scene) {
        List<AiSystemPromptEntity> list = aiSystemPromptMapper.selectList(new LambdaQueryWrapper<AiSystemPromptEntity>()
                .eq(AiSystemPromptEntity::getEnabled, 1)
                .in(AiSystemPromptEntity::getScene, SCENE_BOTH, scene)
                .orderByAsc(AiSystemPromptEntity::getPriority)
                .orderByAsc(AiSystemPromptEntity::getId));
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(AiSystemPromptEntity::getContent)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    private String normalizeScene(String scene) {
        String s = scene == null ? "" : scene.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            return SCENE_CHAT;
        }
        if (!SCENE_CHAT.equals(s) && !SCENE_RAG.equals(s)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "scene 仅支持 CHAT/RAG");
        }
        return s;
    }
}

