package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.QuestionDtos.GenerateReq;
import com.rag.edu.dto.QuestionDtos.GradeReq;
import com.rag.edu.dto.QuestionDtos.GradeItem;
import com.rag.edu.dto.QuestionDtos.QuestionVO;
import com.rag.edu.entity.*;
import com.rag.edu.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 出题模拟:依据 教材块/用户重点/样卷 自动生成题目;客观题自动判分,主观题 AI 评分;错题入错题本.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPracticeService {

    private static final int MAX_SOURCE_CHARS = 4000;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ExamSourceMapper sourceMapper;
    private final QuestionMapper questionMapper;
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final MistakeMapper mistakeMapper;
    private final StudyLogService studyLogService;

    public ExamPracticeService(ChatModel chatModel, ObjectMapper objectMapper, ExamSourceMapper sourceMapper,
                               QuestionMapper questionMapper, PaperMapper paperMapper,
                               PaperQuestionMapper paperQuestionMapper, ExamRecordMapper examRecordMapper,
                               ExamAnswerMapper examAnswerMapper, MistakeMapper mistakeMapper,
                               StudyLogService studyLogService) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
        this.sourceMapper = sourceMapper;
        this.questionMapper = questionMapper;
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examAnswerMapper = examAnswerMapper;
        this.mistakeMapper = mistakeMapper;
        this.studyLogService = studyLogService;
    }

    @Transactional
    public Map<String, Object> generate(Long userId, GenerateReq req) {
        String material = gatherMaterial(userId, req);
        if (material.isBlank()) {
            throw new BizException("素材不足,无法出题");
        }
        int count = req.count() == null || req.count() < 1 ? 5 : Math.min(req.count(), 20);
        int difficulty = req.difficulty() == null ? 1 : req.difficulty();
        String qtypes = req.qtypes() == null || req.qtypes().isEmpty() ? "单选、多选、判断、填空、简答" : joinTypes(req.qtypes());

        String prompt = """
                你是命题助手。请根据下面的【学习材料】生成 %d 道题目,题型为:%s,难度%d(1简单2中等3难)。
                严格按如下 JSON 数组返回,不要输出其它文字,每项字段:
                {"qtype":题型编号(0单选,1多选,2判断,3填空,4简答),"stem":"题干",\
                "options":"选择题选项数组JSON(如 [\\"A.选项1\\",\\"B.选项2\\"])或null(非选择题)",\
                "answer":"答案: 选择题填字母(单选'A'多选'A,B');判断题填'对'或'错';填空/简答填文本",\
                "analysis":"解析","difficulty":难度编号}
                学习材料:
                %s
                """.formatted(count, qtypes, difficulty, material);

        String raw;
        try {
            raw = chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            throw new BizException("大模型出题失败: " + e.getMessage());
        }
        List<JsonNode> items = parseJsonArray(raw);

        List<Question> questions = new ArrayList<>();
        for (JsonNode item : items) {
            Question q = new Question();
            q.setOwnerId(userId);
            q.setResourceId(req.resourceId());
            q.setQtype(item.path("qtype").asInt(0));
            q.setStem(item.path("stem").asText(""));
            q.setOptions(item.path("options").isMissingNode() || item.path("options").isNull()
                    ? null : item.path("options").asText());
            q.setAnswer(item.path("answer").asText(""));
            q.setAnalysis(item.path("analysis").asText(""));
            q.setDifficulty(item.path("difficulty").asInt(difficulty));
            q.setSourceType(req.sourceType() == null ? 0 : req.sourceType());
            if (!q.getStem().isBlank()) {
                questionMapper.insert(q);
                questions.add(q);
            }
        }
        if (questions.isEmpty()) {
            throw new BizException("生成题目为空,请重试");
        }

        // 自动组卷
        Paper paper = new Paper();
        paper.setUserId(userId);
        paper.setTitle("自动试卷 · " + LocalDateTime.now().toLocalDate());
        paper.setSourceType(req.sourceType() == null ? 0 : req.sourceType());
        paper.setDifficulty(difficulty);
        paper.setTotalScore(questions.size() * 10);
        paper.setDurationMin(30);
        paperMapper.insert(paper);
        int order = 0;
        for (Question q : questions) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getPaperId());
            pq.setQuestionId(q.getQuestionId());
            pq.setScore(10);
            pq.setOrderNum(order++);
            paperQuestionMapper.insert(pq);
        }
        return Map.of("paperId", paper.getPaperId(), "title", paper.getTitle(),
                "questions", questions.stream().map(this::toVO).toList());
    }

    public Map<String, Object> paperDetail(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BizException(404, "试卷不存在");
        }
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId).orderByAsc(PaperQuestion::getOrderNum));
        List<Question> questions = new ArrayList<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q != null) {
                questions.add(q);
            }
        }
        return Map.of("paper", paper, "questions", questions.stream().map(this::toVO).toList());
    }

    /** 交卷并判分(客观题自动 + 主观题AI评分),错题入错题本 */
    @Transactional
    public Map<String, Object> grade(Long userId, GradeReq req) {
        Paper paper = paperMapper.selectById(req.paperId());
        if (paper == null) {
            throw new BizException(404, "试卷不存在");
        }
        List<PaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, req.paperId()));
        Map<Long, PaperQuestion> scoreMap = new HashMap<>();
        for (PaperQuestion pq : pqs) {
            scoreMap.put(pq.getQuestionId(), pq);
        }

        ExamRecord exam = new ExamRecord();
        exam.setUserId(userId);
        exam.setPaperId(paper.getPaperId());
        exam.setStartTime(LocalDateTime.now());
        exam.setEndTime(LocalDateTime.now());
        exam.setStatus(1);
        exam.setScore(0.0);
        examRecordMapper.insert(exam);

        List<Map<String, Object>> itemResults = new ArrayList<>();
        double total = 0, got = 0;
        for (GradeItem gi : req.answers()) {
            Question q = questionMapper.selectById(gi.questionId());
            if (q == null) {
                continue;
            }
            PaperQuestion pq = scoreMap.get(q.getQuestionId());
            int score = (pq == null || pq.getScore() == null) ? 10 : pq.getScore();
            total += score;
            ExamAnswer ea = new ExamAnswer();
            ea.setExamId(exam.getExamId());
            ea.setQuestionId(q.getQuestionId());
            ea.setUserAnswer(gi.userAnswer());

            if (q.getQtype() != null && q.getQtype() <= 2) {
                boolean correct = normalize(q.getAnswer()).equals(normalize(gi.userAnswer()));
                ea.setIsCorrect(correct ? 1 : 0);
                if (correct) {
                    got += score;
                } else {
                    upsertMistake(userId, q.getQuestionId());
                }
                itemResults.add(map("questionId", q.getQuestionId(), "correct", correct, "score", correct ? score : 0));
            } else {
                Map<String, Object> grade = aiGrade(q, gi.userAnswer());
                Object s = grade.get("score");
                double sub = s == null ? 0 : Double.parseDouble(s.toString());
                ea.setAiScore(sub);
                ea.setAiComment(String.valueOf(grade.get("comment")));
                got += score * sub / 100.0;
                if (sub < 60) {
                    upsertMistake(userId, q.getQuestionId());
                }
                itemResults.add(map("questionId", q.getQuestionId(), "correct", sub >= 60, "score", score * sub / 100.0));
            }
            examAnswerMapper.insert(ea);
        }
        exam.setScore(Math.round(got * 10) / 10.0);
        examRecordMapper.updateById(exam);

        // 埋点:做题行为约30分钟,供学习画像统计
        try {
            studyLogService.logStudy(userId, 1, null, 1800);
        } catch (Exception e) {
            log.warn("埋点记录失败: {}", e.getMessage());
        }

        return map("examId", exam.getExamId(), "score", exam.getScore(), "total", total, "items", itemResults);
    }

    private Map<String, Object> aiGrade(Question q, String userAnswer) {
        String prompt = """
                你是阅卷老师。请给下面学生的作答打分(0-100,含小数),并给一句评语。
                题目:%s  参考答案:%s  学生答案:%s
                严格返回 JSON:{"score":80.5,"comment":"评语"} ,不要其它文字。
                """.formatted(q.getStem(), q.getAnswer(), userAnswer == null ? "" : userAnswer);
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(extractJson(raw));
            return map("score", node.path("score").asDouble(), "comment", node.path("comment").asText(""));
        } catch (Exception e) {
            log.warn("主观题AI评分失败,待人工复核: {}", e.getMessage());
            return map("score", 0, "comment", "AI评分失败,待人工复核");
        }
    }

    private void upsertMistake(Long userId, Long questionId) {
        Mistake m = mistakeMapper.selectOne(new LambdaQueryWrapper<Mistake>()
                .eq(Mistake::getUserId, userId).eq(Mistake::getQuestionId, questionId).last("LIMIT 1"));
        if (m != null) {
            m.setWrongCount((m.getWrongCount() == null ? 0 : m.getWrongCount()) + 1);
            m.setLastWrongTime(LocalDateTime.now());
            mistakeMapper.updateById(m);
        } else {
            m = new Mistake();
            m.setUserId(userId);
            m.setQuestionId(questionId);
            m.setWrongCount(1);
            m.setLastWrongTime(LocalDateTime.now());
            m.setMastered(0);
            mistakeMapper.insert(m);
        }
    }

    public List<Map<String, Object>> listExams(Long userId) {
        List<ExamRecord> list = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).orderByDesc(ExamRecord::getCreateTime));
        return list.stream().map(e -> {
            Paper p = paperMapper.selectById(e.getPaperId());
            return map("examId", e.getExamId(), "title", p == null ? "" : p.getTitle(),
                    "score", e.getScore(), "time", String.valueOf(e.getCreateTime()));
        }).toList();
    }

    public List<Map<String, Object>> mistakes(Long userId) {
        List<Mistake> list = mistakeMapper.selectList(new LambdaQueryWrapper<Mistake>()
                .eq(Mistake::getUserId, userId).eq(Mistake::getMastered, 0).orderByDesc(Mistake::getLastWrongTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Mistake m : list) {
            Question q = questionMapper.selectById(m.getQuestionId());
            result.add(map("mistakeId", m.getMistakeId(), "questionId", m.getQuestionId(),
                    "stem", q == null ? "" : q.getStem(), "answer", q == null ? "" : q.getAnswer(),
                    "analysis", q == null ? "" : q.getAnalysis(), "wrongCount", m.getWrongCount()));
        }
        return result;
    }

    public void masterMistake(Long userId, Long mistakeId) {
        Mistake m = mistakeMapper.selectById(mistakeId);
        if (m != null && m.getUserId().equals(userId)) {
            m.setMastered(1);
            mistakeMapper.updateById(m);
        }
    }

    private String gatherMaterial(Long userId, GenerateReq req) {
        int sourceType = req.sourceType() == null ? 0 : req.sourceType();
        StringBuilder sb = new StringBuilder();
        if (sourceType == 1) {
            List<String> kps = sourceMapper.userKeypoints(userId, 30);
            kps.forEach(k -> sb.append(k).append('\n'));
        } else {
            List<Map<String, Object>> chunks = sourceMapper.courseChunks(req.courseId(), req.resourceId(), 100);
            for (Map<String, Object> c : chunks) {
                Object content = c.get("content");
                if (content != null) {
                    sb.append(content).append('\n');
                }
            }
        }
        String s = sb.toString();
        return s.length() > MAX_SOURCE_CHARS ? s.substring(0, MAX_SOURCE_CHARS) : s;
    }

    private List<JsonNode> parseJsonArray(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end < start) {
            return List.of();
        }
        try {
            JsonNode arr = objectMapper.readTree(raw.substring(start, end + 1));
            List<JsonNode> list = new ArrayList<>();
            if (arr.isArray()) {
                arr.forEach(list::add);
            }
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        return start < 0 || end < start ? "{}" : raw.substring(start, end + 1);
    }

    private QuestionVO toVO(Question q) {
        return new QuestionVO(q.getQuestionId(), q.getQtype(), q.getStem(), q.getOptions(),
                q.getAnswer(), q.getAnalysis(), q.getDifficulty(), q.getSourceType());
    }

    private String joinTypes(List<Integer> types) {
        StringBuilder sb = new StringBuilder();
        String[] names = {"单选", "多选", "判断", "填空", "简答"};
        for (Integer t : types) {
            if (t >= 0 && t < names.length) {
                sb.append(names[t]).append('、');
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "单选、多选、判断、填空、简答";
    }

    private String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }

    /** 构建 Map<String,Object>(避免 Map.of 混合类型泛型不变性问题) */
    private Map<String, Object> map(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kvs.length; i += 2) {
            m.put((String) kvs[i], kvs[i + 1]);
        }
        return m;
    }
}
