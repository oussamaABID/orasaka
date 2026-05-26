package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.orasaka.core.domain.model.OllamaCatalog;
import com.orasaka.core.domain.model.OllamaModel;
import com.orasaka.core.domain.ports.outbound.ModelCatalogProvider;
import com.orasaka.core.infrastructure.config.CoreProperties;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Unit tests for {@link ComplianceController}. */
class ComplianceControllerTest {

  private DataSource dataSource;
  private Connection connection;
  private Statement statement;
  private ResultSet resultSet;
  private ModelCatalogProvider modelCatalogProvider;
  private CoreProperties coreProperties;
  private ComplianceController controller;

  @BeforeEach
  void setUp() throws Exception {
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    modelCatalogProvider = mock(ModelCatalogProvider.class);
    coreProperties = mock(CoreProperties.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    controller =
        new ComplianceController(
            dataSource, modelCatalogProvider, coreProperties, "http://localhost:11434");
  }

  @Test
  @DisplayName("Constructor throws NullPointerException on null datasource")
  void constructor_nullDataSource_throws() {
    assertThatThrownBy(
            () ->
                new ComplianceController(
                    null, modelCatalogProvider, coreProperties, "http://localhost:11434"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("DataSource");
  }

  @Test
  @DisplayName("Constructor throws NullPointerException on null modelCatalogProvider")
  void constructor_nullModelCatalogProvider_throws() {
    assertThatThrownBy(
            () ->
                new ComplianceController(
                    dataSource, null, coreProperties, "http://localhost:11434"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ModelCatalogProvider");
  }

  @Test
  @DisplayName("Constructor throws NullPointerException on null coreProperties")
  void constructor_nullCoreProperties_throws() {
    assertThatThrownBy(
            () ->
                new ComplianceController(
                    dataSource, modelCatalogProvider, null, "http://localhost:11434"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("CoreProperties");
  }

  @Test
  @DisplayName("Constructor throws NullPointerException on null ollamaBaseUrl")
  void constructor_nullOllamaBaseUrl_throws() {
    assertThatThrownBy(
            () -> new ComplianceController(dataSource, modelCatalogProvider, coreProperties, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Ollama base URL");
  }

  @Test
  @DisplayName(
      "Sovereign-Ready when database and pgvector are active, Ollama is up, and default provider is"
          + " ollama")
  void health_sovereignReady_success() throws Exception {
    // DB & pgvector
    when(resultSet.next()).thenReturn(true);

    // Ollama
    var model = new OllamaModel("llama3.2:3b", "llama3.2:3b", "sha256");
    var catalog = new OllamaCatalog(List.of(model));
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        controller.getComplianceHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo("Sovereign-Ready");
    assertThat(body.pgVectorConnected()).isTrue();
    assertThat(body.pgVectorExtensionInstalled()).isTrue();
    assertThat(body.ollamaConnected()).isTrue();
    assertThat(body.ollamaModels()).containsExactly("llama3.2:3b");
    assertThat(body.defaultProvider()).isEqualTo("ollama");
    assertThat(body.ollamaBaseUrl()).isEqualTo("http://localhost:11434");
    assertThat(body.message()).contains("strictly sovereign");
  }

  @Test
  @DisplayName("Sovereign-Ready when using host.docker.internal alias")
  void health_hostDockerInternal_sovereignReady() throws Exception {
    ComplianceController dockerController =
        new ComplianceController(
            dataSource, modelCatalogProvider, coreProperties, "http://host.docker.internal:11434");

    // DB & pgvector
    when(resultSet.next()).thenReturn(true);

    // Ollama
    var catalog = new OllamaCatalog(List.of(new OllamaModel("llama3.2", "llama3.2", "sha")));
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        dockerController.getComplianceHealth();

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo("Sovereign-Ready");
  }

  @Test
  @DisplayName("Sovereign-Ready when using a private IP subnet (e.g. 192.168.1.100)")
  void health_privateIpSubnet_sovereignReady() throws Exception {
    ComplianceController privateController =
        new ComplianceController(
            dataSource, modelCatalogProvider, coreProperties, "http://192.168.1.100:11434");

    // DB & pgvector
    when(resultSet.next()).thenReturn(true);

    // Ollama
    var catalog = new OllamaCatalog(List.of(new OllamaModel("llama3.2", "llama3.2", "sha")));
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        privateController.getComplianceHealth();

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo("Sovereign-Ready");
  }

  @Test
  @DisplayName("Non-Compliant when using public IP address (e.g. 8.8.8.8)")
  void health_publicIpAddress_nonCompliant() throws Exception {
    ComplianceController publicController =
        new ComplianceController(
            dataSource, modelCatalogProvider, coreProperties, "http://8.8.8.8:11434");

    // DB & pgvector
    when(resultSet.next()).thenReturn(true);

    // Ollama
    var catalog = new OllamaCatalog(List.of(new OllamaModel("llama3.2", "llama3.2", "sha")));
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        publicController.getComplianceHealth();

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo("Non-Compliant");
    assertThat(response.getBody().message()).contains("resolved to a public network address");
  }

  @Test
  @DisplayName("Non-Compliant when database connection fails")
  void health_databaseOffline_nonCompliant() throws Exception {
    when(dataSource.getConnection()).thenThrow(new RuntimeException("DB offline"));

    // Ollama up
    var catalog = new OllamaCatalog(List.of(new OllamaModel("llama3.2:3b", "llama3.2:3b", "sha")));
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        controller.getComplianceHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo("Non-Compliant");
    assertThat(body.pgVectorConnected()).isFalse();
    assertThat(body.pgVectorExtensionInstalled()).isFalse();
    assertThat(body.message()).contains("Database offline");
  }

  @Test
  @DisplayName("Non-Compliant when pgvector extension is missing")
  void health_pgVectorMissing_nonCompliant() throws Exception {
    // Database connection succeeds but extname check yields no rows
    when(resultSet.next()).thenReturn(false);

    // Ollama up
    var catalog = new OllamaCatalog(List.of());
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        controller.getComplianceHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo("Non-Compliant");
    assertThat(body.pgVectorConnected()).isTrue();
    assertThat(body.pgVectorExtensionInstalled()).isFalse();
    assertThat(body.message()).contains("PGVector extension not installed");
  }

  @Test
  @DisplayName("Non-Compliant when Ollama is offline")
  void health_ollamaOffline_nonCompliant() throws Exception {
    // DB & pgvector up
    when(resultSet.next()).thenReturn(true);

    // Ollama down
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.empty());

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("ollama");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        controller.getComplianceHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo("Non-Compliant");
    assertThat(body.ollamaConnected()).isFalse();
    assertThat(body.message()).contains("Ollama service offline");
  }

  @Test
  @DisplayName("Non-Compliant when default provider is non-local (e.g. openai)")
  void health_nonLocalProvider_nonCompliant() throws Exception {
    // DB & pgvector
    when(resultSet.next()).thenReturn(true);

    // Ollama up
    var catalog = new OllamaCatalog(List.of());
    when(modelCatalogProvider.getCatalog()).thenReturn(Optional.of(catalog));

    // Core Properties
    when(coreProperties.defaultProvider()).thenReturn("openai");

    ResponseEntity<ComplianceController.ComplianceResponse> response =
        controller.getComplianceHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo("Non-Compliant");
    assertThat(body.message()).contains("Active AI provider is set to non-local value: 'openai'");
  }
}
