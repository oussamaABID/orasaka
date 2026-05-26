package com.orasaka.persistence.identity.domain.ports;

import com.orasaka.persistence.identity.domain.model.AiRagStoreDto;
import java.util.List;

/** Port interface for managing AiRagStore persistence operations. */
public interface AiRagStorePersistenceProvider {

  List<AiRagStoreDto> findByUserIdAndEnabledTrue(String userId);

  AiRagStoreDto save(AiRagStoreDto storeDto);

  void deleteById(Integer id);
}
