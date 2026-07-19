package top.lifeassistant.schedule.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "日程邀约确认请求")
public class ScheduleInviteAckReq {

    @Schema(description = "操作：accept/reject")
    @NotBlank
    @Pattern(regexp = "accept|reject")
    private String action;
}
