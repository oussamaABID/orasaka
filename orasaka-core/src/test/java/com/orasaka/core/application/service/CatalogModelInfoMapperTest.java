package com.orasaka.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import org.junit.jupiter.api.Test;
import static com.orasaka.test.TestConstants.*;

class CatalogModelInfoMapperTest {

  @Test
  void shouldMapToInfo() {
    CatalogModelDto dto = new CatalogModelDto(1, MODEL_M1, LABEL_L1, CATEGORY, OPTS, true, PROVIDER_OLLAMA);
    CatalogModelInfo info = CatalogModelInfoMapper.toInfo(dto);

    assertThat(info).isNotNull();
    assertThat(info.id()).isEqualTo(1);
    assertThat(info.modelName()).isEqualTo(MODEL_M1);
    assertThat(info.modelLabel()).isEqualTo(LABEL_L1);
    assertThat(info.category()).isEqualTo(CATEGORY);
    assertThat(info.options()).isEqualTo(OPTS);
    assertThat(info.isDefault()).isTrue();
    assertThat(info.providerName()).isEqualTo(PROVIDER_OLLAMA);
  }

  @Test
  void shouldMapToInfoNull() {
    assertThat(CatalogModelInfoMapper.toInfo(null)).isNull();
  }

  @Test
  void shouldMapToDto() {
    CatalogModelInfo info = new CatalogModelInfo(1, MODEL_M1, LABEL_L1, CATEGORY, OPTS, true, PROVIDER_OLLAMA);
    CatalogModelDto dto = CatalogModelInfoMapper.toDto(info);

    assertThat(dto).isNotNull();
    assertThat(dto.id()).isEqualTo(1);
    assertThat(dto.modelName()).isEqualTo(MODEL_M1);
    assertThat(dto.modelLabel()).isEqualTo(LABEL_L1);
    assertThat(dto.category()).isEqualTo(CATEGORY);
    assertThat(dto.options()).isEqualTo(OPTS);
    assertThat(dto.isDefault()).isTrue();
    assertThat(dto.providerName()).isEqualTo(PROVIDER_OLLAMA);
  }

  @Test
  void shouldMapToDtoNull() {
    assertThat(CatalogModelInfoMapper.toDto(null)).isNull();
  }
}
