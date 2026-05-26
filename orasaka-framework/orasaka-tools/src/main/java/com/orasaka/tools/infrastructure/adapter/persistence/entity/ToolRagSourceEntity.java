package com.orasaka.tools.infrastructure.adapter.persistence.entity;

import com.orasaka.persistence.infrastructure.adapter.persistence.converter.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA Entity mapping the {@code orasaka_tools_rag_source} database table.
 *
 * <p>Represents raw content chunks and metadata mapped to a tool to be loaded into the Vector
 * Database.
 */
@Entity
@Table(
    name = "orasaka_tools_rag_source",
    uniqueConstraints =
        @UniqueConstraint(
            name = "unique_tool_content",
            columnNames = {"tool_id", "content"}))
public class ToolRagSourceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "tool_id", nullable = false, length = 255)
  private String toolId;

  @Column(name = "content", nullable = false)
  private String content;

  @Column(name = "metadata", columnDefinition = "TEXT")
  @Convert(converter = JsonMapConverter.class)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private Map<String, Object> metadata = new HashMap<>();

  @Column(name = "ingested")
  private Boolean ingested = false;

  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public ToolRagSourceEntity() {
    // Required by JPA specification for reflective entity instantiation
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getToolId() {
    return toolId;
  }

  public void setToolId(String toolId) {
    this.toolId = toolId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public Boolean getIngested() {
    return ingested;
  }

  public void setIngested(Boolean ingested) {
    this.ingested = ingested;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
