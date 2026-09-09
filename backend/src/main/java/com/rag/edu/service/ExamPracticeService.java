package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.QuestionDtos.AiGradeResult;
import com.rag.edu.dto.QuestionDtos.GeneratedQuestion;
import com.rag.edu.dto.QuestionDtos.GenerateReq;
import com.rag.edu.dto.QuestionDtos.GradeReq;
import com.rag.edu.dto.QuestionDtos.GradeItem;
import com.rag.edu.dto.QuestionDtos.QuestionVO;
import com.rag.edu.entity.*;
import com.rag.edu.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.core.ParameterizedTypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 出题模拟:依据 教材块/用户重点/样卷 自动生成题目;客观题自动判分,主观题 AI 评分;错题入错题本.
 */
@Slf4j
@Service
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
    private final CourseAccessService courseAccessService;

    public ExamPracticeService(ChatModel chatModel, ObjectMapper objectMapper, ExamSourceMapper sourceMapper,
                               QuestionMapper questionMapper, PaperMapper paperMapper,
                               PaperQuestionMapper paperQuestionMapper, ExamRecordMapper examRecordMapper,
                               ExamAnswerMapper examAnswerMapper, MistakeMapper mistakeMapper,
                               StudyLogService studyLogService, CourseAccessService courseAccessService) {
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
        this.courseAccessService = courseAccessService;
    }

    @Transactional
    public Map<String, Object> generate(Long userId, GenerateReq req) {
        int sourceType = req.sourceType() == null ? 0 : req.sourceType();
        if (sourceType != 1) {
            courseAccessService.requireManagedCourse(req.courseId(), userId);
            if (req.resourceId() != null) {
                DocResource resource = courseAccessService.requireManagedResource(req.resourceId(), userId);
                if (!Objects.equals(resource.getCourseId(), req.courseId())) {
                    throw new BizException("资料不属于所选课程");
                }
            }
        }
        String material = gatherMaterial(userId, req);
        if (material.isBlank()) {
            throw new BizException("素材不足,无法出题");
        }
        int count = req.count() == null || req.count() < 1 ? 5 : Math.min(req.count(), 20);
        int difficulty = req.difficulty() == null ? 1 : req.difficulty();
        String qtypes = req.qtypes() == null || req.qtypes().isEmpty() ? "单选、多选、判断、填空、简答" : joinTypes(req.qtypes());

        String prompt = """
                你是命题助手。请根据下面的【学习材料】生成 %d 道题目,题型为:%s,难度%d(1简单2中等3难)。
                要求:选择题必须提供选项;答案格式——选择题填字母(单选"A",多选"A,B");判断题填"对"或"错";填空/简答填参考答案文本;每题附解析。
                学习材料:
                %s
                """.formatted(count, qtypes, difficulty, material);

        List<GeneratedQuestion> items;
        try {
            items = chatClient.prompt().user(prompt).call()
                    .entity(new ParameterizedTypeReference<List<GeneratedQuestion>>() {
                    });
        } catch (Exception e) {
            throw new BizException("大模型出题失败: " + e.getMessage());
        }
        if (items == null || items.isEmpty()) {
            throw new BizException("生成题目为空,请重试");
        }

        List<Question> questions = new ArrayList<>();
        for (GeneratedQuestion item : items) {
            if (item == null || item.stem() == null || item.stem().isBlank()) {
                continue;
            }
            Question q = new Question();
            q.setOwnerId(userId);
            q.setResourceId(req.resourceId());
            q.setQtype(item.qtype() == null ? 0 : item.qtype());
            q.setStem(item.stem());
            q.setOptions(serializeOptions(item.options()));
            q.setAnswer(item.answer() == null ? "" : item.answer());
            q.setAnalysis(item.analysis() == null ? "" : item.analysis());
            q.setDifficulty(item.difficulty() == null ? difficulty : item.difficulty());
            q.setSourceType(req.sourceType() == null ? 0 : req.sourceType());
            questionMapper.insert(q);
            questions.add(q);
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

    public Map<String, Object> paperDetail(Long paperId, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null || !Objects.equals(paper.getUserId(), userId)) {
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

    /** 开始一次正式考试，答案只保留在服务端。 */
    @Transactional
    public Map<String, Object> start(Long userId, Long paperId) {
        Paper paper = requireOwnedPaper(paperId, userId);
        LocalDateTime startedAt = LocalDateTime.now();
        ExamRecord exam = new ExamRecord();
        exam.setUserId(userId);
        exam.setPaperId(paperId);
        exam.setStartTime(startedAt);
        exam.setScore(0.0);
        exam.setStatus(0);
        examRecordMapper.insert(exam);

        Integer durationMin = paper.getDurationMin() == null ? 0 : paper.getDurationMin();
        return map("examId", exam.getExamId(), "paperId", paperId, "title", paper.getTitle(),
                "startTime", startedAt, "deadline", durationMin > 0 ? startedAt.plusMinutes(durationMin) : null,
                "durationMin", durationMin, "questions", loadPaperQuestions(paperId).stream()
                        .map(PaperQuestion::getQuestionId).map(questionMapper::selectById)
                        .filter(Objects::nonNull).map(this::toVO).toList());
    }

    /** 交卷并判分(客观题自动 + 主观题AI评分),错题入错题本 */
    @Transactional
    public Map<String, Object> grade(Long userId, GradeReq req) {
        if (req == null || req.examId() == null) {
            throw new BizException("考试记录ID不能为空");
        }
        ExamRecord exam = examRecordMapper.selectById(req.examId());
        if (exam == null || !Objects.equals(exam.getUserId(), userId)) {
            throw new BizException(404, "考试记录不存在");
        }
        if (!Objects.equals(exam.getStatus(), 0)) {
            throw new BizException(exam.getStatus() != null && exam.getStatus() == 2
                    ? "考试已截止" : "考试已交卷,请勿重复提交");
        }
        Paper paper = requireOwnedPaper(exam.getPaperId(), userId);
        List<PaperQuestion> pqs = loadPaperQuestions(paper.getPaperId());
        Map<Long, PaperQuestion> scoreMap = new HashMap<>();
        for (PaperQuestion pq : pqs) {
            scoreMap.put(pq.getQuestionId(), pq);
        }

        Map<Long, String> submitted = new HashMap<>();
        for (GradeItem item : req.answers() == null ? List.<GradeItem>of() : req.answers()) {
            if (item == null || item.questionId() == null) {
                throw new BizException("作答题目ID不能为空");
            }
            if (!scoreMap.containsKey(item.questionId())) {
                throw new BizException("提交内容包含不属于该试卷的题目");
            }
            if (submitted.putIfAbsent(item.questionId(), item.userAnswer()) != null) {
                throw new BizException("同一道题不能重复提交");
            }
        }

        LocalDateTime endedAt = LocalDateTime.now();
        int durationMin = paper.getDurationMin() == null ? 0 : paper.getDurationMin();
        if (exam.getStartTime() == null) {
            throw new BizException("考试尚未开始");
        }
        LocalDateTime deadline = durationMin > 0 ? exam.getStartTime().plusMinutes(durationMin) : null;
        if (deadline != null && endedAt.isAfter(deadline)) {
            claimExam(exam.getExamId(), userId, 2, deadline);
            return map("examId", exam.getExamId(), "status", "expired", "message", "考试已截止",
                    "startTime", exam.getStartTime(), "endTime", deadline);
        }
        if (claimExam(exam.getExamId(), userId, 1, endedAt) == 0) {
            throw new BizException("考试已交卷,请勿重复提交");
        }

        List<Map<String, Object>> itemResults = new ArrayList<>();
        double total = 0, got = 0;
        for (PaperQuestion pq : pqs) {
            Question q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) {
                continue;
            }
            int score = pq.getScore() == null ? 10 : pq.getScore();
            total += score;
            String userAnswer = submitted.getOrDefault(q.getQuestionId(), "");
            ExamAnswer ea = new ExamAnswer();
            ea.setExamId(exam.getExamId());
            ea.setQuestionId(q.getQuestionId());
            ea.setUserAnswer(userAnswer);

            if (q.getQtype() != null && q.getQtype() <= 2) {
                boolean correct = normalize(q.getAnswer()).equals(normalize(userAnswer));
                ea.setIsCorrect(correct ? 1 : 0);
                if (correct) {
                    got += score;
                } else {
                    upsertMistake(userId, q.getQuestionId());
                }
                itemResults.add(map("questionId", q.getQuestionId(), "correct", correct, "score", correct ? score : 0));
            } else {
                Map<String, Object> grade = aiGrade(q, userAnswer);
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
        double finalScore = Math.round(got * 10) / 10.0;
        examRecordMapper.updateScore(exam.getExamId(), finalScore);

        // 埋点:使用服务端记录的真实考试时长,供学习画像统计。
        try {
            long actualSeconds = Math.max(0, Duration.between(exam.getStartTime(), endedAt).toSeconds());
            studyLogService.logStudy(userId, 1, null, (int) Math.min(Integer.MAX_VALUE, actualSeconds));
        } catch (Exception e) {
            log.warn("埋点记录失败: {}", e.getMessage());
        }

        return map("examId", exam.getExamId(), "status", "submitted", "score", finalScore,
                "total", total, "startTime", exam.getStartTime(), "endTime", endedAt, "items", itemResults);
    }

    private int claimExam(Long examId, Long userId, int status, LocalDateTime endTime) {
        return examRecordMapper.claim(examId, userId, status, endTime);
    }

    private Paper requireOwnedPaper(Long paperId, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null || !Objects.equals(paper.getUserId(), userId)) {
            throw new BizException(404, "试卷不存在");
        }
        return paper;
    }

    private List<PaperQuestion> loadPaperQuestions(Long paperId) {
        return paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId).orderByAsc(PaperQuestion::getOrderNum));
    }

    private Map<String, Object> aiGrade(Question q, String userAnswer) {
        String prompt = """
                你是阅卷老师。请给下面学生的作答打分(0-100,含小数),并给一句评语。
                题目:%s  参考答案:%s  学生答案:%s
                """.formatted(q.getStem(), q.getAnswer(), userAnswer == null ? "" : userAnswer);
        try {
            AiGradeResult result = chatClient.prompt().user(prompt).call().entity(AiGradeResult.class);
            return map("score", result == null ? 0 : result.score(),
                    "comment", result == null ? "" : result.comment());
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
            int durationMin = p == null || p.getDurationMin() == null ? 0 : p.getDurationMin();
            if (Objects.equals(e.getStatus(), 0) && durationMin > 0 && e.getStartTime() != null) {
                LocalDateTime deadline = e.getStartTime().plusMinutes(durationMin);
                if (LocalDateTime.now().isAfter(deadline) && claimExam(e.getExamId(), userId, 2, deadline) == 1) {
                    e.setStatus(2);
                    e.setEndTime(deadline);
                }
            }
            return map("examId", e.getExamId(), "title", p == null ? "" : p.getTitle(),
                    "score", e.getScore(), "status", e.getStatus(), "startTime", e.getStartTime(),
                    "endTime", e.getEndTime(), "durationMin", durationMin);
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
            List<Map<String, Object>> chunks = sourceMapper.courseChunks(
                    userId, req.courseId(), req.resourceId(), 100);
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

    private QuestionVO toVO(Question q) {
        return new QuestionVO(q.getQuestionId(), q.getQtype(), q.getStem(), parseOptions(q.getOptions()),
                q.getDifficulty(), q.getSourceType());
    }

    /** 选项数组序列化为 JSON 字符串落库(选择题);非选择题为 null */
    String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> options = new ArrayList<>();
            node.forEach(option -> options.add(option.asText()));
            return options;
        } catch (Exception e) {
            return List.of();
        }
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
