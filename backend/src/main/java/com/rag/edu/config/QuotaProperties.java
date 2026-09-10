package com.rag.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 成本配额(对应 application.yml 中 quota.* 配置):
 * 每用户每月 AI 调用次数上限,0 表示不限;管理员不受限。
 */
@Data
@ConfigurationProperties(prefix = "quota")
public class QuotaProperties {

    /** 配额总开关 */
    private boolean enabled = true;

    /** 每用户每月智能问答次数上限 */
    private int chatLimit = 200;

    /** 每用户每月出题次数上限 */
    private int generateLimit = 30;

    /** 每用户每月主观题 AI 评分次数上限 */
    private int gradeLimit = 200;

    /** 每用户每月项目辅导 AI 拆解次数上限 */
    private int projectLimit = 10;

    /** 每用户每月知识图谱生成次数上限 */
    private int graphLimit = 30;
}
