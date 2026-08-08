package top.lifeassistant.partner.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PartnerPointsRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 8);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 14, 30, 0);

    @Test
    void nullDate_returnsNow() {
        assertEquals(NOW, PartnerPointsRules.resolveCreatedAt(null, TODAY, NOW));
    }

    @Test
    void today_returnsNow() {
        assertEquals(NOW, PartnerPointsRules.resolveCreatedAt(TODAY, TODAY, NOW));
    }

    @Test
    void pastDate_returnsStartOfDay() {
        LocalDate past = LocalDate.of(2026, 8, 5);
        assertEquals(past.atStartOfDay(), PartnerPointsRules.resolveCreatedAt(past, TODAY, NOW));
    }

    @Test
    void futureDate_throws() {
        assertThrows(BadRequestException.class,
            () -> PartnerPointsRules.resolveCreatedAt(TODAY.plusDays(1), TODAY, NOW));
    }
}
