## Why

网站「小助手」需要向中国工信部提交 ICP 备案申请，备案材料中需要明确填写服务名称、服务功能和服务用途。目前这些信息尚未整理成文，需要在当前项目中编写备案所需的描述文本，以便后续提交至阿里云 ICP 备案系统。

## What Changes

- 编写 ICP 备案所需的服务名称（中文/英文）
- 编写 ICP 备案所需的服务功能描述
- 编写 ICP 备案所需的服务用途说明
- 将备案信息整理为项目文档，存储在 `docs/icp/` 目录下

## Capabilities

### New Capabilities
- `icp-filing-docs`: ICP 备案材料文档，包含服务名称、服务功能、服务用途的规范描述

### Modified Capabilities

无

## Impact

- 新增 `docs/icp/` 目录及备案文档
- 无代码变更，无 API/数据库/依赖影响
