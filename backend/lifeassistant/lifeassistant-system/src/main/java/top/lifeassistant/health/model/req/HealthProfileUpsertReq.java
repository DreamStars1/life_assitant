package top.lifeassistant.health.model.req;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.IOException;
import java.math.BigDecimal;

@Data
@Schema(description = "健康档案 upsert 请求")
@JsonDeserialize(using = HealthProfileUpsertReq.Deserializer.class)
public class HealthProfileUpsertReq {

    @Schema(description = "展示名")
    private String displayName;

    @Schema(description = "鼓励文案")
    private String motto;

    @Schema(description = "身高 cm")
    private BigDecimal heightCm;

    @Schema(description = "目标体重 kg，可空")
    private BigDecimal targetKg;

    @Schema(description = "静息代谢 kcal")
    private Integer restingKcal;

    /** JSON 是否包含 targetKg 键（区分未传 vs 显式清空）。 */
    private boolean targetKgPresent;

    static class Deserializer extends JsonDeserializer<HealthProfileUpsertReq> {
        @Override
        public HealthProfileUpsertReq deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            HealthProfileUpsertReq req = new HealthProfileUpsertReq();
            req.displayName = textOrNull(node.get("displayName"));
            req.motto = textOrNull(node.get("motto"));
            req.heightCm = decimalOrNull(node.get("heightCm"));
            req.restingKcal = intOrNull(node.get("restingKcal"));
            req.targetKgPresent = node.has("targetKg");
            if (req.targetKgPresent) {
                req.targetKg = decimalOrNull(node.get("targetKg"));
            }
            return req;
        }

        private static String textOrNull(JsonNode n) {
            return n == null || n.isNull() ? null : n.asText();
        }

        private static BigDecimal decimalOrNull(JsonNode n) {
            return n == null || n.isNull() ? null : n.decimalValue();
        }

        private static Integer intOrNull(JsonNode n) {
            return n == null || n.isNull() ? null : n.asInt();
        }
    }
}
