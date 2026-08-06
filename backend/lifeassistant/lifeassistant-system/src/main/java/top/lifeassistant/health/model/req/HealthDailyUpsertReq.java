package top.lifeassistant.health.model.req;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.IOException;
import java.time.LocalDate;

@Data
@Schema(description = "每日状态 upsert 请求")
@JsonDeserialize(using = HealthDailyUpsertReq.Deserializer.class)
public class HealthDailyUpsertReq {

    @NotNull
    @Schema(description = "日期 yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "胃状态")
    private String stomachStatus;

    @Schema(description = "胃备注")
    private String stomachNote;

    @Schema(description = "周期阶段")
    private String cyclePhase;

    @Schema(description = "周期第几天")
    private Integer cycleDay;

    @Schema(description = "当日消耗 kcal，可空")
    private Integer burnKcal;

    /** JSON 是否包含 burnKcal 键（区分未传 vs 显式清空）。 */
    private boolean burnKcalPresent;

    static class Deserializer extends JsonDeserializer<HealthDailyUpsertReq> {
        @Override
        public HealthDailyUpsertReq deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            HealthDailyUpsertReq req = new HealthDailyUpsertReq();
            JsonNode dateNode = node.get("date");
            req.date = dateNode == null || dateNode.isNull() ? null : LocalDate.parse(dateNode.asText());
            req.stomachStatus = textOrNull(node.get("stomachStatus"));
            req.stomachNote = textOrNull(node.get("stomachNote"));
            req.cyclePhase = textOrNull(node.get("cyclePhase"));
            req.cycleDay = intOrNull(node.get("cycleDay"));
            req.burnKcalPresent = node.has("burnKcal");
            if (req.burnKcalPresent) {
                req.burnKcal = intOrNull(node.get("burnKcal"));
            }
            return req;
        }

        private static String textOrNull(JsonNode n) {
            return n == null || n.isNull() ? null : n.asText();
        }

        private static Integer intOrNull(JsonNode n) {
            return n == null || n.isNull() ? null : n.asInt();
        }
    }
}
