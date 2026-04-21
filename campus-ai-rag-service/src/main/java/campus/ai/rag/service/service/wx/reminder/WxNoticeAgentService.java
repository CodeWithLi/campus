package campus.ai.rag.service.service.wx.reminder;

import campus.ai.rag.service.dto.wx.WxNoticeAgentCreateDTO;
import campus.ai.rag.service.vo.wx.WxNoticeAgentParseVO;
import campus.ai.rag.service.vo.wx.WxNoticeAgentRecordVO;
import campus.ai.rag.service.vo.wx.WxScheduleReminderVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WxNoticeAgentService {

    WxNoticeAgentParseVO parse(MultipartFile file);

    WxNoticeAgentParseVO parseByImageUnderstand(MultipartFile file);

    WxNoticeAgentParseVO parseByImageModel(MultipartFile file);

    List<WxScheduleReminderVO> createBatch(WxNoticeAgentCreateDTO dto);

    List<WxNoticeAgentRecordVO> listRecords();

    void deleteRecord(Long recordId);
}
