package com.rag.edu.service;

import com.rag.edu.common.BizException;
import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.entity.Assistant;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.DocResource;
import com.rag.edu.mapper.AssistantCourseMapper;
import com.rag.edu.mapper.AssistantMapper;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocResourceMapper;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.mapper.QaRecordMapper;
import com.rag.edu.service.rag.ChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseAccessServiceTest {

    private final CourseMapper courseMapper = mock(CourseMapper.class);
    private final DocResourceMapper resourceMapper = mock(DocResourceMapper.class);
    private final CourseAccessService access = new CourseAccessService(courseMapper, resourceMapper);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void privateCourseIsLimitedToOwner() {
        Course course = course(10L, 1L, 0, 0);
        when(courseMapper.selectById(10L)).thenReturn(course);

        assertEquals(course, access.requireReadableCourse(10L, 1L));
        BizException denied = assertThrows(BizException.class,
                () -> access.requireReadableCourse(10L, 2L));
        assertEquals(403, denied.getCode());
    }

    @Test
    void publicCourseRequiresApproval() {
        when(courseMapper.selectById(10L)).thenReturn(course(10L, 1L, 1, 0));
        assertThrows(BizException.class, () -> access.requireReadableCourse(10L, 2L));

        Course approved = course(11L, 1L, 1, 1);
        when(courseMapper.selectById(11L)).thenReturn(approved);
        assertEquals(approved, access.requireReadableCourse(11L, 2L));
    }

    @Test
    void publicResourceAlsoRequiresPublicApprovedCourse() {
        DocResource resource = resource(20L, 1L, 10L, 1, 1);
        when(resourceMapper.selectById(20L)).thenReturn(resource);
        when(courseMapper.selectById(10L)).thenReturn(course(10L, 1L, 0, 0));
        assertThrows(BizException.class, () -> access.requireReadableResource(20L, 2L));

        when(courseMapper.selectById(10L)).thenReturn(course(10L, 1L, 1, 1));
        assertEquals(resource, access.requireReadableResource(20L, 2L));
    }

    @Test
    void administratorCanManageForeignData() {
        Course course = course(10L, 1L, 0, 0);
        DocResource resource = resource(20L, 1L, 10L, 0, 0);
        when(courseMapper.selectById(10L)).thenReturn(course);
        when(resourceMapper.selectById(20L)).thenReturn(resource);
        UserContext.set(new LoginUser(99L, "admin", 1));

        assertDoesNotThrow(() -> access.requireManagedCourse(10L, 99L));
        assertDoesNotThrow(() -> access.requireManagedResource(20L, 99L));
    }

    @Test
    void assistantDetailIsLimitedToItsOwner() {
        AssistantMapper assistantMapper = mock(AssistantMapper.class);
        Assistant assistant = new Assistant();
        assistant.setAssistantId(30L);
        assistant.setUserId(1L);
        when(assistantMapper.selectById(30L)).thenReturn(assistant);
        AssistantService service = new AssistantService(
                assistantMapper, mock(AssistantCourseMapper.class), access);

        BizException denied = assertThrows(BizException.class, () -> service.getDetail(30L, 2L));
        assertEquals(404, denied.getCode());
    }

    @Test
    void scopedVectorSearchFailsClosed() {
        VectorStore vectorStore = mock(VectorStore.class);
        KbConfigService configService = mock(KbConfigService.class);
        when(configService.get()).thenReturn(new KbConfig(5, 0.5, 500, 50, null));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("filter unavailable"));
        ChatService chatService = new ChatService(mock(ChatModel.class), vectorStore, configService,
                mock(StringRedisTemplate.class), mock(QaRecordMapper.class), mock(DocChunkMapper.class),
                new ObjectMapper(), access);

        BizException denied = assertThrows(BizException.class,
                () -> chatService.ask(1L, "session", "question", List.of(10L), null));

        assertEquals("知识库检索失败,请稍后重试", denied.getMessage());
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    private static Course course(Long id, Long ownerId, int visibility, int auditStatus) {
        Course course = new Course();
        course.setCourseId(id);
        course.setOwnerId(ownerId);
        course.setVisibility(visibility);
        course.setAuditStatus(auditStatus);
        return course;
    }

    private static DocResource resource(Long id, Long userId, Long courseId,
                                        int visibility, int auditStatus) {
        DocResource resource = new DocResource();
        resource.setResourceId(id);
        resource.setUserId(userId);
        resource.setCourseId(courseId);
        resource.setVisibility(visibility);
        resource.setAuditStatus(auditStatus);
        return resource;
    }
}
