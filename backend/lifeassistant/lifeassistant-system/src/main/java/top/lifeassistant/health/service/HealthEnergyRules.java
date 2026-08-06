package top.lifeassistant.health.service;

import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.health.model.entity.HealthMealDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HealthEnergyRules {

    public static final int MAX_KCAL = 20000;

    private HealthEnergyRules() {}

    public static void validateKcal(Integer kcal, String fieldLabel) {
        if (kcal == null) {
            return;
        }
        if (kcal < 0 || kcal > MAX_KCAL) {
            throw new BusinessException(fieldLabel + " must be 0.." + MAX_KCAL);
        }
    }

    /** 入参应为已带非空 kcal 的餐次；按日加总后对天数求平均。 */
    public static BigDecimal averageDailyIntakeKcal(List<HealthMealDO> mealsWithKcal) {
        if (mealsWithKcal == null || mealsWithKcal.isEmpty()) {
            return null;
        }
        Map<LocalDate, Integer> byDay = new HashMap<>();
        for (HealthMealDO meal : mealsWithKcal) {
            byDay.merge(meal.getMealDate(), meal.getKcal(), Integer::sum);
        }
        int sum = byDay.values().stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(byDay.size()), 0, RoundingMode.HALF_UP);
    }

    public static Integer effectiveBurnKcal(Integer burnKcal, Integer restingKcal) {
        return burnKcal != null ? burnKcal : restingKcal;
    }
}
