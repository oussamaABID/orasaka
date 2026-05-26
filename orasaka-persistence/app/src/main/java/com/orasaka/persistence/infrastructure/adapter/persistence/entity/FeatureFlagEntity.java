package com.orasaka.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA Entity mapping the framework's dynamic feature flags configuration table. */
@Entity
@Table(name = "orasaka_feature_flags")
public class FeatureFlagEntity {

  @Id
  @Column(name = "feature_key")
  private String featureKey;

  @Column(name = "is_enabled", nullable = false)
  private Boolean isEnabled;

  public String getFeatureKey() {
    return featureKey;
  }

  public void setFeatureKey(String featureKey) {
    this.featureKey = featureKey;
  }

  public Boolean getIsEnabled() {
    return isEnabled;
  }

  public void setIsEnabled(Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }
}
