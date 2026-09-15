# 项目约束(开发与协作规范)

> 本文件为项目的**强制工作流约束**,所有修改与提交须遵守。

## 1. 修改与提交流程(必须遵守)

1. **修改前检查**:每次进行修改前,先运行 `git status` 检查是否存在未提交的修改。
   - 若存在未提交的修改,**先提交这些修改**(确保能回退到所需版本),再进行新的修改。
2. **修改后编译测试**:修改完成后,执行编译/构建验证:
   - 后端:`mvn -o compile`(使用 JDK 17,本机路径 `E:\JDK\jdk-17.0.12`)
   - 前端:`npm run build`
3. **编译通过后才提交**:仅当编译/构建**通过**后,才执行 `git commit`。编译失败时须修正后再提交。
4. **大规模重构须新开分支**:遇到大规模代码重构/结构性修改(如跨模块或跨层批量改动、目录/包结构调整、框架或依赖体系迁移、伴随数据结构重组的改动)时,不得直接在 `main` 上开发:
   - 先从 `main` 切出分支,命名 `refactor/<主题>`(如 `refactor/分层重构`);
   - 在分支上仍按上述 1~3 步执行(修改前检查、编译验证、通过后提交);
   - 全量测试(后端 `mvn -o test` + 前端 `npm run build`)通过后再合回 `main`,合并前 `main` 有新提交时先 rebase/merge 解决冲突。
   - 日常小改动(bug 修复、小功能、文档)不受此条约束,可直接在 `main` 上进行。

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
