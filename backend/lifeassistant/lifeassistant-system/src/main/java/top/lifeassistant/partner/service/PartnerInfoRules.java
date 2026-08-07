package top.lifeassistant.partner.service;

import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;

public final class PartnerInfoRules {

    private PartnerInfoRules() {}

    public static String[] orderedPair(String u1, String u2) {
        if (u1.compareTo(u2) < 0) {
            return new String[]{u1, u2};
        }
        return new String[]{u2, u1};
    }

    public static void requireNotFuture(LocalDate since, LocalDate today) {
        if (since.isAfter(today)) {
            throw new BadRequestException("在一起日期不能晚于今天");
        }
    }
}
