package top.lifeassistant.partner.service;

import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PartnerPointsRules {

    private PartnerPointsRules() {}

    public static LocalDateTime resolveCreatedAt(LocalDate recordDate, LocalDate today, LocalDateTime now) {
        if (recordDate == null || recordDate.equals(today)) {
            return now;
        }
        if (recordDate.isAfter(today)) {
            throw new BadRequestException("记录日期不能晚于今天");
        }
        return recordDate.atStartOfDay();
    }
}
