package com.rag.edu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.QuestionDtos.GeneratedQuestion;
import com.rag.edu.dto.QuestionDtos.GradeReq;
import com.rag.edu.entity.ExamRecord;
import com.rag.edu.entity.KnowledgePoint;
import com.rag.edu.entity.Mistake;
import com.rag.edu.entity.Paper;
import com.rag.edu.entity.PaperQuestion;
import com.rag.edu.entity.Question;
import com.rag.edu.mapper.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExamPracticeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuestionMapper questionMapper = mock(QuestionMapper.class);
    private final PaperMapper paperMapper = mock(PaperMapper.class);
    private final PaperQuestionMapper paperQuestionMapper = mock(PaperQuestionMapper.class);
    private final ExamRecordMapper examRecordMapper = mock(ExamRecordMapper.class);
    private final MistakeMapper mistakeMapper = mock(MistakeMapper.class);
    private final KnowledgePointMapper knowledgePointMapper = mock(KnowledgePointMapper.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final ExamPracticeService service = new ExamPracticeService(chatModel, objectMapper,
            mock(ExamSourceMapper.class), questionMapper, paperMapper, paperQuestionMapper,
            examRecordMapper, mock(ExamAnswerMapper.class), mistakeMapper,
            knowledgePointMapper, mock(StudyLogService.class), mock(CourseAccessService.class));

    @Test
    void optionsAreStoredAsJsonArrayAndNullForNonChoice() {
        assertEquals("[\"A.甲\",\"B.乙\"]", service.serializeOptions(List.of("A.甲", "B.乙")));
        assertNull(service.serializeOptions(null));
        assertNull(service.serializeOptions(List.of()));
    }

    @Test
    void paperDetailDoesNotExposeAnswersBeforeSubmission() throws Exception {
        Paper paper = paper(2L, 1L, 30);
        PaperQuestion relation = new PaperQuestion();
        relation.setPaperId(2L);
        relation.setQuestionId(3L);
        Question question = new Question();
        question.setQuestionId(3L);
        question.setQtype(0);
        question.setStem("题干");
        question.setOptions("[\"A.甲\",\"B.乙\"]");
        question.setAnswer("A");
        question.setAnalysis("保密解析");
        when(paperMapper.selectById(2L)).thenReturn(paper);
        when(paperQuestionMapper.selectList(any())).thenReturn(List.of(relation));
        when(questionMapper.selectById(3L)).thenReturn(question);

        String json = objectMapper.writeValueAsString(service.paperDetail(2L, 1L));

        assertFalse(json.contains("保密解析"));
        assertFalse(json.contains("\"answer\""));
        assertTrue(json.contains("\"options\":[\"A.甲\",\"B.乙\"]"));
    }

    @Test
    void submittedExamCannotBeSubmittedAgain() {
        ExamRecord exam = exam(5L, 1L, 1, LocalDateTime.now());
        when(examRecordMapper.selectById(5L)).thenReturn(exam);

        BizException error = assertThrows(BizException.class,
                () -> service.grade(1L, new GradeReq(5L, List.of())));

        assertEquals("考试已交卷,请勿重复提交", error.getMessage());
    }

    @Test
    void expiredExamIsClosedWithoutGrading() {
        ExamRecord exam = exam(5L, 1L, 0, LocalDateTime.now().minusMinutes(31));
        when(examRecordMapper.selectById(5L)).thenReturn(exam);
        when(paperMapper.selectById(2L)).thenReturn(paper(2L, 1L, 30));
        when(paperQuestionMapper.selectList(any())).thenReturn(List.of());
        when(examRecordMapper.claim(any(), any(), any(Integer.class), any())).thenReturn(1);

        Map<String, Object> result = service.grade(1L, new GradeReq(5L, List.of()));

        assertEquals("expired", result.get("status"));
        assertEquals("考试已截止", result.get("message"));
    }

    @Test
    void variantsPaperIsBuiltAsLightweightPractice() {
        Mistake mistake = new Mistake();
        mistake.setMistakeId(7L);
        mistake.setUserId(1L);
        mistake.setQuestionId(3L);
        Question origin = new Question();
        origin.setQuestionId(3L);
        origin.setOwnerId(1L);
        origin.setKpId(5L);
        origin.setQtype(0);
        origin.setStem("求 f(x)=x² 的导数");
        origin.setOptions("[\"A.2x\",\"B.x\"]");
        origin.setAnswer("A");
        origin.setAnalysis("求导公式");
        origin.setDifficulty(2);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKpId(5L);
        kp.setKpName("导数");
        when(mistakeMapper.selectById(7L)).thenReturn(mistake);
        when(questionMapper.selectById(3L)).thenReturn(origin);
        when(knowledgePointMapper.selectById(5L)).thenReturn(kp);
        when(examRecordMapper.selectList(any())).thenReturn(List.of());
        AtomicLong ids = new AtomicLong(100);
        when(questionMapper.insert(any(Question.class))).thenAnswer(inv -> {
            inv.getArgument(0, Question.class).setQuestionId(ids.getAndIncrement());
            return 1;
        });
        when(paperMapper.insert(any(Paper.class))).thenAnswer(inv -> {
            inv.getArgument(0, Paper.class).setPaperId(9L);
            return 1;
        });

        ExamPracticeService spy = spy(service);
        GeneratedQuestion variant = new GeneratedQuestion(0, "求 g(t)=3t² 的导数", List.of("A.6t", "B.3t"), "A",
                "同考点变式解析", null);
        doReturn(List.of(variant, variant)).when(spy).requestVariants(anyString());

        Map<String, Object> result = spy.generateVariants(1L, 7L);

        assertEquals(9L, result.get("paperId"));
        ArgumentCaptor<Paper> paperCaptor = ArgumentCaptor.forClass(Paper.class);
        verify(paperMapper).insert(paperCaptor.capture());
        Paper savedPaper = paperCaptor.getValue();
        assertEquals(ExamPracticeService.SOURCE_VARIANT, savedPaper.getSourceType());
        assertEquals(0, savedPaper.getDurationMin());
        assertTrue(savedPaper.getTitle().startsWith("变式训练"));
        assertTrue(savedPaper.getTitle().contains("导数"));
        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper, times(2)).insert(questionCaptor.capture());
        Question savedQuestion = questionCaptor.getValue();
        assertEquals(ExamPracticeService.SOURCE_VARIANT, savedQuestion.getSourceType());
        assertEquals(5L, savedQuestion.getKpId());
        verify(paperQuestionMapper, times(2)).insert(any(PaperQuestion.class));
        assertEquals(20, savedPaper.getTotalScore());
    }

    @Test
    void variantsRejectOtherUsersMistake() {
        Mistake mistake = new Mistake();
        mistake.setMistakeId(7L);
        mistake.setUserId(2L);
        mistake.setQuestionId(3L);
        when(mistakeMapper.selectById(7L)).thenReturn(mistake);

        BizException error = assertThrows(BizException.class, () -> service.generateVariants(1L, 7L));

        assertEquals(404, error.getCode());
        assertEquals("错题不存在", error.getMessage());
    }

    @Test
    void variantsWrapLlmFailureAsBizException() {
        Mistake mistake = new Mistake();
        mistake.setMistakeId(7L);
        mistake.setUserId(1L);
        mistake.setQuestionId(3L);
        Question origin = new Question();
        origin.setQuestionId(3L);
        origin.setQtype(0);
        origin.setStem("题干");
        when(mistakeMapper.selectById(7L)).thenReturn(mistake);
        when(questionMapper.selectById(3L)).thenReturn(origin);
        when(examRecordMapper.selectList(any())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("boom"));

        BizException error = assertThrows(BizException.class, () -> service.generateVariants(1L, 7L));

        assertTrue(error.getMessage().startsWith("大模型出题失败"));
    }

    private static Paper paper(Long paperId, Long userId, int durationMin) {
        Paper paper = new Paper();
        paper.setPaperId(paperId);
        paper.setUserId(userId);
        paper.setDurationMin(durationMin);
        return paper;
    }

    private static ExamRecord exam(Long examId, Long userId, int status, LocalDateTime startTime) {
        ExamRecord exam = new ExamRecord();
        exam.setExamId(examId);
        exam.setUserId(userId);
        exam.setPaperId(2L);
        exam.setStatus(status);
        exam.setStartTime(startTime);
        return exam;
    }
}
