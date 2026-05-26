package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.AiMcpServerDto;
import java.util.List;

/** Port interface for managing AiMcpServer persistence operations. */
public interface AiMcpServerPersistenceProvider {

  List<AiMcpServerDto> findByUserIdAndEnabledTrue(String userId);

  AiMcpServerDto save(AiMcpServerDto serverDto);

  void deleteById(Integer id);
}
