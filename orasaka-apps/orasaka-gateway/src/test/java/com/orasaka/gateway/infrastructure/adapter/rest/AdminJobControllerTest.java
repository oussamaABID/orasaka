package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.ports.inbound.ChatSessionService;
import com.orasaka.core.domain.ports.inbound.JobService;
import com.orasaka.gateway.application.service.JobStreamService;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminJobControllerTest {

  @Mock private JobService jobService;
  @Mock private ChatSessionService chatSessionService;
  @Mock private ObjectProvider<StatefulRedisConnection<String, byte[]>> redisConnectionProvider;
  @Mock private JobStreamService jobStreamService;
  @Mock private StatefulRedisConnection<String, byte[]> redisConnection;
  @Mock private RedisCommands<String, byte[]> redisCommands;

  private AdminJobController controller;

  @BeforeEach
  void setUp() {
    controller =
        new AdminJobController(
            jobService, chatSessionService, redisConnectionProvider, jobStreamService);
  }

  @Test
  void purgeTestData_successfulExecution_cleansEverything() {
    // Setup Redis mock
    doAnswer(
            invocation -> {
              Consumer<StatefulRedisConnection<String, byte[]>> consumer =
                  invocation.getArgument(0);
              consumer.accept(redisConnection);
              return null;
            })
        .when(redisConnectionProvider)
        .ifAvailable(any());

    when(redisConnection.sync()).thenReturn(redisCommands);
    when(redisCommands.keys(anyString())).thenReturn(List.of("key-1"));
    when(redisCommands.del(any(String[].class))).thenReturn(1L);

    ResponseEntity<Void> response = controller.purgeTestData();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify DB purge
    List<String> testUserIds =
        List.of(
            "550e8400-e29b-41d4-a716-446655440001",
            "550e8400-e29b-41d4-a716-446655440002",
            "550e8400-e29b-41d4-a716-446655440003");
    for (String userId : testUserIds) {
      verify(jobService).purgeJobsByUserId(userId);
      verify(chatSessionService).purgeSessionsByUserId(userId);
    }

    // Verify Redis calls
    verify(redisCommands).flushdb();
    verify(redisCommands, times(3)).keys(anyString());
    verify(redisCommands, times(3)).del(any(String[].class));

    // Verify SSE purge
    verify(jobStreamService).purgeAllEmitters();
  }

  @Test
  void purgeTestData_databaseException_continuesGracefully() {
    // Throw in db operations
    doThrow(new TransientDataAccessResourceException("DB Error"))
        .when(jobService)
        .purgeJobsByUserId(anyString());

    ResponseEntity<Void> response = controller.purgeTestData();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify it still tries to run SSE emitter purge
    verify(jobStreamService).purgeAllEmitters();
  }

  @Test
  void purgeTestData_redisException_continuesGracefully() {
    // Setup Redis mock to throw exception
    doAnswer(
            invocation -> {
              Consumer<StatefulRedisConnection<String, byte[]>> consumer =
                  invocation.getArgument(0);
              consumer.accept(redisConnection);
              return null;
            })
        .when(redisConnectionProvider)
        .ifAvailable(any());

    when(redisConnection.sync()).thenThrow(new RuntimeException("Redis connection error"));

    ResponseEntity<Void> response = controller.purgeTestData();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify SSE still executes
    verify(jobStreamService).purgeAllEmitters();
  }

  @Test
  void purgeTestData_sseException_continuesGracefully() {
    doThrow(new RuntimeException("SSE purge error")).when(jobStreamService).purgeAllEmitters();

    ResponseEntity<Void> response = controller.purgeTestData();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void getActiveConnections_returnsCount() {
    when(jobStreamService.getActiveConnectionCount()).thenReturn(42);

    ResponseEntity<Integer> response = controller.getActiveConnections();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(42, response.getBody());
  }
}
