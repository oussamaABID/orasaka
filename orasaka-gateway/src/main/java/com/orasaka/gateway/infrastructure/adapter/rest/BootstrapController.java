package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.gateway.infrastructure.config.FeatureProperties;
import com.orasaka.gateway.infrastructure.config.FeatureRegistryProperties;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for bootstrapping configuration metadata. */
@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapController {

  private final FeatureRegistryProperties featureRegistryProperties;

  public BootstrapController(FeatureRegistryProperties featureRegistryProperties) {
    this.featureRegistryProperties = featureRegistryProperties;
  }

  /**
   * Endpoint returning all enabled feature registry blocks.
   *
   * @return List of enabled features.
   */
  @GetMapping("/features")
  public ResponseEntity<List<FeatureResponse>> getEnabledFeatures() {
    Map<String, FeatureProperties> features = featureRegistryProperties.features();
    if (features == null) {
      return ResponseEntity.ok(List.of());
    }

    List<FeatureResponse> enabledFeatures =
        features.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().enabled())
            .map(
                entry -> {
                  String rawKey = entry.getKey();
                  // Clean brackets if present
                  String cleanId = rawKey.replace("[", "").replace("]", "");
                  FeatureProperties props = entry.getValue();
                  return new FeatureResponse(
                      cleanId,
                      props.label(),
                      props.icon(),
                      props.uriPath(),
                      props.httpMethod(),
                      props.payloadTemplate());
                })
            .toList();

    return ResponseEntity.ok(enabledFeatures);
  }
}
