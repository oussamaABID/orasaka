package com.orasaka.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import com.orasaka.persistence.domain.ports.inbound.CatalogModelManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogModelServiceImplTest {

  @Mock private CatalogModelManager catalogModelManager;

  private CatalogModelServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CatalogModelServiceImpl(catalogModelManager);
  }

  @Test
  void constructorShouldThrowOnNullManager() {
    assertThatThrownBy(() -> new CatalogModelServiceImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("CatalogModelManager cannot be null");
  }

  @Test
  void shouldGetAllModels() {
    CatalogModelDto dto = new CatalogModelDto(1, "m1", "L1", "category", "opts", true, "ollama");
    when(catalogModelManager.getAllModels()).thenReturn(List.of(dto));

    List<CatalogModelInfo> result = service.getAllModels();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).modelName()).isEqualTo("m1");
    verify(catalogModelManager).getAllModels();
  }

  @Test
  void shouldGetModelsByCategory() {
    CatalogModelDto dto = new CatalogModelDto(1, "m1", "L1", "category", "opts", true, "ollama");
    when(catalogModelManager.getModelsByCategory("category")).thenReturn(List.of(dto));

    List<CatalogModelInfo> result = service.getModelsByCategory("category");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).category()).isEqualTo("category");
    verify(catalogModelManager).getModelsByCategory("category");
  }

  @Test
  void shouldGetDefaultModelByCategory() {
    CatalogModelDto dto = new CatalogModelDto(1, "m1", "L1", "category", "opts", true, "ollama");
    when(catalogModelManager.getDefaultModelByCategory("category")).thenReturn(Optional.of(dto));

    Optional<CatalogModelInfo> result = service.getDefaultModelByCategory("category");

    assertThat(result).isPresent();
    assertThat(result.get().isDefault()).isTrue();
    verify(catalogModelManager).getDefaultModelByCategory("category");
  }

  @Test
  void shouldSaveModel() {
    CatalogModelInfo info = new CatalogModelInfo(1, "m1", "L1", "category", "opts", true, "ollama");
    CatalogModelDto dto = new CatalogModelDto(1, "m1", "L1", "category", "opts", true, "ollama");
    when(catalogModelManager.saveModel(dto)).thenReturn(dto);

    CatalogModelInfo result = service.saveModel(info);

    assertThat(result.modelName()).isEqualTo("m1");
    verify(catalogModelManager).saveModel(dto);
  }

  @Test
  void shouldDeleteModel() {
    service.deleteModel(1);
    verify(catalogModelManager).deleteModel(1);
  }
}
