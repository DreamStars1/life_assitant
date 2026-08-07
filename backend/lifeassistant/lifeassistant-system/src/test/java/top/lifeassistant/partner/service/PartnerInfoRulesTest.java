package top.lifeassistant.partner.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PartnerInfoRulesTest {

    @Test
    void orderedPair_sortsLexicographically() {
        String[] p = PartnerInfoRules.orderedPair("b-uuid", "a-uuid");
        assertEquals("a-uuid", p[0]);
        assertEquals("b-uuid", p[1]);
        String[] same = PartnerInfoRules.orderedPair("a-uuid", "b-uuid");
        assertArrayEquals(p, same);
    }

    @Test
    void requireNotFuture_rejectsTomorrow() {
        LocalDate today = LocalDate.of(2026, 8, 7);
        assertThrows(BadRequestException.class,
            () -> PartnerInfoRules.requireNotFuture(today.plusDays(1), today));
    }

    @Test
    void requireNotFuture_allowsToday() {
        LocalDate today = LocalDate.of(2026, 8, 7);
        assertDoesNotThrow(() -> PartnerInfoRules.requireNotFuture(today, today));
    }
}
