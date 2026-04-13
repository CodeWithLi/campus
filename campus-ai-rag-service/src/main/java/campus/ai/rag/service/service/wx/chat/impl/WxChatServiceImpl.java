package campus.ai.rag.service.service.wx.chat.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxChatDTO;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.wx.chat.WxChatService;
import campus.ai.rag.service.vo.wx.WxChatVO;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class WxChatServiceImpl implements WxChatService {

    private final AiChatRouter aiChatRouter;

    public WxChatServiceImpl(AiChatRouter aiChatRouter) {
        this.aiChatRouter = aiChatRouter;
    }

    @Override
    public WxChatVO chat(WxChatDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getMessage())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }

        String finalModelCode = isBlank(dto.getModelCode()) ? aiChatRouter.getDefaultModelCode() : dto.getModelCode().trim();
        String answer = aiChatRouter.chat(finalModelCode, dto.getMessage().trim());

        WxChatVO vo = new WxChatVO();
        vo.setModelCode(finalModelCode);
        vo.setAnswer(answer);
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

