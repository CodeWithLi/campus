package campus.ai.rag.service.service.wx.post;

import campus.ai.rag.service.dto.wx.WxPostAiGenerateDTO;
import campus.ai.rag.service.dto.wx.WxPostCommentCreateDTO;
import campus.ai.rag.service.dto.wx.WxPostPublishDTO;
import campus.ai.rag.service.vo.wx.WxPostAiGenerateVO;
import campus.ai.rag.service.vo.wx.WxPostCommentVO;
import campus.ai.rag.service.vo.wx.WxPostDetailVO;
import campus.ai.rag.service.vo.wx.WxPostFavoriteVO;
import campus.ai.rag.service.vo.wx.WxPostInteractionPageVO;
import campus.ai.rag.service.vo.wx.WxPostLikeVO;
import campus.ai.rag.service.vo.wx.WxPostPageVO;
import campus.ai.rag.service.vo.wx.WxPostPublishVO;
import campus.ai.rag.service.vo.wx.WxPostUploadImageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WxPostService {

    WxPostAiGenerateVO aiGenerate(WxPostAiGenerateDTO dto);

    WxPostUploadImageVO uploadImages(MultipartFile[] files);

    WxPostPublishVO publish(WxPostPublishDTO dto);

    WxPostPageVO list(Integer pageNo, Integer pageSize);

    WxPostDetailVO detail(Long postId);

    WxPostPageVO myPosts(Integer pageNo, Integer pageSize);

    WxPostLikeVO toggleLike(Long postId);

    WxPostFavoriteVO toggleFavorite(Long postId);

    WxPostCommentVO createComment(Long postId, WxPostCommentCreateDTO dto);

    List<WxPostCommentVO> listComments(Long postId);

    Boolean deleteMyPost(Long postId);

    WxPostPageVO myFavorites(Integer pageNo, Integer pageSize);

    WxPostInteractionPageVO myInteractions(Integer pageNo, Integer pageSize);
}
