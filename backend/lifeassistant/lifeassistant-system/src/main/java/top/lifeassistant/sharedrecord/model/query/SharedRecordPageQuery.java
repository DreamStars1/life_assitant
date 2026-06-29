package top.lifeassistant.sharedrecord.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "共享记录分页查询参数")
public class SharedRecordPageQuery extends PageQuery {

    @Schema(description = "搜索关键字（匹配标题或内容）")
    private String keyword;

    @Schema(description = "发生时间范围（开始）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;

    @Schema(description = "发生时间范围（结束）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime end;
}
