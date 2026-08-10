package top.lifeassistant.partner.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;

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
        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> PartnerPointsRules.resolveCreatedAt(TODAY.plusDays(1), TODAY, NOW));
        assertEquals("记录日期不能晚于今天", ex.getMessage());
    }

    @Test
    void isCcSkipConfirm_exactMatchOnly() {
        assertTrue(PartnerPointsRules.isCcSkipConfirm("cc"));
        assertFalse(PartnerPointsRules.isCcSkipConfirm("CC"));
        assertFalse(PartnerPointsRules.isCcSkipConfirm("cc "));
        assertFalse(PartnerPointsRules.isCcSkipConfirm(null));
        assertFalse(PartnerPointsRules.isCcSkipConfirm("小星露"));
    }

    @Test
    void isPendingExpired_nullOrOver24h() {
        LocalDateTime t0 = LocalDateTime.of(2026, 8, 10, 12, 0, 0);
        assertTrue(PartnerPointsRules.isPendingExpired(null, t0));
        assertFalse(PartnerPointsRules.isPendingExpired(t0, t0.plusHours(23)));
        assertTrue(PartnerPointsRules.isPendingExpired(t0, t0.plusHours(24)));
        assertTrue(PartnerPointsRules.isPendingExpired(t0, t0.plusHours(25)));
    }

    @Test
    void clearPendingFields_nullsAllThree() {
        PartnerPointsDO r = new PartnerPointsDO();
        r.setPendingRecordDate(LocalDate.of(2026, 8, 1));
        r.setPendingRequestedBy("u1");
        r.setPendingRequestedAt(LocalDateTime.now());
        PartnerPointsRules.clearPendingFields(r);
        assertNull(r.getPendingRecordDate());
        assertNull(r.getPendingRequestedBy());
        assertNull(r.getPendingRequestedAt());
    }
}
