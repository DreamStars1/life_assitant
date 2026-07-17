# 伴侣看板实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现伴侣看板页面，包含积分（加减+历史）、作息打卡（起床/睡觉+折线图）和数据展示功能

**Architecture:** 后端新增 2 张表、2 个 Controller/Service 模块；前端新增"看板"TabBar + 3 个页面（看板主页/积分详情/作息详情）

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant + ECharts

---

### Task 1: 后端 — Flyway 迁移文件

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V8__create_partner_points.sql`
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V9__create_partner_checkin.sql`

- [ ] **Step 1: 创建 V8 迁移文件**

```sql
CREATE TABLE partner_points (
    id CHAR(36) PRIMARY KEY,
    created_by CHAR(36) NOT NULL,
    points_change INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: 创建 V9 迁移文件**

```sql
CREATE TABLE partner_checkin (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    checkin_type VARCHAR(10) NOT NULL COMMENT 'wake/sleep',
    checkin_time DATETIME NOT NULL,
    checkin_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date_type (user_id, checkin_date, checkin_type)
);
```

### Task 2: 后端 — 实体 DO + Mapper

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/model/entity/partner/PartnerPointsDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/model/entity/partner/PartnerCheckinDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/mapper/partner/PartnerPointsMapper.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/mapper/partner/PartnerCheckinMapper.java`

- [ ] **Step 1: 创建 PartnerPointsDO**

