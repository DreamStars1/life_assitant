package top.lifeassistant.sharedmedia.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "共享媒体分页查询参数")
public class SharedMediaPageQuery extends PageQuery {
    @Schema(description = "媒体类型: movie/book/tv，不传=全部")
    private String mediaType;

    @Schema(description = "状态: finished=已看完, unfinished=没看完, 不传=全部")
    private String status;
}
