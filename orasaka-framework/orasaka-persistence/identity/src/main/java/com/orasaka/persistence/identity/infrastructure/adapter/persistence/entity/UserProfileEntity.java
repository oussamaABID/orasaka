package com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity;

import com.orasaka.persistence.identity.infrastructure.adapter.persistence.converter.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA Entity mapping the {@code orasaka_user_profiles} database table. */
@Entity
@Table(name = "orasaka_user_profiles")
public class UserProfileEntity {

  @Id
  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(name = "theme", length = 50)
  private String theme = "emerald";

  @Column(name = "voice_model", length = 50)
  private String voiceModel = "alloy";

  @Column(name = "primary_industry", length = 100)
  private String primaryIndustry = "tech";

  @Column(name = "ai_behavior")
  private String aiBehavior;

  @Column(name = "raw_preferences", columnDefinition = "TEXT")
  @Convert(converter = JsonMapConverter.class)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private Map<String, Object> rawPreferences;

  /** Default constructor required by JPA/Hibernate. */
  public UserProfileEntity() {
    /* JPA requires no-arg constructor */
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getVoiceModel() {
    return voiceModel;
  }

  public void setVoiceModel(String voiceModel) {
    this.voiceModel = voiceModel;
  }

  public String getPrimaryIndustry() {
    return primaryIndustry;
  }

  public void setPrimaryIndustry(String primaryIndustry) {
    this.primaryIndustry = primaryIndustry;
  }

  public String getAiBehavior() {
    return aiBehavior;
  }

  public void setAiBehavior(String aiBehavior) {
    this.aiBehavior = aiBehavior;
  }

  public Map<String, Object> getRawPreferences() {
    return rawPreferences;
  }

  public void setRawPreferences(Map<String, Object> rawPreferences) {
    this.rawPreferences = rawPreferences;
  }
}
