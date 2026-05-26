package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.core.domain.ports.inbound.ChatSessionService;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobStreamService;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only REST controller exposing database and cache sanitization operations. */
@RestController
@RequestMapping("/api/v1/admin/jobs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminJobController {

  private static final Logger logger = LoggerFactory.getLogger(AdminJobController.class);

  private final JobService jobService;
  private final ChatSessionService chatSessionService;
  private final ObjectProvider<StatefulRedisConnection<String, byte[]>> redisConnectionProvider;
  private final JobStreamService jobStreamService;

  public AdminJobController(
      JobService jobService,
      ChatSessionService chatSessionService,
      ObjectProvider<StatefulRedisConnection<String, byte[]>> redisConnectionProvider,
      JobStreamService jobStreamService) {
    this.jobService = Objects.requireNonNull(jobService, "JobService must not be null");
    this.chatSessionService =
        Objects.requireNonNull(chatSessionService, "ChatSessionService must not be null");
    this.redisConnectionProvider =
        Objects.requireNonNull(redisConnectionProvider, "RedisConnectionProvider must not be null");
    this.jobStreamService =
        Objects.requireNonNull(jobStreamService, "JobStreamService must not be null");
  }

  /**
   * Purges database jobs, chat sessions, and rate-limit cache entries for all default test user
   * accounts.
   *
   * @return Empty 200 OK response.
   */
  @PostMapping("/purge")
  public ResponseEntity<Void> purgeTestData() {
    logger.info("Admin initiated database and cache purge of test accounts.");

    List<String> testUserIds =
        List.of(
            "550e8400-e29b-41d4-a716-446655440001", // admin
            "550e8400-e29b-41d4-a716-446655440002", // user
            "550e8400-e29b-41d4-a716-446655440003" // guest
            );

    // 1. Purge jobs and chat sessions from database for all test users
    for (String userId : testUserIds) {
      try {
        jobService.purgeJobsByUserId(userId);
        chatSessionService.purgeSessionsByUserId(userId);
        logger.debug("Successfully purged database jobs and sessions for user ID: {}", userId);
      } catch (DataAccessException e) {
        logger.error("Failed to purge database jobs/sessions for user ID: {}", userId, e);
      }
    }

    // 2. Evict Bucket4j rate-limiting keys and clear entire Redis cache database
    redisConnectionProvider.ifAvailable(
        connection -> {
          try {
            var commands = connection.sync();
            commands.flushdb();
            logger.info("Successfully executed FLUSHDB on Redis test database.");

            for (String userId : testUserIds) {
              var keys = commands.keys(userId + "*");
              if (keys != null && !keys.isEmpty()) {
                commands.del(keys.toArray(new String[0]));
                logger.info("Evicted Redis rate-limiting keys for user ID: {}: {}", userId, keys);
              }
            }
          } catch (RuntimeException e) {
            logger.warn("Failed to evict rate-limiting keys/flush from Redis", e);
          }
        });

    // 3. Purge active SSE emitters to release connection threads immediately
    try {
      jobStreamService.purgeAllEmitters();
      logger.info("Successfully purged active SSE emitters.");
    } catch (RuntimeException e) {
      logger.error("Failed to purge SSE emitters", e);
    }

    return ResponseEntity.ok().build();
  }

  /**
   * Retrieves the current active connections count.
   *
   * @return The active connections count.
   */
  @GetMapping("/active-connections")
  public ResponseEntity<Integer> getActiveConnections() {
    return ResponseEntity.ok(jobStreamService.getActiveConnectionCount());
  }
}
