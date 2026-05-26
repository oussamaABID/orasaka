package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.gateway.application.service.JobStreamService;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserProfile;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import com.orasaka.persistence.domain.model.JobDto;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobListenerTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @TempDir Path tempDir;

  @Mock private JobPersistenceProvider jobPersistenceProvider;

  @Mock private JobStreamService jobStreamService;

  @Mock private ChatGenerationStrategy chatStrategy;

  @Mock private IdentityService identityService;

  @Mock private UserProfileProvider userProfileProvider;

  private JobListener listener;
  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    // Set up direct executor that runs task synchronously
    ExecutorService directExecutor = mock(ExecutorService.class);
    lenient()
        .doAnswer(
            invocation -> {
              Runnable runnable = invocation.getArgument(0);
              runnable.run();
              return null;
            })
        .when(directExecutor)
        .execute(any(Runnable.class));

    listener =
        new JobListener(
            jobPersistenceProvider,
            jobStreamService,
            List.of(chatStrategy),
            tempDir.toString(),
            directExecutor,
            5, // timeout seconds
            objectMapper,
            Optional.empty(),
            identityService,
            userProfileProvider);
  }

  @Test
  void onMessage_successfulExecution_completesJob() throws Exception {
    String jobId = "job-123";
    String userId = UUID.randomUUID().toString();
    JobMessage msg =
        new JobMessage(jobId, userId, "orasaka.core.chat.text", Map.of("prompt", "Hello test"));

    User user =
        new User(
            UUID.fromString(userId),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    UserProfile profile = new UserProfile(userId, "dark", "alloy", "tech", "helpful", Map.of());

    when(identityService.getUser(userId)).thenReturn(user);
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(chatStrategy.supports("orasaka.core.chat.text")).thenReturn(true);

    Map<String, Object> expectedResult = Map.of("content", "Response text");
    when(chatStrategy.execute(eq(msg), any())).thenReturn(expectedResult);

    // Call listener
    listener.onMessage(msg);

    // Verify status updates
    verify(jobPersistenceProvider).updateJobStatus(jobId, "PROCESSING", null, null);
    verify(jobPersistenceProvider).updateJobStatus(jobId, "COMPLETED", expectedResult, null);

    // Check that files were written
    Path inputJson = tempDir.resolve(userId).resolve(jobId).resolve("input").resolve("input.json");
    Path resultJson =
        tempDir.resolve(userId).resolve(jobId).resolve("output").resolve("result.json");

    assertTrue(Files.exists(inputJson));
    assertTrue(Files.exists(resultJson));
  }

  @Test
  void onMessage_strategyThrowsException_failsJob() throws Exception {
    String jobId = "job-123";
    String userId = UUID.randomUUID().toString();
    JobMessage msg =
        new JobMessage(jobId, userId, "orasaka.core.chat.text", Map.of("prompt", "Hello test"));

    User user =
        new User(
            UUID.fromString(userId),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    UserProfile profile = new UserProfile(userId, "dark", "alloy", "tech", "helpful", Map.of());

    when(identityService.getUser(userId)).thenReturn(user);
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(chatStrategy.supports("orasaka.core.chat.text")).thenReturn(true);
    when(chatStrategy.execute(eq(msg), any()))
        .thenThrow(new JobExecutionException("Strategy failed"));

    // Call listener
    listener.onMessage(msg);

    // Verify status updates
    verify(jobPersistenceProvider).updateJobStatus(jobId, "PROCESSING", null, null);
    verify(jobPersistenceProvider)
        .updateJobStatus(
            jobId,
            "FAILED",
            null,
            "com.orasaka.gateway.infrastructure.adapter.amqp.JobExecutionException: Strategy failed");
  }

  @Test
  void onMessage_unsupportedFeature_failsJob() {
    String jobId = "job-123";
    String userId = UUID.randomUUID().toString();
    JobMessage msg =
        new JobMessage(jobId, userId, "unsupported.feature", Map.of("prompt", "Hello test"));

    User user =
        new User(
            UUID.fromString(userId),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    UserProfile profile = new UserProfile(userId, "dark", "alloy", "tech", "helpful", Map.of());

    when(identityService.getUser(userId)).thenReturn(user);
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(chatStrategy.supports("unsupported.feature")).thenReturn(false);

    // Call listener
    listener.onMessage(msg);

    // Verify status updates
    verify(jobPersistenceProvider).updateJobStatus(jobId, "PROCESSING", null, null);
    verify(jobPersistenceProvider)
        .updateJobStatus(
            jobId,
            "FAILED",
            null,
            "com.orasaka.gateway.infrastructure.adapter.amqp.JobExecutionException: No strategy found for feature key: unsupported.feature");
  }

  @Test
  void onProgressMessage_callsStreamService() {
    ProgressMessage msg = new ProgressMessage("job-123", 45);
    JobDto job =
        new JobDto(
            "job-123",
            "user-456",
            "feature-x",
            "RUNNING",
            Map.of(),
            Map.of(),
            null,
            Instant.now(FIXED_CLOCK),
            Instant.now(FIXED_CLOCK));

    when(jobPersistenceProvider.getJob("job-123")).thenReturn(Optional.of(job));

    listener.onProgressMessage(msg);

    verify(jobStreamService).broadcastProgress("job-123", "user-456", 45);
  }

  @Test
  void onDeadLetter_updatesStatusAndBroadcastsFailure() {
    JobMessage msg = new JobMessage("job-123", "user-456", "feature-x", Map.of());

    listener.onDeadLetter(msg);

    verify(jobPersistenceProvider)
        .updateJobStatus(
            "job-123",
            "FAILED",
            null,
            "Job dead-lettered due to processing failure after max retry attempts");
    verify(jobStreamService).broadcastFailure("job-123");
  }

  @Test
  void onMessage_timeoutException_marksJobAsFailed() throws Exception {
    String jobId = "job-timeout";
    String userId = UUID.randomUUID().toString();
    JobMessage msg =
        new JobMessage(jobId, userId, "orasaka.core.chat.text", Map.of("prompt", "Hello test"));

    java.util.concurrent.ExecutorService threadExecutor =
        java.util.concurrent.Executors.newSingleThreadExecutor();
    try {
      JobListener timeoutListener =
          new JobListener(
              jobPersistenceProvider,
              jobStreamService,
              List.of(chatStrategy),
              tempDir.toString(),
              threadExecutor,
              1, // 1 second timeout
              objectMapper,
              Optional.empty(),
              identityService,
              userProfileProvider);

      when(identityService.getUser(userId))
          .thenAnswer(
              invocation -> {
                Thread.sleep(2000);
                return null;
              });

      timeoutListener.onMessage(msg);

      verify(jobPersistenceProvider).updateJobStatus(jobId, "PROCESSING", null, null);
      verify(jobPersistenceProvider)
          .updateJobStatus(jobId, "FAILED", null, "EXECUTION_TIMEOUT_EXCEEDED");
    } finally {
      threadExecutor.shutdownNow();
    }
  }

  @Test
  void onMessage_interruptedException_marksJobAsFailed() throws Exception {
    String jobId = "job-interrupt";
    String userId = UUID.randomUUID().toString();
    JobMessage msg =
        new JobMessage(jobId, userId, "orasaka.core.chat.text", Map.of("prompt", "Hello test"));

    java.util.concurrent.ExecutorService threadExecutor =
        java.util.concurrent.Executors.newSingleThreadExecutor();
    try {
      JobListener interruptListener =
          new JobListener(
              jobPersistenceProvider,
              jobStreamService,
              List.of(chatStrategy),
              tempDir.toString(),
              threadExecutor,
              5, // timeout seconds
              objectMapper,
              Optional.empty(),
              identityService,
              userProfileProvider);

      lenient()
          .when(identityService.getUser(userId))
          .thenAnswer(
              invocation -> {
                Thread.sleep(10000);
                return null;
              });

      // Set thread status to interrupted before calling get()
      Thread.currentThread().interrupt();

      interruptListener.onMessage(msg);

      // Clear interrupted status so JUnit can proceed
      Thread.interrupted();

      verify(jobPersistenceProvider).updateJobStatus(jobId, "PROCESSING", null, null);
      verify(jobPersistenceProvider)
          .updateJobStatus(jobId, "FAILED", null, "EXECUTION_INTERRUPTED");
    } finally {
      threadExecutor.shutdownNow();
    }
  }

  @Test
  void onMessage_withFilePath_copiesFileToInputDirectory() throws Exception {
    String jobId = "job-file-copy";
    String userId = UUID.randomUUID().toString();

    // Create a temp file to copy
    Path sourceFile = Files.createTempFile(tempDir, "test-source", ".mp4");
    Files.writeString(sourceFile, "video content");

    JobMessage msg =
        new JobMessage(
            jobId,
            userId,
            "orasaka.core.video",
            Map.of("prompt", "Hello test", "filePath", sourceFile.toString()));

    User user =
        new User(
            UUID.fromString(userId),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    UserProfile profile = new UserProfile(userId, "dark", "alloy", "tech", "helpful", Map.of());

    when(identityService.getUser(userId)).thenReturn(user);
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(chatStrategy.supports("orasaka.core.video")).thenReturn(true);
    when(chatStrategy.execute(eq(msg), any())).thenReturn(Map.of("result", "done"));

    listener.onMessage(msg);

    // Verify file copied to the correct path
    Path copiedFile = tempDir.resolve(userId).resolve(jobId).resolve("input").resolve("input.mp4");
    assertTrue(Files.exists(copiedFile));
    assertEquals("video content", Files.readString(copiedFile));
  }

  @Test
  void onMessage_withAudioAndVisionFilePaths_copiesWithCorrectNames() throws Exception {
    String jobId = "job-file-copy-media";
    String userId = UUID.randomUUID().toString();

    Path sourceAudio = Files.createTempFile(tempDir, "test-audio", ".mp3");
    Path sourceVision = Files.createTempFile(tempDir, "test-vision", ".png");
    Path sourceOther = Files.createTempFile(tempDir, "test-other", ".txt");

    User user =
        new User(
            UUID.fromString(userId),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    UserProfile profile = new UserProfile(userId, "dark", "alloy", "tech", "helpful", Map.of());

    when(identityService.getUser(userId)).thenReturn(user);
    when(userProfileProvider.getProfile(userId)).thenReturn(profile);
    when(chatStrategy.supports(any())).thenReturn(true);
    when(chatStrategy.execute(any(), any())).thenReturn(Map.of("result", "done"));

    // 1. Audio
    JobMessage msgAudio =
        new JobMessage(
            jobId, userId, "orasaka.core.audio", Map.of("filePath", sourceAudio.toString()));
    listener.onMessage(msgAudio);
    assertTrue(
        Files.exists(tempDir.resolve(userId).resolve(jobId).resolve("input").resolve("input.mp3")));

    // 2. Vision
    JobMessage msgVision =
        new JobMessage(
            jobId + "-v",
            userId,
            "orasaka.core.vision",
            Map.of("filePath", sourceVision.toString()));
    listener.onMessage(msgVision);
    assertTrue(
        Files.exists(
            tempDir.resolve(userId).resolve(jobId + "-v").resolve("input").resolve("input.png")));

    // 3. Other
    JobMessage msgOther =
        new JobMessage(
            jobId + "-o", userId, "orasaka.core.other", Map.of("filePath", sourceOther.toString()));
    listener.onMessage(msgOther);
    assertTrue(
        Files.exists(
            tempDir.resolve(userId).resolve(jobId + "-o").resolve("input").resolve("input.txt")));
  }
}
