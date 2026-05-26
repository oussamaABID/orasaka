package com.orasaka.tools.application.service;

import static com.orasaka.test.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.orasaka.tools.infrastructure.adapter.persistence.entity.ToolCacheEntity;
import com.orasaka.tools.infrastructure.adapter.persistence.entity.ToolCacheId;
import com.orasaka.tools.infrastructure.adapter.persistence.repository.ToolCacheRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

/** Unit tests for {@link ToolCacheService}. */
@ExtendWith(MockitoExtension.class)
class ToolCacheServiceTest {

  @Mock private ToolCacheRepository cacheRepository;

  private ToolCacheService service;

  @BeforeEach
  void setUp() {
    service = new ToolCacheService(cacheRepository);
  }

  // --- CacheEntry record tests ---

  @Test
  void cacheEntry_nullExpiry_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new ToolCacheService.CacheEntry("val", null));
  }

  @Test
  void cacheEntry_nullValue_defaultsToEmpty() {
    var entry = new ToolCacheService.CacheEntry(null, Instant.now().plusSeconds(60));
    assertEquals("", entry.value());
  }

  @Test
  void cacheEntry_isExpired_pastTimestamp_returnsTrue() {
    var entry = new ToolCacheService.CacheEntry("v", Instant.now().minusSeconds(10));
    assertTrue(entry.isExpired());
  }

  @Test
  void cacheEntry_isExpired_futureTimestamp_returnsFalse() {
    var entry = new ToolCacheService.CacheEntry("v", Instant.now().plusSeconds(60));
    assertFalse(entry.isExpired());
  }

  // --- get() ---

  @Test
  void get_localCacheHit_returnsValue() {
    // First put into cache
    service.put(TOOL_1, KEY_1, "cached-value", 600);

    String result = service.get(TOOL_1, KEY_1);

    assertEquals("cached-value", result);
  }

  @Test
  void get_localCacheMiss_dbHit_returnsValue() {
    ToolCacheEntity entity = new ToolCacheEntity();
    entity.setId(new ToolCacheId(TOOL_1, "key2"));
    entity.setCacheValue("db-value");
    entity.setExpiresAt(Instant.now().plusSeconds(3600));

    when(cacheRepository.findById(new ToolCacheId(TOOL_1, "key2"))).thenReturn(Optional.of(entity));

    String result = service.get(TOOL_1, "key2");

    assertEquals("db-value", result);
  }

  @Test
  void get_localCacheMiss_dbMiss_returnsNull() {
    when(cacheRepository.findById(any(ToolCacheId.class))).thenReturn(Optional.empty());

    assertNull(service.get(TOOL_1, "unknown"));
  }

  @Test
  void get_localCacheMiss_dbExpired_evictsAndReturnsNull() {
    ToolCacheEntity entity = new ToolCacheEntity();
    entity.setId(new ToolCacheId(TOOL_1, "key3"));
    entity.setCacheValue("expired-value");
    entity.setExpiresAt(Instant.now().minusSeconds(10));

    when(cacheRepository.findById(new ToolCacheId(TOOL_1, "key3"))).thenReturn(Optional.of(entity));

    String result = service.get(TOOL_1, "key3");

    assertNull(result);
    verify(cacheRepository).deleteById(new ToolCacheId(TOOL_1, "key3"));
  }

  @Test
  void get_dbException_returnsNullGracefully() {
    when(cacheRepository.findById(any(ToolCacheId.class)))
        .thenThrow(new DataAccessResourceFailureException("DB down"));

    String result = service.get(TOOL_1, "keyErr");

    assertNull(result);
  }

  // --- put() ---

  @Test
  void put_storesInLocalAndDb() {
    service.put(TOOL_1, KEY_1, "value", 300);

    verify(cacheRepository).save(any(ToolCacheEntity.class));
    assertEquals("value", service.get(TOOL_1, KEY_1));
  }

  @Test
  void put_dbException_localCacheStillWorks() {
    doThrow(new DataAccessResourceFailureException("DB down"))
        .when(cacheRepository)
        .save(any(ToolCacheEntity.class));

    service.put(TOOL_1, "key-err", "value", 300);

    // Local cache should still have it
    assertEquals("value", service.get(TOOL_1, "key-err"));
  }

  // --- local cache expiry ---

  @Test
  void get_localCacheExpired_evictsLocalAndDb() {
    // Put with 0 TTL (expired immediately)
    service.put(TOOL_1, "key-exp", "val", 0);

    // Should detect as expired
    String result = service.get(TOOL_1, "key-exp");

    assertNull(result);
    verify(cacheRepository).deleteById(new ToolCacheId(TOOL_1, "key-exp"));
  }
}
