# 悬浮柴犬彩蛋 — 剩余素材生成提示词

> 目标：补齐 `ASSET_CHECKLIST.txt` 中缺失的 **34 帧动画**（+ 可选 3 个 FX）。
> 风格锁：必须以现有 `D:\life_assistant\asset\dog-idle-happy.png` 为角色参考（同角色、同线宽、同配色、同朝向）。
> 设计规范：`docs/superpowers/specs/2026-08-08-floating-shiba-pet-design-guide.md`
> 日期：2026-08-08

---

## 使用方法

1. 把 **§0 全局锁** 整段贴到每次生成最前面。
2. 若工具支持图生图 / 角色参考：挂上 `dog-idle-happy.png`；做磕头序列时再挂 `dog-greet-bow-keyfra.png`；做爬行时挂 `dog-react-crawl-keyf.png`（仅锁角色，勿照抄错误站姿）。
3. 优先一次出「分镜条/精灵表」再裁切；不行则按帧号分次生成，并写明「第 N 帧」。
4. 输出：**2048×2048 PNG**，透明底（或后期去白底），命名见各节。

---

## §0 全局锁（每次必贴）

```text
【角色锁定】同一只 Q 版柴犬幼犬，必须与参考图完全一致：暖橙棕主毛、奶油色口鼻胸腹与眉上圆形奶油斑、尖耳浅粉内耳、小黑鼻、深棕粗描边、柴犬卷尾、大头短腿、四分之三偏右朝向。贴纸/表情包扁平风，轻阴影。

【画布】2048x2048，角色居中约占 70%，四周留白，透明背景（不要场景、不要文字水印、不要UI框）。

【情绪底线】可爱讨好；可委屈可怜；禁止生气、龇牙、怒眉、血腥写实伤痕。

【动画纪律】多帧时锁机位与角色比例，胸口锚点几乎不动；只允许尾巴/头/腿做规定动作；不要换脸换配色。
```

英文备用（部分模型更稳）：

```text
Same chibi Shiba Inu as the reference: warm ginger fur, cream urajiro muzzle/chest, cream round eyebrow spots, pointed ears, black nose, thick dark outlines, curled shiba tail, big head short legs, three-quarter view facing slightly right. Flat sticker style, soft shading. 2048x2048, character ~70% frame, transparent background, no text. Cute people-pleasing mood only; never angry. Multi-frame: locked camera and proportions; only intended body parts move.
```

---

## §1 闲置摇尾 `dog-idle-wag`（缺 8 帧）

**交付文件名：** `dog-idle-wag-01.png` … `dog-idle-wag-08.png`

### 总提示词

```text
{粘贴 §0}

任务：生成 8 帧可无缝循环的「坐姿摇尾巴讨好」动画关键帧（或一张 8 格分镜条，从左到右/从上到下按时间排序）。

动作脚本：
01 坐姿微笑，卷尾偏左
02-03 尾巴甩向右，身体几乎不动，眼神期待
04-05 尾巴甩向左，耳朵微颤
06-07 再甩向右
08 尾巴回中，接得上第 01 帧循环

要求：表情接近开心待机；主运动是尾巴；不要站起来、不要跑、不要换表情大变。
参考：dog-idle-happy.png（坐姿与角色）。
```

### 若只能单帧生成（逐条）

把 `{N}` 换成 01–08，并写清该帧尾巴位置：

```text
{粘贴 §0}
单帧动画中间帧。角色同 dog-idle-happy 坐姿。第 {N}/8 帧：柴犬讨好摇尾，尾巴位于【左/中/右——按脚本】，身体几乎不动，微笑期待。2048x2048 透明底。文件名意图：dog-idle-wag-{N}.png
```

---

## §2 抚摸后狗爬 `dog-react-crawl`（缺 8 帧）

**交付文件名：** `dog-react-crawl-01.png` … `dog-react-crawl-08.png`
**注意：** 现有 `dog-react-crawl-keyf.png` 偏站立开心，**不要做成站立不动**；必须是前低后高爬行。

### 总提示词

