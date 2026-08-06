-- V17__health_meal_kcal_and_daily_burn.sql
ALTER TABLE health_meal
    ADD COLUMN kcal INT NULL COMMENT '该餐热量 kcal' AFTER protein_g;

ALTER TABLE health_daily
    ADD COLUMN burn_kcal INT NULL COMMENT '当日消耗覆盖；NULL=回退静息' AFTER cycle_day;
