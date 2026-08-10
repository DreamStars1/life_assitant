package top.lifeassistant.partner.service;

import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PartnerPointsRules {

    public static final int PENDING_TTL_HOURS = 24;

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

    public static boolean isCcSkipConfirm(String fullName) {
        return "cc".equals(fullName);
    }

    public static boolean isPendingExpired(LocalDateTime requestedAt, LocalDateTime now) {
        if (requestedAt == null) {
            return true;
        }
        return !requestedAt.plusHours(PENDING_TTL_HOURS).isAfter(now);
    }

    public static void clearPendingFields(PartnerPointsDO record) {
        record.setPendingRecordDate(null);
        record.setPendingRequestedBy(null);
        record.setPendingRequestedAt(null);
    }
}
