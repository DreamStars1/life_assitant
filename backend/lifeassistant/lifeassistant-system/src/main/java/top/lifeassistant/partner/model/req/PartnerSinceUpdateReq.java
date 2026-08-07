package top.lifeassistant.partner.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "伴侣相识日期更新请求")
public class PartnerSinceUpdateReq {

    @NotNull
    @Schema(description = "相识日期")
    private LocalDate partnerSince;
}
