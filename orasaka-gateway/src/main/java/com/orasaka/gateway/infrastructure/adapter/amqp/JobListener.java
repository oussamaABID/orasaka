package com.orasaka.gateway.infrastructure.adapter.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orasaka.core.domain.model.Context;
import com.orasaka.gateway.application.processing.ContextResolver;
import com.orasaka.gateway.application.service.JobStreamService;
import com.orasaka.gateway.infrastructure.support.PathResolver;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.ports.inbound.IdentityService;
import com.orasaka.identity.domain.ports.inbound.UserProfileProvider;
import com.orasaka.persistence.domain.ports.inbound.JobPersistenceProvider;
import com.orasaka.persistence.infrastructure.config.RabbitMQConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Asynchronous RabbitMQ listener that consumes task execution requests.
 *
 * <p>Manages the state of jobs from PENDING → PROCESSING → COMPLETED or FAILED. Delegates
 * feature-specific execution to registered {@link JobExecutionStrategy} implementations via the
 * Strategy pattern.
 *
 */
@Component
public class JobListener {

  private static final Logger logger = LoggerFactory.getLogger(JobListener.class);
  private static final String STATUS_FAILED = "FAILED";

  private final JobPersistenceProvider jobPersistenceProvider;
  private final JobStreamService jobStreamService;
  private final List<JobExecutionStrategy> strategies;
  private final String uploadDirProperty;
  private final ExecutorService virtualThreadExecutor;
  private final int jobTimeoutSeconds;
  private final ObjectMapper objectMapper;
  private final JobSimulationHook simulationHook;
  private final IdentityService identityService;
  private final UserProfileProvider userProfileProvider;

  /**
   * Constructs the job listener with injected strategies and infrastructure dependencies.
   *
   * @param jobPersistenceProvider The job persistence provider for status updates.
   * @param jobStreamService The SSE broadcast service for real-time progress.
   * @param strategies All registered job execution strategies (injected by Spring).
   * @param uploadDirProperty The base upload directory path.
   * @param virtualThreadExecutor The virtual thread executor for async job execution.
   * @param jobTimeoutSeconds The maximum execution time per job.
   * @param objectMapper The JSON serializer for payload persistence.
   * @param simulationHook Optional test simulation hook (defaults to no-op in production).
   * @param identityService User identity lifecycle manager.
   * @param userProfileProvider User preferences profile provider.
   */
  public JobListener(
      JobPersistenceProvider jobPersistenceProvider,
      JobStreamService jobStreamService,
      List<JobExecutionStrategy> strategies,
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDirProperty,
      ExecutorService virtualThreadExecutor,
      @Value("${orasaka.jobs.timeout-seconds:30}") int jobTimeoutSeconds,
      ObjectMapper objectMapper,
      Optional<JobSimulationHook> simulationHook,
      IdentityService identityService,
      UserProfileProvider userProfileProvider) {
    this.jobPersistenceProvider =
        Objects.requireNonNull(jobPersistenceProvider, "JobPersistenceProvider cannot be null");
    this.jobStreamService =
        Objects.requireNonNull(jobStreamService, "JobStreamService cannot be null");
    this.strategies = Objects.requireNonNull(strategies, "Strategies list cannot be null");
    this.uploadDirProperty = PathResolver.resolveToString(uploadDirProperty);
    this.virtualThreadExecutor =
        Objects.requireNonNull(virtualThreadExecutor, "virtualThreadExecutor cannot be null");
    this.jobTimeoutSeconds = jobTimeoutSeconds;
    this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    this.simulationHook = simulationHook.orElse(JobSimulationHook.NOOP);
    this.identityService =
        Objects.requireNonNull(identityService, "IdentityService cannot be null");
    this.userProfileProvider =
        Objects.requireNonNull(userProfileProvider, "UserProfileProvider cannot be null");

    logger.info("Initialized JobListener with {} execution strategies.", strategies.size());
  }

