package top.lifeassistant.partner.model.resp;

import lombok.Data;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;

import java.util.List;

@Data
public class PartnerPointsHistoryResp {
    private List<PartnerPointsDO> records;
    private long total;
    private long size;
    private long current;
    private long pages;
    private int pendingConfirmCount;
}
