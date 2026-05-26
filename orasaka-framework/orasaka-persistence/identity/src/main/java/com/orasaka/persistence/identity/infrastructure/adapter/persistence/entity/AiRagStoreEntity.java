package com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA Entity mapping the {@code orasaka_ai_rag_stores} database table. */
@Entity
@Table(name = "orasaka_ai_rag_stores")
public class AiRagStoreEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "store_type", nullable = false, length = 50)
  private String storeType;

  @Column(name = "host", length = 255)
  private String host;

  @Column(name = "port")
  private Integer port;

  @Column(name = "database_name", length = 255)
  private String databaseName;

  @Column(name = "table_name", length = 255)
  private String tableName;

  @Column(name = "username", length = 255)
  private String username;

  @Column(name = "password", length = 255)
  private String password;

  @Column(name = "enabled")
  private Boolean enabled = true;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  public AiRagStoreEntity() {
    /* JPA requires no-arg constructor */
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStoreType() {
    return storeType;
  }

  public void setStoreType(String storeType) {
    this.storeType = storeType;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
