package top.lifeassistant.health.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "健康分页查询参数")
public class HealthPageQuery extends PageQuery {

    @Schema(description = "每页条数（query 别名 pageSize）")
    public Integer getPageSize() {
        return getSize();
    }

    public void setPageSize(int pageSize) {
        setSize(pageSize);
    }
}
