package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.AuthorityDto;
import java.util.List;

/** Port interface for managing Authority persistence operations. */
public interface AuthorityPersistenceProvider {

  void saveAuthority(String userId, String authorityName);

  List<AuthorityDto> findByUserId(String userId);
}
