package com.orasaka.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orasaka.core.domain.model.model.CatalogModelInfo;
import com.orasaka.persistence.domain.model.CatalogModelDto;
import org.junit.jupiter.api.Test;

class CatalogModelInfoMapperTest {

  @Test
  void shouldMapToInfo() {
    CatalogModelDto dto = new CatalogModelDto(1, "m1", "L1", "category", "opts", true, "ollama");
    CatalogModelInfo info = CatalogModelInfoMapper.toInfo(dto);

    assertThat(info).isNotNull();
    assertThat(info.id()).isEqualTo(1);
    assertThat(info.modelName()).isEqualTo("m1");
    assertThat(info.modelLabel()).isEqualTo("L1");
    assertThat(info.category()).isEqualTo("category");
    assertThat(info.options()).isEqualTo("opts");
    assertThat(info.isDefault()).isTrue();
    assertThat(info.providerName()).isEqualTo("ollama");
  }

  @Test
  void shouldMapToInfoNull() {
    assertThat(CatalogModelInfoMapper.toInfo(null)).isNull();
  }

  @Test
  void shouldMapToDto() {
    CatalogModelInfo info = new CatalogModelInfo(1, "m1", "L1", "category", "opts", true, "ollama");
    CatalogModelDto dto = CatalogModelInfoMapper.toDto(info);

    assertThat(dto).isNotNull();
    assertThat(dto.id()).isEqualTo(1);
    assertThat(dto.modelName()).isEqualTo("m1");
    assertThat(dto.modelLabel()).isEqualTo("L1");
    assertThat(dto.category()).isEqualTo("category");
    assertThat(dto.options()).isEqualTo("opts");
    assertThat(dto.isDefault()).isTrue();
    assertThat(dto.providerName()).isEqualTo("ollama");
  }

  @Test
  void shouldMapToDtoNull() {
    assertThat(CatalogModelInfoMapper.toDto(null)).isNull();
  }
}
