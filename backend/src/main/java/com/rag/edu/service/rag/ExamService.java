package com.rag.edu.service.rag;

import com.rag.edu.common.BizException;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.entity.Course;
import com.rag.edu.service.CourseAccessService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学习辅助:按章节自动生成知识点梳理与练习题
 */
@Service
public class ExamService {

    private static final String PROMPT = """
            你是数字媒体技术专业的资深教师,请基于给定的课程资料,围绕指定章节完成:
            一、知识点梳理(分条列出核心概念,每个概念一两句话解释,并标注来源编号如[1]);
            二、重点考点(3-5条,说明常考题型);
            三、练习题(5道,题型混合:名词解释/简答/应用,每题附参考答案与解析)。
            若资料不足以覆盖该章节,请如实说明已覆盖的范围。
            """;

    private final CourseAccessService courseAccessService;
    private final RetrievalService retrievalService;
    private final ChatClient chatClient;

    public ExamService(CourseAccessService courseAccessService, RetrievalService retrievalService,
                       ChatModel chatModel) {
        this.courseAccessService = courseAccessService;
        this.retrievalService = retrievalService;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String generate(Long courseId, Long userId, String chapter) {
        Course course = courseAccessService.requireManagedCourse(courseId, userId);
        String query = (chapter == null || chapter.isBlank())
                ? course.getCourseName() + " 核心知识点 考点"
                : course.getCourseName() + " " + chapter;

        // 统一检索入口(双路召回+融合),权限过滤在 RetrievalService 内完成;出题保持 TopK=10、阈值 0.3
        List<Document> hits = retrievalService.retrieve(query, List.of(courseId), userId,
                new KbConfig(10, 0.3, null, null, null, false));
        if (hits == null || hits.isEmpty()) {
            throw new BizException("该课程知识库暂无内容,请先上传并解析课件文档");
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            Document hit = hits.get(i);
            context.append('[').append(i + 1).append("] ")
                    .append(hit.getMetadata().getOrDefault("docTitle", "")).append('\n')
                    .append(abbreviate(hit.getText() == null ? "" : hit.getText(), 800)).append("\n\n");
        }
        String user = "课程: " + course.getCourseName()
                + (chapter == null || chapter.isBlank() ? "(全课程)" : "章节: " + chapter)
                + "\n\n【课程资料】\n" + context;
        try {
            return chatClient.prompt().system(PROMPT).user(user).call().content();
        } catch (Exception e) {
            throw new BizException("大模型调用失败: " + e.getMessage());
        }
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
