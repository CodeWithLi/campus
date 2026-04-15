package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "KbDocumentImagePreviewVO", description = "文档图片理解预览结果")
public class KbDocumentImagePreviewVO {

    @ApiModelProperty(value = "文档ID", example = "1")
    private Long documentId;

    @ApiModelProperty(value = "识别图片数量", example = "3")
    private Integer imageCount;

    @ApiModelProperty(value = "汇总文本（可直接回显）")
    private String mergedText;

    @ApiModelProperty(value = "逐图理解结果")
    private List<Item> items;

    @Data
    @ApiModel(value = "KbDocumentImagePreviewVO.Item", description = "单张图片理解结果")
    public static class Item {
        @ApiModelProperty(value = "图片序号（从1开始）", example = "1")
        private Integer imageIndex;

        @ApiModelProperty(value = "图片后缀", example = "png")
        private String imageExt;

        @ApiModelProperty(value = "图片本地存储路径（绝对路径，便于排查/回显）")
        private String imagePath;

        @ApiModelProperty(value = "理解结果文本")
        private String text;
    }
}

