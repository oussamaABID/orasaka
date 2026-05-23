package com.orasaka.tools.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Convert;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.orasaka.tools.entity.converter.JsonMapConverter;

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
public class OrasakaToolRagSourceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "tool_id", nullable = false, length = 255)
  private String toolId;

  @Column(name = "content", nullable = false)
  private String content;

  @Convert(converter = JsonMapConverter.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata = new HashMap<>();

  @Column(name = "ingested")
  private Boolean ingested = false;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public OrasakaToolRagSourceEntity() {}

  /**
   * Gets the generated unique ID.
   *
   * @return The ID.
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the generated unique ID.
   *
   * @param id The ID.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the unique tool identifier this RAG source is associated with.
   *
   * @return The tool ID.
   */
  public String getToolId() {
    return toolId;
  }

  /**
   * Sets the unique tool identifier this RAG source is associated with.
   *
   * @param toolId The tool ID.
   */
  public void setToolId(String toolId) {
    this.toolId = toolId;
  }

  /**
   * Gets the text content to be ingested.
   *
   * @return The text content.
   */
  public String getContent() {
    return content;
  }

  /**
   * Sets the text content to be ingested.
   *
   * @param content The text content.
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * Gets the JSON metadata attributes.
   *
   * @return The metadata map.
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /**
   * Sets the JSON metadata attributes.
   *
   * @param metadata The metadata map.
   */
  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  /**
   * Checks if this source has already been ingested into the vector DB.
   *
   * @return True if ingested, false otherwise.
   */
  public Boolean getIngested() {
    return ingested;
  }

  /**
   * Sets whether this source has already been ingested into the vector DB.
   *
   * @param ingested True if ingested, false otherwise.
   */
  public void setIngested(Boolean ingested) {
    this.ingested = ingested;
  }

  /**
   * Gets the creation timestamp.
   *
   * @return The creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the creation timestamp.
   *
   * @param createdAt The creation timestamp.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
