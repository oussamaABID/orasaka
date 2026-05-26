package com.orasaka.gateway.infrastructure.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.gateway.application.service.JobQueuePublisherService;
import com.orasaka.gateway.infrastructure.adapter.amqp.JobMessage;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobSubmissionHelperTest {

  @Test
  void testSubmitSuccess(@TempDir Path tempDir) throws IOException {
    JobPersistenceProvider jobPersistenceProvider = mock(JobPersistenceProvider.class);
    JobQueuePublisherService jobQueuePublisher = mock(JobQueuePublisherService.class);

    // Create a temporary source file
    Path sourceFilePath = tempDir.resolve("test-video.mp4");
    Files.writeString(sourceFilePath, "dummy content");
    File sourceFile = sourceFilePath.toFile();

    String uploadDir = tempDir.resolve("uploads").toString();
    String userId = "user-abc";
    String featureKey = "vision";
    Map<String, Object> payload = new HashMap<>();

    JobSubmissionHelper.SubmissionResult result =
        JobSubmissionHelper.submit(
            jobPersistenceProvider,
            jobQueuePublisher,
            uploadDir,
            userId,
            featureKey,
            payload,
            sourceFile);

    assertNotNull(result);
    assertNotNull(result.jobId());
    assertNotNull(result.inputFilePath());
    assertTrue(Files.exists(result.inputFilePath()));

    // Verify folders were created
    Path jobDir = tempDir.resolve("uploads").resolve(userId).resolve(result.jobId());
    assertTrue(Files.exists(jobDir.resolve("input")));
    assertTrue(Files.exists(jobDir.resolve("output")));
    assertTrue(Files.exists(jobDir.resolve("temp")));

    // Verify payload values
    assertEquals(result.inputFilePath().toAbsolutePath().toString(), payload.get("filePath"));
    assertEquals("video/mp4", payload.get("mimeType"));

    // Verify mock interactions
    verify(jobPersistenceProvider).createJob(result.jobId(), userId, featureKey, payload);
    verify(jobQueuePublisher).publish(any(JobMessage.class));
  }

  @Test
  void testSubmitMimeTypeResolution(@TempDir Path tempDir) throws IOException {
    JobPersistenceProvider jobPersistenceProvider = mock(JobPersistenceProvider.class);
    JobQueuePublisherService jobQueuePublisher = mock(JobQueuePublisherService.class);
    String uploadDir = tempDir.resolve("uploads").toString();

    // mp3 extension
    Path pathMp3 = tempDir.resolve("test.mp3");
    Files.writeString(pathMp3, "dummy");
    Map<String, Object> payload1 = new HashMap<>();
    JobSubmissionHelper.submit(
        jobPersistenceProvider,
        jobQueuePublisher,
        uploadDir,
        "u",
        "key",
        payload1,
        pathMp3.toFile());
    assertEquals("audio/mpeg", payload1.get("mimeType"));

    // png extension
    Path pathPng = tempDir.resolve("test.png");
    Files.writeString(pathPng, "dummy");
    Map<String, Object> payload2 = new HashMap<>();
    JobSubmissionHelper.submit(
        jobPersistenceProvider,
        jobQueuePublisher,
        uploadDir,
        "u",
        "key",
        payload2,
        pathPng.toFile());
    assertEquals("image/png", payload2.get("mimeType"));

    // jpg extension
    Path pathJpg = tempDir.resolve("test.jpg");
    Files.writeString(pathJpg, "dummy");
    Map<String, Object> payload3 = new HashMap<>();
    JobSubmissionHelper.submit(
        jobPersistenceProvider,
        jobQueuePublisher,
        uploadDir,
        "u",
        "key",
        payload3,
        pathJpg.toFile());
    assertEquals("image/jpeg", payload3.get("mimeType"));

    // unknown extension
    Path pathUnknown = tempDir.resolve("test.xyzabcde");
    Files.writeString(pathUnknown, "dummy");
    Map<String, Object> payload4 = new HashMap<>();
    JobSubmissionHelper.submit(
        jobPersistenceProvider,
        jobQueuePublisher,
        uploadDir,
        "u",
        "key",
        payload4,
        pathUnknown.toFile());
    assertEquals("application/octet-stream", payload4.get("mimeType"));
  }
}