```java
package top.lifeassistant.system.model.entity.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PartnerPointsDO {
    private String id;
    @JsonProperty("created_by")
    private String createdBy;
    @JsonProperty("points_change")
    private Integer pointsChange;
    private String reason;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 PartnerCheckinDO**

```java
package top.lifeassistant.system.model.entity.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PartnerCheckinDO {
    private String id;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("checkin_type")
    private String checkinType;
    @JsonProperty("checkin_time")
    private LocalDateTime checkinTime;
    @JsonProperty("checkin_date")
    private LocalDate checkinDate;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 PartnerPointsMapper**

```java
package top.lifeassistant.system.mapper.partner;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lifeassistant.system.model.entity.partner.PartnerPointsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PartnerPointsMapper extends BaseMapper<PartnerPointsDO> {
    @Select("SELECT COALESCE(SUM(points_change), 0) FROM partner_points WHERE created_by IN (#{userId}, #{partnerId})")
    Integer sumPoints(@Param("userId") String userId, @Param("partnerId") String partnerId);
}
```

- [ ] **Step 4: 创建 PartnerCheckinMapper**

```java
package top.lifeassistant.system.mapper.partner;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lifeassistant.system.model.entity.partner.PartnerCheckinDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PartnerCheckinMapper extends BaseMapper<PartnerCheckinDO> {

    @Select("SELECT * FROM partner_checkin WHERE user_id IN (#{userId}, #{partnerId}) AND checkin_date = #{date}")
    List<PartnerCheckinDO> findToday(@Param("userId") String userId, @Param("partnerId") String partnerId, @Param("date") LocalDate date);

    @Select("SELECT * FROM partner_checkin WHERE user_id IN (#{userId}, #{partnerId}) AND checkin_date >= #{startDate} ORDER BY checkin_date, checkin_time")
    List<PartnerCheckinDO> findWeekly(@Param("userId") String userId, @Param("partnerId") String partnerId, @Param("startDate") LocalDate startDate);
}
```

### Task 3: 后端 — Service

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/service/partner/PartnerPointsService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/service/partner/PartnerPointsServiceImpl.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/service/partner/PartnerCheckinService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/service/partner/PartnerCheckinServiceImpl.java`

- [ ] **Step 1: 创建 PartnerPointsService 接口**

```java
package top.lifeassistant.system.service.partner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.lifeassistant.system.model.entity.partner.PartnerPointsDO;

public interface PartnerPointsService {
    Integer getBalance();
    Page<PartnerPointsDO> getHistory(int page, int size);
    void addPoints(int pointsChange, String reason);
}
```

- [ ] **Step 2: 创建 PartnerPointsServiceImpl**

```java
package top.lifeassistant.system.service.partner;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lifeassistant.system.mapper.partner.PartnerPointsMapper;
import top.lifeassistant.system.model.entity.partner.PartnerPointsDO;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.user.UserService;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PartnerPointsServiceImpl implements PartnerPointsService {
    private final PartnerPointsMapper mapper;
    private final UserService userService;

    private String getUserId() {
        return StpUtil.getLoginIdAsString();
    }

    private String getPartnerId() {
        UserDO user = userService.getById(getUserId());
        if (user == null || user.getPartnerId() == null)
            throw new RuntimeException("请先绑定伴侣");
        return user.getPartnerId();
    }

    @Override
    public Integer getBalance() {
        String uid = getUserId();
        String pid = getPartnerId();
        Integer sum = mapper.sumPoints(uid, pid);
        return sum != null ? sum : 0;
    }

    @Override
    public Page<PartnerPointsDO> getHistory(int page, int size) {
        String uid = getUserId();
        String pid = getPartnerId();
        LambdaQueryWrapper<PartnerPointsDO> wrapper = new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, uid, pid)
            .orderByDesc(PartnerPointsDO::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void addPoints(int pointsChange, String reason) {
        PartnerPointsDO record = new PartnerPointsDO();
        record.setId(UUID.randomUUID().toString());
        record.setCreatedBy(getUserId());
        record.setPointsChange(pointsChange);
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        mapper.insert(record);
    }
}
```

- [ ] **Step 3: 创建 PartnerCheckinService 接口**

```java
package top.lifeassistant.system.service.partner;

import top.lifeassistant.system.model.entity.partner.PartnerCheckinDO;
import java.util.List;

public interface PartnerCheckinService {
    void checkin(String checkinType);
    List<PartnerCheckinDO> getToday();
    List<PartnerCheckinDO> getWeekly();
}
```

- [ ] **Step 4: 创建 PartnerCheckinServiceImpl**

```java
package top.lifeassistant.system.service.partner;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lifeassistant.system.mapper.partner.PartnerCheckinMapper;
import top.lifeassistant.system.model.entity.partner.PartnerCheckinDO;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.user.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerCheckinServiceImpl implements PartnerCheckinService {
    private final PartnerCheckinMapper mapper;
    private final UserService userService;

    private String getUserId() {
        return StpUtil.getLoginIdAsString();
    }

    private String getPartnerId() {
        UserDO user = userService.getById(getUserId());
        if (user == null || user.getPartnerId() == null)
            throw new RuntimeException("请先绑定伴侣");
        return user.getPartnerId();
    }

    @Override
    public void checkin(String checkinType) {
        String uid = getUserId();
        LocalDate today = LocalDate.now();

        // 检查今天是否已打此卡
        Long count = mapper.selectCount(new LambdaQueryWrapper<PartnerCheckinDO>()
            .eq(PartnerCheckinDO::getUserId, uid)
            .eq(PartnerCheckinDO::getCheckinDate, today)
            .eq(PartnerCheckinDO::getCheckinType, checkinType));
        if (count > 0)
            throw new RuntimeException("今天已经打过" + ("wake".equals(checkinType) ? "起床" : "睡觉") + "卡了");

        PartnerCheckinDO record = new PartnerCheckinDO();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(uid);
        record.setCheckinType(checkinType);
        record.setCheckinTime(LocalDateTime.now());
        record.setCheckinDate(today);
        record.setCreatedAt(LocalDateTime.now());
        mapper.insert(record);
    }

    @Override
    public List<PartnerCheckinDO> getToday() {
        String uid = getUserId();
        String pid = getPartnerId();
        return mapper.findToday(uid, pid, LocalDate.now());
    }

    @Override
    public List<PartnerCheckinDO> getWeekly() {
        String uid = getUserId();
        String pid = getPartnerId();
        return mapper.findWeekly(uid, pid, LocalDate.now().minusDays(6));
    }
}
```

### Task 4: 后端 — Controller

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/controller/partner/PartnerPointsController.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/system/controller/partner/PartnerCheckinController.java`

- [ ] **Step 1: 创建 PartnerPointsController**

```java
package top.lifeassistant.system.controller.partner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.system.model.entity.partner.PartnerPointsDO;
import top.lifeassistant.system.service.partner.PartnerPointsService;
import top.lifeassistant.common.model.ApiResponse;

@RestController
@RequestMapping("/partner/points")
@RequiredArgsConstructor
public class PartnerPointsController {
    private final PartnerPointsService service;

    @GetMapping
    public ApiResponse<Integer> getBalance() {
        return ApiResponse.ok(service.getBalance());
    }

    @GetMapping("/history")
    public ApiResponse<Page<PartnerPointsDO>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getHistory(page, size));
    }

    @PostMapping
    public ApiResponse<Void> addPoints(@Valid @RequestBody PointsChangeRequest req) {
        service.addPoints(req.getPointsChange(), req.getReason());
        return ApiResponse.ok();
    }

    @Data
    public static class PointsChangeRequest {
        @NotNull
        private Integer pointsChange;
        @NotBlank
        private String reason;
    }
}
```

- [ ] **Step 2: 创建 PartnerCheckinController**

```java
package top.lifeassistant.system.controller.partner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.system.model.entity.partner.PartnerCheckinDO;
import top.lifeassistant.system.service.partner.PartnerCheckinService;
import top.lifeassistant.common.model.ApiResponse;
import java.util.List;

@RestController
@RequestMapping("/partner/checkin")
@RequiredArgsConstructor
public class PartnerCheckinController {
    private final PartnerCheckinService service;

    @PostMapping
    public ApiResponse<Void> checkin(@Valid @RequestBody CheckinRequest req) {
        service.checkin(req.getCheckinType());
        return ApiResponse.ok();
    }

    @GetMapping("/today")
    public ApiResponse<List<PartnerCheckinDO>> getToday() {
        return ApiResponse.ok(service.getToday());
    }

    @GetMapping("/weekly")
    public ApiResponse<List<PartnerCheckinDO>> getWeekly() {
        return ApiResponse.ok(service.getWeekly());
    }

    @Data
    public static class CheckinRequest {
        @NotBlank
        private String checkinType;
    }
}
```

### Task 5: 前端 — API 模块

**Files:**
- Create: `front/vue3-vant-mobile/src/api/modules/partner-points.ts`
- Create: `front/vue3-vant-mobile/src/api/modules/partner-checkin.ts`

- [ ] **Step 1: 创建 partner-points.ts**

```typescript
import request from '@/utils/request'

export interface PointsRecord {
  id: string
  createdBy: string
  pointsChange: number
  reason: string
  createdAt: string
}

export function getPointsBalance() {
  return request.get<number>('/partner/points')
}

export function getPointsHistory(page = 0, size = 20) {
  return request.get<{ records: PointsRecord[]; total: number }>('/partner/points/history', { params: { page, size } })
}

export function addPoints(pointsChange: number, reason: string) {
  return request.post('/partner/points', { pointsChange, reason })
}
```

- [ ] **Step 2: 创建 partner-checkin.ts**

```typescript
import request from '@/utils/request'

export interface CheckinRecord {
  id: string
  userId: string
  checkinType: 'wake' | 'sleep'
  checkinTime: string
  checkinDate: string
}

export function doCheckin(checkinType: 'wake' | 'sleep') {
  return request.post('/partner/checkin', { checkinType })
}

export function getTodayCheckin() {
  return request.get<CheckinRecord[]>('/partner/checkin/today')
}

export function getWeeklyCheckin() {
  return request.get<CheckinRecord[]>('/partner/checkin/weekly')
}
```

### Task 6: 前端 — 看板主页

**Files:**
- Create: `front/vue3-vant-mobile/src/pages/partner/dashboard/index.vue`

- [ ] **Step 1: 创建看板主页（卡片网格布局 — Variant A 风格）**

- [ ] **Step 2: 添加路由声明 `<route>`**

### Task 7: 前端 — 积分详情页

**Files:**
- Create: `front/vue3-vant-mobile/src/pages/partner/dashboard/points.vue`

- [ ] **Step 1: 创建积分详情页（余额 + stepper 自定义分数 + 原因输入 + 完整历史列表）**

- [ ] **Step 2: 添加路由声明 `<route>`**

### Task 8: 前端 — 作息详情页

**Files:**
- Create: `front/vue3-vant-mobile/src/pages/partner/dashboard/sleep.vue`

- [ ] **Step 1: 创建作息详情页（打卡按钮 + 7 天 ECharts 折线图）**

- [ ] **Step 2: 添加路由声明 `<route>`**

### Task 9: 前端 — 导航配置

**Files:**
- Modify: `front/vue3-vant-mobile/src/components/TabBar.vue`
- Modify: `front/vue3-vant-mobile/src/config/routes.ts`
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json`
- Modify: `front/vue3-vant-mobile/src/locales/en-US.json`
- Modify: `front/vue3-vant-mobile/src/App.vue` (如果原型相关配置需要清理)

- [ ] **Step 1: 更新 TabBar.vue** — 在"伴侣"和"我的"之间添加看板 Tab
- [ ] **Step 2: 更新 config/routes.ts** — 添加 PartnerDashboard
- [ ] **Step 3: 更新 locales/zh-CN.json** — 添加看板相关键
- [ ] **Step 4: 更新 locales/en-US.json** — 添加看板相关键

### Task 10: 提交

- [ ] **Step 1: Stage + Commit**