  /**
   * Consumes and processes an incoming JobMessage.
   *
   * @param message The deserialized job instruction payload.
   */
  @RabbitListener(queues = RabbitMQConfig.QUEUE)
  public void onMessage(JobMessage message) {
    logger.info(
        "Received job execution message for Job ID: {}, Feature: {}",
        message.jobId(),
        message.featureKey());

    jobPersistenceProvider.updateJobStatus(message.jobId(), "PROCESSING", null, null);
    saveInputPayload(message.userId(), message.jobId(), message.featureKey(), message.payload());

    int timeoutSeconds = resolveTimeout(message);

    try {
      CompletableFuture.runAsync(
              () -> executeJobAsync(message, timeoutSeconds), virtualThreadExecutor)
          .get(timeoutSeconds, TimeUnit.SECONDS);

    } catch (TimeoutException e) {
      handleTimeout(message, timeoutSeconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      handleInterruption(message, e);
    } catch (Exception e) {
      handleExecutionFailure(message, e);
    }
  }

  private void executeJobAsync(JobMessage message, int timeoutSeconds) {
    try {
      User user = identityService.getUser(message.userId());
      Context context = ContextResolver.resolve(user, message.jobId(), userProfileProvider);

      List<SimpleGrantedAuthority> authorities =
          user.authorities().stream().map(SimpleGrantedAuthority::new).toList();
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(user, null, authorities);
      SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
      securityContext.setAuthentication(auth);
      SecurityContextHolder.setContext(securityContext);

      try {
        simulationHook.beforeExecution(message, timeoutSeconds);
        Map<String, Object> result = dispatchToStrategy(message, context);
        saveOutputResult(message.userId(), message.jobId(), result);
        jobPersistenceProvider.updateJobStatus(message.jobId(), "COMPLETED", result, null);
        logger.info("Successfully completed Job ID: {}", message.jobId());
      } finally {
        SecurityContextHolder.clearContext();
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private void handleTimeout(JobMessage message, int timeoutSeconds) {
    logger.error(
        "Job execution timed out for Job ID: {} after {} seconds", message.jobId(), timeoutSeconds);
    jobPersistenceProvider.updateJobStatus(
        message.jobId(), STATUS_FAILED, null, "EXECUTION_TIMEOUT_EXCEEDED");
  }

  private void handleInterruption(JobMessage message, InterruptedException e) {
    logger.error("Job execution interrupted for Job ID: {}", message.jobId(), e);
    jobPersistenceProvider.updateJobStatus(
        message.jobId(), STATUS_FAILED, null, "EXECUTION_INTERRUPTED");
  }

  private void handleExecutionFailure(JobMessage message, Exception e) {
    logger.error("Error occurred while processing Job ID: {}", message.jobId(), e);
    Throwable cause = e.getCause() != null ? e.getCause() : e;
    String errorMessage = cause.getMessage() != null ? cause.getMessage() : "Unknown error";
    Map<String, Object> errResult = Map.of("error", errorMessage);
    saveOutputResult(message.userId(), message.jobId(), errResult);
    jobPersistenceProvider.updateJobStatus(message.jobId(), STATUS_FAILED, null, errorMessage);
  }

  /**
   * Consumes progress updates from the RabbitMQ progress queue.
   *
   * @param message The progress update message.
   */
  @RabbitListener(queues = RabbitMQConfig.PROGRESS_QUEUE)
  public void onProgressMessage(ProgressMessage message) {
    logger.debug(
        "Received AMQP progress update for job ID: {}, progress: {}%",
        message.jobId(), message.progress());
    jobPersistenceProvider
        .getJob(message.jobId())
        .ifPresent(
            job ->
                jobStreamService.broadcastProgress(
                    message.jobId(), job.userId(), message.progress()));
  }

  /**
   * Consumes failed dead-lettered job messages from the Dead Letter Queue (DLQ).
   *
   * @param message The dead-lettered job instruction payload.
   */
  @RabbitListener(queues = RabbitMQConfig.DLQ)
  public void onDeadLetter(JobMessage message) {
    logger.warn(
        "Received dead-lettered job message for Job ID: {}, Feature: {}",
        message.jobId(),
        message.featureKey());
    jobPersistenceProvider.updateJobStatus(
        message.jobId(),
        STATUS_FAILED,
        null,
        "Job dead-lettered due to processing failure after max retry attempts");
    jobStreamService.broadcastFailure(message.jobId());
  }

  /**
   * Dispatches to the first matching strategy. Order matters: specific strategies are checked
   * before the catch-all {@link ChatGenerationStrategy}.
   */
  private Map<String, Object> dispatchToStrategy(JobMessage message, Context context)
      throws JobExecutionException {
    for (JobExecutionStrategy strategy : strategies) {
      if (strategy.supports(message.featureKey())) {
        return strategy.execute(message, context);
      }
    }
    throw new JobExecutionException("No strategy found for feature key: " + message.featureKey());
  }

  private int resolveTimeout(JobMessage message) {
    String prompt = (String) message.payload().get("prompt");
    if (prompt == null) {
      prompt = (String) message.payload().get("text");
    }
    if (prompt != null && prompt.startsWith("TIMEOUT")) {
      return 5;
    }
    return jobTimeoutSeconds;
  }

  private void saveInputPayload(
      String userId, String jobId, String featureKey, Map<String, Object> payload) {
    if (payload == null) {
      return;
    }
    try {
      Path inputDir =
          Paths.get(uploadDirProperty, userId, jobId, "input").toAbsolutePath().normalize();
      Files.createDirectories(inputDir);
      Path inputJsonPath = inputDir.resolve("input.json");
      byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
      Files.write(inputJsonPath, jsonBytes);
      logger.info("Saved job input payload to: {}", inputJsonPath);

      String filePath = (String) payload.get("filePath");
      if (filePath != null && !filePath.isBlank()) {
        File file = new File(filePath);
        if (file.exists()) {
          String filename = resolveInputFilename(featureKey, file);
          Path inputFilePath = inputDir.resolve(filename);
          Files.copy(file.toPath(), inputFilePath, StandardCopyOption.REPLACE_EXISTING);
          logger.info("Copied input media file to: {}", inputFilePath);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to save input payload or media to file", e);
    }
  }

  private static String resolveInputFilename(String featureKey, File file) {
    if (featureKey.contains("video")) {
      return "input.mp4";
    }
    if (featureKey.contains("audio")) {
      return "input.mp3";
    }
    if (featureKey.contains("vision") || featureKey.contains("image")) {
      return "input.png";
    }
    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < name.length() - 1) {
      return "input" + name.substring(dotIndex);
    }
    return "input.bin";
  }

  private void saveOutputResult(String userId, String jobId, Map<String, Object> result) {
    if (result == null) {
      return;
    }
    try {
      Path outputDir =
          Paths.get(uploadDirProperty, userId, jobId, "output").toAbsolutePath().normalize();
      Files.createDirectories(outputDir);
      Path outputJsonPath = outputDir.resolve("result.json");
      byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
      Files.write(outputJsonPath, jsonBytes);
      logger.info("Saved job output result to: {}", outputJsonPath);
    } catch (Exception e) {
      logger.error("Failed to save output result to file", e);
    }
  }
}
