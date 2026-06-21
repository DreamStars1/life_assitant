package top.lifeassistant.system.model.entity.user;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;
import java.io.Serial;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class UserDO extends BaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private String email;
    private String password;
    private String fullName;
    private Boolean isActive;
    private Boolean isSuperuser;
    private String partnerId;
    private String timezone;
    private Boolean pushEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
}
