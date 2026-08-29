package com.rag.edu.service.rag;

import com.rag.edu.common.BizException;
import com.rag.edu.entity.Course;
import com.rag.edu.mapper.CourseMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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

    private final CourseMapper courseMapper;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public ExamService(CourseMapper courseMapper, VectorStore vectorStore, ChatModel chatModel) {
        this.courseMapper = courseMapper;
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String generate(Long courseId, String chapter) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        String query = (chapter == null || chapter.isBlank())
                ? course.getCourseName() + " 核心知识点 考点"
                : course.getCourseName() + " " + chapter;

        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query).topK(10).similarityThreshold(0.3).build());
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
