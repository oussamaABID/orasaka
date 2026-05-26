package com.orasaka.core.application.engine;

import com.orasaka.core.application.pipeline.EnginePipelineBridge;
import com.orasaka.core.application.pipeline.EngineStreamBridge;
import com.orasaka.core.application.service.DynamicChatModelFactory;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.core.domain.ports.outbound.UserCredentialsProvider;
import java.util.Objects;

/**
 * Immutable parameter object grouping engine infrastructure dependencies.
 *
 * <p>Introduced to reduce the {@link AbstractEngine} constructor from 11 parameters to 7,
 * satisfying SonarQube rule {@code java:S107} (constructor parameter count ≤ 7).
 *
 * @param modelCatalogProvider Outbound port for querying available model metadata.
 * @param pipelineBridge The pipeline compilation bridge component.
 * @param streamBridge The reactive streaming bridge component.
 * @param credentialsProvider Outbound port for decrypted user API credentials lookup.
 * @param modelFactory Dynamic ChatModel factory for commercial providers.
 * @since 1.0.0
 */
public record EngineInfrastructure(
    ModelCatalogProvider modelCatalogProvider,
    EnginePipelineBridge pipelineBridge,
    EngineStreamBridge streamBridge,
    UserCredentialsProvider credentialsProvider,
    DynamicChatModelFactory modelFactory) {

  public EngineInfrastructure {
    Objects.requireNonNull(pipelineBridge, "EnginePipelineBridge must not be null");
    Objects.requireNonNull(streamBridge, "EngineStreamBridge must not be null");
    Objects.requireNonNull(credentialsProvider, "UserCredentialsProvider must not be null");
    Objects.requireNonNull(modelFactory, "DynamicChatModelFactory must not be null");
  }
}
