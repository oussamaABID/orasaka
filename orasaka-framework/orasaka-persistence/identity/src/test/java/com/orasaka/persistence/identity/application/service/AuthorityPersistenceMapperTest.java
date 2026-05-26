package com.orasaka.persistence.identity.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.persistence.identity.domain.model.AuthorityDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.AuthorityEntity;
import org.junit.jupiter.api.Test;

class AuthorityPersistenceMapperTest {

  @Test
  void toDto_mapsAllFields() {
    var entity = new AuthorityEntity();
    entity.setId(1L);
    entity.setUserId("user-1");
    entity.setAuthorityName("ROLE_ADMIN");
    AuthorityDto dto = AuthorityPersistenceMapper.toDto(entity);
    assertEquals(1, dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("ROLE_ADMIN", dto.authorityName());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(AuthorityPersistenceMapper.toDto(null));
  }

  @Test
  void toEntity_mapsAllFields() {
    var dto = new AuthorityDto(1L, "user-1", "ROLE_USER");
    var entity = AuthorityPersistenceMapper.toEntity(dto);
    assertEquals(1, entity.getId());
    assertEquals("user-1", entity.getUserId());
    assertEquals("ROLE_USER", entity.getAuthorityName());
  }

  @Test
  void toEntity_null_returnsNull() {
    assertNull(AuthorityPersistenceMapper.toEntity(null));
  }
}
