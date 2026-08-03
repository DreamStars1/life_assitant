package top.lifeassistant.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.health.mapper.HealthDailyMapper;
import top.lifeassistant.health.mapper.HealthMealMapper;
import top.lifeassistant.health.mapper.HealthMemoryMapper;
import top.lifeassistant.health.mapper.HealthProfileMapper;
import top.lifeassistant.health.mapper.HealthToleranceMapper;
import top.lifeassistant.health.mapper.HealthTriggerMapper;
import top.lifeassistant.health.mapper.HealthWeightMapper;
import top.lifeassistant.health.model.entity.HealthDailyDO;
import top.lifeassistant.health.model.entity.HealthMealDO;
import top.lifeassistant.health.model.entity.HealthMemoryDO;
import top.lifeassistant.health.model.entity.HealthProfileDO;
import top.lifeassistant.health.model.entity.HealthToleranceDO;
import top.lifeassistant.health.model.entity.HealthTriggerDO;
import top.lifeassistant.health.model.entity.HealthWeightDO;
import top.lifeassistant.health.model.query.HealthPageQuery;
import top.lifeassistant.health.model.req.HealthDailyUpsertReq;
import top.lifeassistant.health.model.req.HealthMealUpsertReq;
import top.lifeassistant.health.model.req.HealthMemoryCreateReq;
import top.lifeassistant.health.model.req.HealthMemoryPatchReq;
import top.lifeassistant.health.model.req.HealthProfileUpsertReq;
import top.lifeassistant.health.model.req.HealthToleranceCreateReq;
import top.lifeassistant.health.model.req.HealthTolerancePatchReq;
import top.lifeassistant.health.model.req.HealthTriggerCreateReq;
import top.lifeassistant.health.model.req.HealthTriggerPatchReq;
import top.lifeassistant.health.model.req.HealthWeightCreateReq;
import top.lifeassistant.health.model.resp.HealthDailyResp;
import top.lifeassistant.health.model.resp.HealthMealResp;
import top.lifeassistant.health.model.resp.HealthMemoryResp;
import top.lifeassistant.health.model.resp.HealthProfileResp;
import top.lifeassistant.health.model.resp.HealthSummaryResp;
import top.lifeassistant.health.model.resp.HealthToleranceResp;
import top.lifeassistant.health.model.resp.HealthTriggerResp;
import top.lifeassistant.health.model.resp.HealthWeightResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthService {

    private static final Set<String> MEAL_TYPES = Set.of("早餐", "午餐", "晚餐");
    private static final Set<String> WEIGHT_TYPES = Set.of("晨重", "晚重");
    private static final Set<String> TOLERANCE_LEVELS = Set.of("舒适", "低风险", "谨慎", "高风险");
    private static final int MEMORY_MAX = 100;

    private final HealthProfileMapper profileMapper;
    private final HealthMealMapper mealMapper;
    private final HealthDailyMapper dailyMapper;
    private final HealthWeightMapper weightMapper;
    private final HealthMemoryMapper memoryMapper;
    private final HealthToleranceMapper toleranceMapper;
    private final HealthTriggerMapper triggerMapper;

    public HealthProfileResp getProfile(String userId) {
        HealthProfileDO row = profileMapper.selectOne(new LambdaQueryWrapper<HealthProfileDO>()
            .eq(HealthProfileDO::getUserId, userId));
        return HealthProfileResp.from(row);
    }

    public HealthProfileResp upsertProfile(String userId, HealthProfileUpsertReq req) {
        LocalDateTime now = LocalDateTime.now();
        HealthProfileDO row = profileMapper.selectOne(new LambdaQueryWrapper<HealthProfileDO>()
            .eq(HealthProfileDO::getUserId, userId));
        if (row == null) {
            row = new HealthProfileDO();
            row.setId(UUID.randomUUID().toString());
            row.setUserId(userId);
            row.setCreatedAt(now);
            applyProfileFields(row, req, true);
            row.setUpdatedAt(now);
            profileMapper.insert(row);
        } else {
            applyProfileFields(row, req, false);
            row.setUpdatedAt(now);
            profileMapper.updateById(row);
        }
        return HealthProfileResp.from(row);
    }

    private static void applyProfileFields(HealthProfileDO row, HealthProfileUpsertReq req, boolean isNew) {
        if (isNew) {
            row.setDisplayName(req.getDisplayName());
            row.setMotto(req.getMotto());
            row.setHeightCm(req.getHeightCm());
            row.setTargetKg(req.getTargetKg());
            row.setRestingKcal(req.getRestingKcal());
            return;
        }
        if (req.getDisplayName() != null) {
            row.setDisplayName(req.getDisplayName());
        }
        if (req.getMotto() != null) {
            row.setMotto(req.getMotto());
        }
        if (req.getHeightCm() != null) {
            row.setHeightCm(req.getHeightCm());
        }
        if (req.isTargetKgPresent()) {
            row.setTargetKg(req.getTargetKg());
        }
        if (req.getRestingKcal() != null) {
            row.setRestingKcal(req.getRestingKcal());
        }
    }

    public List<HealthMealResp> listMeals(String userId, LocalDate date) {
        return mealMapper.selectList(new LambdaQueryWrapper<HealthMealDO>()
                .eq(HealthMealDO::getUserId, userId)
                .eq(HealthMealDO::getMealDate, date)
                .orderByAsc(HealthMealDO::getMealType))
            .stream()
            .map(HealthMealResp::from)
            .toList();
    }

    public HealthMealResp upsertMeal(String userId, HealthMealUpsertReq req) {
        if (!MEAL_TYPES.contains(req.getMealType())) {
            throw new BusinessException("mealType must be 早餐/午餐/晚餐");
        }
        LocalDateTime now = LocalDateTime.now();
        HealthMealDO row = mealMapper.selectOne(new LambdaQueryWrapper<HealthMealDO>()
            .eq(HealthMealDO::getUserId, userId)
            .eq(HealthMealDO::getMealDate, req.getDate())
            .eq(HealthMealDO::getMealType, req.getMealType()));
        if (row == null) {
            row = new HealthMealDO();
            row.setId(UUID.randomUUID().toString());
            row.setUserId(userId);
            row.setMealDate(req.getDate());
            row.setMealType(req.getMealType());
            row.setCreatedAt(now);
            row.setFood(req.getFood());
            row.setProteinG(req.getProteinG());
            row.setFeedback(req.getFeedback());
            row.setUpdatedAt(now);
            mealMapper.insert(row);
        } else {
            row.setFood(req.getFood());
            row.setProteinG(req.getProteinG());
            row.setFeedback(req.getFeedback());
            row.setUpdatedAt(now);
            mealMapper.updateById(row);
        }
        return HealthMealResp.from(row);
    }

    public void deleteMeal(String userId, String id) {
        HealthMealDO row = mealMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("meal not found");
        }
        mealMapper.deleteById(id);
    }

    public HealthDailyResp getDaily(String userId, LocalDate date) {
        HealthDailyDO row = dailyMapper.selectOne(new LambdaQueryWrapper<HealthDailyDO>()
            .eq(HealthDailyDO::getUserId, userId)
            .eq(HealthDailyDO::getDailyDate, date));
        return HealthDailyResp.from(row);
    }

    public HealthDailyResp upsertDaily(String userId, HealthDailyUpsertReq req) {
        LocalDateTime now = LocalDateTime.now();
        HealthDailyDO row = dailyMapper.selectOne(new LambdaQueryWrapper<HealthDailyDO>()
            .eq(HealthDailyDO::getUserId, userId)
            .eq(HealthDailyDO::getDailyDate, req.getDate()));
        boolean isNew = row == null;
        if (isNew) {
            row = new HealthDailyDO();
            row.setId(UUID.randomUUID().toString());
            row.setUserId(userId);
            row.setDailyDate(req.getDate());
            row.setCreatedAt(now);
        }
        if (isNew) {
            row.setStomachStatus(req.getStomachStatus());
            row.setStomachNote(req.getStomachNote());
            row.setCyclePhase(req.getCyclePhase());
            row.setCycleDay(req.getCycleDay());
        } else {
            if (req.getStomachStatus() != null) {
                row.setStomachStatus(req.getStomachStatus());
            }
            if (req.getStomachNote() != null) {
                row.setStomachNote(req.getStomachNote());
            }
            if (req.getCyclePhase() != null) {
                row.setCyclePhase(req.getCyclePhase());
            }
            if (req.getCycleDay() != null) {
                row.setCycleDay(req.getCycleDay());
            }
        }
        row.setUpdatedAt(now);
        if (isNew) {
            dailyMapper.insert(row);
        } else {
            dailyMapper.updateById(row);
        }
        return HealthDailyResp.from(row);
    }

    public List<HealthWeightResp> listWeights(String userId, LocalDate from, LocalDate to, String type) {
        LambdaQueryWrapper<HealthWeightDO> qw = new LambdaQueryWrapper<HealthWeightDO>()
            .eq(HealthWeightDO::getUserId, userId);
        if (from != null) {
            qw.ge(HealthWeightDO::getWeightDate, from);
        }
        if (to != null) {
            qw.le(HealthWeightDO::getWeightDate, to);
        }
        if (type != null && !type.isBlank() && !"all".equals(type)) {
            if (!WEIGHT_TYPES.contains(type)) {
                throw new BusinessException("type must be 晨重/晚重/all");
            }
            qw.eq(HealthWeightDO::getWeightType, type);
        }
        qw.orderByDesc(HealthWeightDO::getWeightDate)
            .orderByDesc(HealthWeightDO::getCreatedAt);
        return weightMapper.selectList(qw).stream().map(HealthWeightResp::from).toList();
    }

    public HealthWeightResp createWeight(String userId, HealthWeightCreateReq req) {
        if (!WEIGHT_TYPES.contains(req.getWeightType())) {
            throw new BusinessException("weightType must be 晨重/晚重");
        }
        LocalDateTime now = LocalDateTime.now();
        HealthWeightDO row = new HealthWeightDO();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setWeightDate(req.getDate());
        row.setWeightType(req.getWeightType());
        row.setKg(req.getKg());
        row.setStandard(Boolean.TRUE.equals(req.getStandard()));
        row.setCreatedAt(now);
        weightMapper.insert(row);
        return HealthWeightResp.from(row);
    }

    public void deleteWeight(String userId, String id) {
        HealthWeightDO row = weightMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("weight not found");
        }
        weightMapper.deleteById(id);
    }

    public PageResult<HealthMemoryResp> listMemories(String userId, HealthPageQuery query) {
        return pageEntities(userId, query, memoryMapper, HealthMemoryDO::getUserId,
            new LambdaQueryWrapper<HealthMemoryDO>()
                .orderByAsc(HealthMemoryDO::getSortOrder)
                .orderByAsc(HealthMemoryDO::getCreatedAt),
            HealthMemoryResp::from);
    }

    public HealthMemoryResp createMemory(String userId, HealthMemoryCreateReq req) {
        Long cnt = memoryMapper.selectCount(new LambdaQueryWrapper<HealthMemoryDO>()
            .eq(HealthMemoryDO::getUserId, userId));
        if (cnt >= MEMORY_MAX) {
            throw new BusinessException("健康规律最多 100 条");
        }
        LocalDateTime now = LocalDateTime.now();
        HealthMemoryDO row = new HealthMemoryDO();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setTitle(req.getTitle());
        row.setDetail(req.getDetail());
        row.setSortOrder(cnt.intValue());
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        memoryMapper.insert(row);
        return HealthMemoryResp.from(row);
    }

    public HealthMemoryResp patchMemory(String userId, String id, HealthMemoryPatchReq req) {
        HealthMemoryDO row = memoryMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("memory not found");
        }
        if (req.getTitle() != null) {
            row.setTitle(req.getTitle());
        }
        if (req.getDetail() != null) {
            row.setDetail(req.getDetail());
        }
        row.setUpdatedAt(LocalDateTime.now());
        memoryMapper.updateById(row);
        return HealthMemoryResp.from(row);
    }

    public void deleteMemory(String userId, String id) {
        HealthMemoryDO row = memoryMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("memory not found");
        }
        memoryMapper.deleteById(id);
    }

    public PageResult<HealthToleranceResp> listTolerances(String userId, HealthPageQuery query) {
        return pageEntities(userId, query, toleranceMapper, HealthToleranceDO::getUserId,
            new LambdaQueryWrapper<HealthToleranceDO>()
                .orderByAsc(HealthToleranceDO::getCreatedAt),
            HealthToleranceResp::from);
    }

    public HealthToleranceResp createTolerance(String userId, HealthToleranceCreateReq req) {
        validateToleranceLevel(req.getLevel());
        LocalDateTime now = LocalDateTime.now();
        HealthToleranceDO row = new HealthToleranceDO();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setName(req.getName());
        row.setLevel(req.getLevel());
        row.setCreatedAt(now);
        toleranceMapper.insert(row);
        return HealthToleranceResp.from(row);
    }

    public HealthToleranceResp patchTolerance(String userId, String id, HealthTolerancePatchReq req) {
        HealthToleranceDO row = toleranceMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("tolerance not found");
        }
        if (req.getName() != null) {
            row.setName(req.getName());
        }
        if (req.getLevel() != null) {
            validateToleranceLevel(req.getLevel());
            row.setLevel(req.getLevel());
        }
        toleranceMapper.updateById(row);
        return HealthToleranceResp.from(row);
    }

    public void deleteTolerance(String userId, String id) {
        HealthToleranceDO row = toleranceMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("tolerance not found");
        }
        toleranceMapper.deleteById(id);
    }

    public PageResult<HealthTriggerResp> listTriggers(String userId, HealthPageQuery query) {
        return pageEntities(userId, query, triggerMapper, HealthTriggerDO::getUserId,
            new LambdaQueryWrapper<HealthTriggerDO>()
                .orderByAsc(HealthTriggerDO::getCreatedAt),
            HealthTriggerResp::from);
    }

    public HealthTriggerResp createTrigger(String userId, HealthTriggerCreateReq req) {
        validateStars(req.getStars());
        LocalDateTime now = LocalDateTime.now();
        HealthTriggerDO row = new HealthTriggerDO();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setName(req.getName());
        row.setNote(req.getNote());
        row.setStars(req.getStars());
        row.setCreatedAt(now);
        triggerMapper.insert(row);
        return HealthTriggerResp.from(row);
    }

    public HealthTriggerResp patchTrigger(String userId, String id, HealthTriggerPatchReq req) {
        HealthTriggerDO row = triggerMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("trigger not found");
        }
        if (req.getName() != null) {
            row.setName(req.getName());
        }
        if (req.getNote() != null) {
            row.setNote(req.getNote());
        }
        if (req.getStars() != null) {
            validateStars(req.getStars());
            row.setStars(req.getStars());
        }
        triggerMapper.updateById(row);
        return HealthTriggerResp.from(row);
    }

    public void deleteTrigger(String userId, String id) {
        HealthTriggerDO row = triggerMapper.selectById(id);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException("trigger not found");
        }
        triggerMapper.deleteById(id);
    }

    public HealthSummaryResp getSummary(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);
        LocalDate thirtyDaysAgo = today.minusDays(29);

        HealthProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<HealthProfileDO>()
            .eq(HealthProfileDO::getUserId, userId));
        BigDecimal targetKg = profile != null ? profile.getTargetKg() : null;

        HealthWeightDO latestMorning = weightMapper.selectOne(new LambdaQueryWrapper<HealthWeightDO>()
            .eq(HealthWeightDO::getUserId, userId)
            .eq(HealthWeightDO::getWeightType, "晨重")
            .orderByDesc(HealthWeightDO::getWeightDate)
            .orderByDesc(HealthWeightDO::getCreatedAt)
            .last("LIMIT 1"));
        BigDecimal latestMorningKg = latestMorning != null ? latestMorning.getKg() : null;

        List<HealthWeightDO> standardMorning = weightMapper.selectList(new LambdaQueryWrapper<HealthWeightDO>()
            .eq(HealthWeightDO::getUserId, userId)
            .eq(HealthWeightDO::getWeightType, "晨重")
            .eq(HealthWeightDO::getStandard, true)
            .ge(HealthWeightDO::getWeightDate, sevenDaysAgo)
            .le(HealthWeightDO::getWeightDate, today));
        BigDecimal avg7MorningKg = averageDailyMorningKg(standardMorning);

        BigDecimal gapToTargetKg = null;
        if (latestMorningKg != null && targetKg != null) {
            gapToTargetKg = latestMorningKg.subtract(targetKg);
        }

        List<HealthMealDO> proteinMeals = mealMapper.selectList(new LambdaQueryWrapper<HealthMealDO>()
            .eq(HealthMealDO::getUserId, userId)
            .ge(HealthMealDO::getMealDate, thirtyDaysAgo)
            .le(HealthMealDO::getMealDate, today)
            .isNotNull(HealthMealDO::getProteinG));
        BigDecimal avg30ProteinG = averageDailyProtein(proteinMeals);

        return HealthSummaryResp.builder()
            .latestMorningKg(latestMorningKg)
            .avg7MorningKg(avg7MorningKg)
            .targetKg(targetKg)
            .gapToTargetKg(gapToTargetKg)
            .avg30ProteinG(avg30ProteinG)
            .build();
    }

    private static BigDecimal averageDailyMorningKg(List<HealthWeightDO> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Map<LocalDate, HealthWeightDO> latestPerDay = new HashMap<>();
        for (HealthWeightDO row : rows) {
            latestPerDay.merge(row.getWeightDate(), row, (a, b) -> {
                if (a.getCreatedAt() == null) {
                    return b;
                }
                if (b.getCreatedAt() == null) {
                    return a;
                }
                return a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
            });
        }
        BigDecimal sum = latestPerDay.values().stream()
            .map(HealthWeightDO::getKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(latestPerDay.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal averageDailyProtein(List<HealthMealDO> meals) {
        if (meals.isEmpty()) {
            return null;
        }
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (HealthMealDO meal : meals) {
            byDay.merge(meal.getMealDate(), meal.getProteinG(), BigDecimal::add);
        }
        BigDecimal sum = byDay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(byDay.size()), 1, RoundingMode.HALF_UP);
    }

    private static void validateToleranceLevel(String level) {
        if (!TOLERANCE_LEVELS.contains(level)) {
            throw new BusinessException("level must be 舒适/低风险/谨慎/高风险");
        }
    }

    private static void validateStars(int stars) {
        if (stars < 1 || stars > 5) {
            throw new BusinessException("stars must be 1-5");
        }
    }

    private <E, R> PageResult<R> pageEntities(
            String userId,
            HealthPageQuery query,
            com.baomidou.mybatisplus.core.mapper.BaseMapper<E> mapper,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<E, ?> userIdColumn,
            LambdaQueryWrapper<E> orderWrapper,
            java.util.function.Function<E, R> toResp) {
        Page<E> page = query.toPage();
        orderWrapper.eq(userIdColumn, userId);
        Page<E> result = mapper.selectPage(page, orderWrapper);
        Page<R> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        respPage.setRecords(result.getRecords().stream().map(toResp).toList());
        return PageResult.of(respPage);
    }
}
