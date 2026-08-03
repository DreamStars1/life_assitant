package top.lifeassistant.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.health.mapper.HealthDailyMapper;
import top.lifeassistant.health.mapper.HealthMealMapper;
import top.lifeassistant.health.mapper.HealthProfileMapper;
import top.lifeassistant.health.model.entity.HealthDailyDO;
import top.lifeassistant.health.model.entity.HealthMealDO;
import top.lifeassistant.health.model.entity.HealthProfileDO;
import top.lifeassistant.health.model.req.HealthDailyUpsertReq;
import top.lifeassistant.health.model.req.HealthMealUpsertReq;
import top.lifeassistant.health.model.req.HealthProfileUpsertReq;
import top.lifeassistant.health.model.resp.HealthDailyResp;
import top.lifeassistant.health.model.resp.HealthMealResp;
import top.lifeassistant.health.model.resp.HealthProfileResp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthService {

    private static final Set<String> MEAL_TYPES = Set.of("早餐", "午餐", "晚餐");

    private final HealthProfileMapper profileMapper;
    private final HealthMealMapper mealMapper;
    private final HealthDailyMapper dailyMapper;

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
            applyProfileFields(row, req);
            row.setUpdatedAt(now);
            profileMapper.insert(row);
        } else {
            applyProfileFields(row, req);
            row.setUpdatedAt(now);
            profileMapper.updateById(row);
        }
        return HealthProfileResp.from(row);
    }

    private static void applyProfileFields(HealthProfileDO row, HealthProfileUpsertReq req) {
        row.setDisplayName(req.getDisplayName());
        row.setMotto(req.getMotto());
        row.setHeightCm(req.getHeightCm());
        row.setTargetKg(req.getTargetKg());
        row.setRestingKcal(req.getRestingKcal());
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
        row.setStomachStatus(req.getStomachStatus());
        row.setStomachNote(req.getStomachNote());
        row.setCyclePhase(req.getCyclePhase());
        row.setCycleDay(req.getCycleDay());
        row.setUpdatedAt(now);
        if (isNew) {
            dailyMapper.insert(row);
        } else {
            dailyMapper.updateById(row);
        }
        return HealthDailyResp.from(row);
    }
}
