package com.zhai.hw.model.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowJobEventTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testDeserializeValidQueuedPayload() throws IOException {
        // Given
        InputStream inputStream = new ClassPathResource("github/workflow_job_queued.json").getInputStream();
        
        // When
        WorkflowJobEvent event = objectMapper.readValue(inputStream, WorkflowJobEvent.class);
        
        // Then
        assertNotNull(event);
        assertEquals("queued", event.getAction());
        
        WorkflowJob job = event.getWorkflowJob();
        assertNotNull(job);
        assertEquals(12345678, job.getId());
        assertEquals(87654321, job.getRunId());
        assertEquals("queued", job.getStatus());
        assertEquals(Arrays.asList("android-small", "self-hosted"), job.getLabels());
        
        Repository repository = job.getRepository();
        assertNotNull(repository);
        assertEquals("eBayMobile/andr_core", repository.getFullName());
    }
    
    @Test
    void testDeserializeValidCompletedPayload() throws IOException {
        // Given
        InputStream inputStream = new ClassPathResource("github/workflow_job_completed.json").getInputStream();
        
        // When
        WorkflowJobEvent event = objectMapper.readValue(inputStream, WorkflowJobEvent.class);
        
        // Then
        assertNotNull(event);
        assertEquals("completed", event.getAction());
        assertEquals("completed", event.getWorkflowJob().getStatus());
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Given
        Repository repo1 = new Repository("eBayMobile/andr_core");
        Repository repo2 = new Repository("eBayMobile/andr_core");
        Repository repo3 = new Repository("different/repo");
        
        WorkflowJob job1 = new WorkflowJob(1, 2, "queued", Arrays.asList("label1"), repo1);
        WorkflowJob job2 = new WorkflowJob(1, 2, "queued", Arrays.asList("label1"), repo2);
        WorkflowJob job3 = new WorkflowJob(3, 4, "completed", Arrays.asList("label2"), repo3);
        
        WorkflowJobEvent event1 = new WorkflowJobEvent("queued", job1);
        WorkflowJobEvent event2 = new WorkflowJobEvent("queued", job2);
        WorkflowJobEvent event3 = new WorkflowJobEvent("completed", job3);
        
        // Then
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
        
        assertNotEquals(event1, event3);
        assertNotEquals(event1.hashCode(), event3.hashCode());
    }
}
