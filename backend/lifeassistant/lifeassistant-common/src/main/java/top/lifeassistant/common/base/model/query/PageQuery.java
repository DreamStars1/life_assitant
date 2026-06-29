package top.lifeassistant.common.base.model.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小为 1")
    private int page = 1;

    @Schema(description = "每页条数", example = "5")
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private int size = 5;

    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }
}
