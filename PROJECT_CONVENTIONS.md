# 项目约束(开发与协作规范)

> 本文件为项目的**强制工作流约束**,所有修改与提交须遵守。

## 1. 修改与提交流程(必须遵守)

1. **修改前检查**:每次进行修改前,先运行 `git status` 检查是否存在未提交的修改。
   - 若存在未提交的修改,**先提交这些修改**(确保能回退到所需版本),再进行新的修改。
2. **修改后编译测试**:修改完成后,执行编译/构建验证:
   - 后端:`mvn -o compile`(使用 JDK 17,本机路径 `E:\JDK\jdk-17.0.12`)
   - 前端:`npm run build`
3. **编译通过后才提交**:仅当编译/构建**通过**后,才执行 `git commit`。编译失败时须修正后再提交。

## 2. 提交信息规范(Conventional Commits)

格式:`<type>(<scope>): <description>`

- **`description` 一律使用中文简述本次修改概要**(不用英文),让 `git log` 不经翻译即可读。
- 一次提交只做一件事,不把无关改动混在同一个提交里。

| type | 含义 |
|------|------|
| feat | 新功能 |
| fix | 修复 Bug |
| refactor | 重构(不改变功能) |
| style | 样式修改 |
| docs | 文档更新 |
| test | 测试相关 |
| chore | 构建/工具变更 |

示例:`feat(platform): 重构为通用多学科智能学习平台`

## 3. 其它约定

- **数据库**:新库/建表脚本放在 `sql/`(如 `sql/init_learning.sql`),统一 `utf8mb4`。
- **技术栈**:后端 Spring Boot + Spring AI;前端 Vue3 + Element Plus;AI 走云端 API,不做模型微调。
- **目录**:前端页面在 `frontend/src/views/`,接口封装在 `frontend/src/api/index.js`,后端按 controller/service/mapper/entity 分层。