```text
{粘贴 §0}

任务：生成 8 帧「被摸后狗爬讨好」一次性短动画关键帧（或 8 格分镜条）。

动作脚本：
01 坐或半起，开心被摸，微笑
02-03 前腿趴下，臀部抬起（play bow 预备）
04-06 保持前低后高，向前小爬 1～2 小步（位移很小，仍居中）
07-08 停下，抬头看镜头撒娇微笑

要求：可爱轻快；四肢着地爬行感明确；禁止只换表情不换姿势。
角色参考：dog-idle-happy.png；姿势勿抄站立 keyframe。
```

---

## §3 抚摸后磕头 `dog-react-bow`（缺 8 帧）

**交付文件名：** `dog-react-bow-01.png` … `dog-react-bow-08.png`
**关键帧参考：** `dog-greet-bow-keyfra.png`（低头触地姿态可作 04–05）

### 总提示词

```text
{粘贴 §0}

任务：生成 8 帧「抚摸后磕头讨好」短动画关键帧（或 8 格分镜条）。

动作脚本：
01 站或坐，微笑看镜头
02-03 前腿弯曲，头开始低下
04-05 额头接近地面磕头，屁股微抬（接近 dog-greet-bow-keyfra 的低头姿态）
06-07 轻轻抬头
08 抬头微笑结束（可再带一点点点头余韵）

要求：恭敬、柔和、可爱；不要夸张砸地、不要崩坏比例。
角色参考：dog-idle-happy.png + dog-greet-bow-keyfra.png。
```

---

## §4 打卡请安 `dog-greet-bow`（缺 10 帧）

**交付文件名：** `dog-greet-bow-01.png` … `dog-greet-bow-10.png`
**关键帧参考：** `dog-greet-bow-keyfra.png`

### 总提示词

```text
{粘贴 §0}

任务：生成 10 帧「起床/睡觉打卡磕头请安」仪式感短动画关键帧（或 10 格分镜条）。比日常抚摸磕头更郑重：低头有停顿。

动作脚本：
01 端正坐/站，注视镜头
02-03 收前腿，准备鞠躬
04-06 深深磕头请安，头低近地，其中至少 1 帧停顿（可用 dog-greet-bow-keyfra 为中间帧）
07-08 抬头，乖巧眼神
09-10 微微摇尾或轻轻再点一下头，收束到可接回待机

要求：可爱郑重；无文字；透明底；锁角色。
参考：dog-idle-happy.png、dog-greet-bow-keyfra.png。
```

---

## §5 可选 FX（缺则后补）

### `fx-heart-small.png`

```text
2048x2048 或 512x512 透明底，一颗小号粉色爱心，扁平描边，无背景，适合叠在柴犬头旁，简洁。
```

### `fx-sweat.png`

```text
透明底，一滴浅蓝卡通汗滴，扁平描边，无背景，小图标。
```

### `fx-sparkle.png`

```text
透明底，两三点金色/奶油色小闪光星芒，扁平，无背景。
```

---

## §6 一次生成「四套分镜条」的合并提示（可选）

若模型上下文够大，可一次要求四张条：

```text
{粘贴 §0}

请基于同一只参考柴犬，分别输出 4 张分镜条（每张一条动画，格子清晰分隔）：
A) 8 格：坐姿摇尾循环（dog-idle-wag）
B) 8 格：狗爬讨好（dog-react-crawl，前低后高）
C) 8 格：抚摸磕头（dog-react-bow）
D) 10 格：打卡请安磕头带停顿（dog-greet-bow）

每格同一朝向与比例；格子内不要字幕；白底或透明格均可，角色本身透明边缘干净。
```

---

## §7 生成后自检（提交前）

```text
[ ] 文件名与清单一致（wag 8 / crawl 8 / bow 8 / greet 10）
[ ] 与 dog-idle-happy 同角色
[ ] 爬行真的在爬；磕头真的有低头触地
[ ] 摇尾可循环
[ ] 请安有低头停顿
[ ] 无生气脸、无文字水印
```

---

## §8 交付放置

生成完成后放入：

```text
D:\life_assistant\asset\
  dog-idle-wag-01.png … 08.png
  dog-react-crawl-01.png … 08.png
  dog-react-bow-01.png … 08.png
  dog-greet-bow-01.png … 10.png
  （可选）fx-heart-small.png / fx-sweat.png / fx-sparkle.png
```

并更新 `D:\life_assistant\asset\ASSET_CHECKLIST.txt` 勾选状态。
