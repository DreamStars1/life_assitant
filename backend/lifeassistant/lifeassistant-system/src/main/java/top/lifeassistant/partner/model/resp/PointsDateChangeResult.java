package top.lifeassistant.partner.model.resp;

import lombok.Data;

@Data
public class PointsDateChangeResult {
    private String status; // APPLIED | PENDING

    public static PointsDateChangeResult applied() {
        PointsDateChangeResult r = new PointsDateChangeResult();
        r.status = "APPLIED";
        return r;
    }

    public static PointsDateChangeResult pending() {
        PointsDateChangeResult r = new PointsDateChangeResult();
        r.status = "PENDING";
        return r;
    }
}
