package top.lifeassistant.health.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.health.model.entity.HealthMealDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HealthEnergyRulesTest {

    @Test
    void validateKcal_nullOk() {
        assertDoesNotThrow(() -> HealthEnergyRules.validateKcal(null, "kcal"));
    }

    @Test
    void validateKcal_rejectsNegative() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> HealthEnergyRules.validateKcal(-1, "kcal"));
        assertTrue(ex.getMessage().contains("kcal"));
    }

    @Test
    void validateKcal_rejectsOver20000() {
        assertThrows(BusinessException.class,
            () -> HealthEnergyRules.validateKcal(20001, "burnKcal"));
    }

    @Test
    void averageDailyIntake_partialMealsAndSkipEmptyDays() {
        // day1: 150+200=350; day2: 100; day3 has meal but no kcal → not in list
        HealthMealDO a = meal(LocalDate.of(2026, 8, 1), 150);
        HealthMealDO b = meal(LocalDate.of(2026, 8, 1), 200);
        HealthMealDO c = meal(LocalDate.of(2026, 8, 2), 100);
        BigDecimal avg = HealthEnergyRules.averageDailyIntakeKcal(List.of(a, b, c));
        // (350+100)/2 = 225
        assertEquals(0, new BigDecimal("225").compareTo(avg));
    }

    @Test
    void averageDailyIntake_emptyNull() {
        assertNull(HealthEnergyRules.averageDailyIntakeKcal(List.of()));
    }

    @Test
    void effectiveBurn_prefersOverride() {
        assertEquals(1800, HealthEnergyRules.effectiveBurnKcal(1800, 1400));
        assertEquals(1400, HealthEnergyRules.effectiveBurnKcal(null, 1400));
        assertNull(HealthEnergyRules.effectiveBurnKcal(null, null));
    }

    private static HealthMealDO meal(LocalDate d, int kcal) {
        HealthMealDO m = new HealthMealDO();
        m.setMealDate(d);
        m.setKcal(kcal);
        return m;
    }
}
