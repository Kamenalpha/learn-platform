package com.rag.edu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.QuestionDtos.GradeReq;
import com.rag.edu.entity.ExamRecord;
import com.rag.edu.entity.Paper;
import com.rag.edu.entity.PaperQuestion;
import com.rag.edu.entity.Question;
import com.rag.edu.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExamPracticeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuestionMapper questionMapper = mock(QuestionMapper.class);
    private final PaperMapper paperMapper = mock(PaperMapper.class);
    private final PaperQuestionMapper paperQuestionMapper = mock(PaperQuestionMapper.class);
    private final ExamRecordMapper examRecordMapper = mock(ExamRecordMapper.class);
    private final ExamPracticeService service = new ExamPracticeService(mock(ChatModel.class), objectMapper,
            mock(ExamSourceMapper.class), questionMapper, paperMapper, paperQuestionMapper,
            examRecordMapper, mock(ExamAnswerMapper.class), mock(MistakeMapper.class),
            mock(StudyLogService.class), mock(CourseAccessService.class));

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
