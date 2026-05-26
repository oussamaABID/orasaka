package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.UserMcpServerDto;
import java.util.List;
import java.util.Optional;

/** Port interface for managing UserMcpServer persistence operations. */
public interface UserMcpServerPersistenceProvider {

  List<UserMcpServerDto> findByUserIdAndEnabledTrue(String userId);

  List<UserMcpServerDto> findByUserId(String userId);

  Optional<UserMcpServerDto> findById(Integer id);

  UserMcpServerDto save(UserMcpServerDto serverDto);

  void deleteById(Integer id);
}
