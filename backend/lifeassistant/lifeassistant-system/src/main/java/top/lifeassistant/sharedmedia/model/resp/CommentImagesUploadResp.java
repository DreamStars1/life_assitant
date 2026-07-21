package top.lifeassistant.sharedmedia.model.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommentImagesUploadResp {
    private List<String> urls;
}
