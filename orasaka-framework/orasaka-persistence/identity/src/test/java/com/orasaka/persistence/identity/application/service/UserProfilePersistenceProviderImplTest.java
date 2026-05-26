package com.orasaka.persistence.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.orasaka.persistence.identity.domain.model.UserProfileDto;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.entity.UserProfileEntity;
import com.orasaka.persistence.identity.infrastructure.adapter.persistence.repository.UserProfileRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfilePersistenceProviderImplTest {

  @Mock private UserProfileRepository repository;

  @InjectMocks private UserProfilePersistenceProviderImpl provider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserProfilePersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserProfileRepository cannot be null");
  }

  @Test
  @DisplayName("findByUserId retrieves and maps UserProfileEntity")
  void findByUserId() {
    UserProfileEntity entity = new UserProfileEntity();
    entity.setUserId("user-1");
    entity.setTheme("dark");
    entity.setVoiceModel("shimmer");
    entity.setPrimaryIndustry("finance");
    entity.setAiBehavior("creative");
    entity.setRawPreferences(Collections.emptyMap());

    when(repository.findById("user-1")).thenReturn(Optional.of(entity));

    Optional<UserProfileDto> result = provider.findByUserId("user-1");

    assertThat(result).isPresent();
    assertThat(result.get().userId()).isEqualTo("user-1");
    assertThat(result.get().theme()).isEqualTo("dark");
    assertThat(result.get().voiceModel()).isEqualTo("shimmer");
    assertThat(result.get().primaryIndustry()).isEqualTo("finance");
    assertThat(result.get().aiBehavior()).isEqualTo("creative");

    verify(repository).findById("user-1");
  }

  @Test
  @DisplayName("findByUserId throws NullPointerException on null userId")
  void findByUserIdNull() {
    assertThatThrownBy(() -> provider.findByUserId(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");
  }

  @Test
  @DisplayName("save persists and maps UserProfileDto")
  void save() {
    UserProfileDto dto =
        new UserProfileDto(
            "user-1", "dark", "shimmer", "finance", "creative", Collections.emptyMap());
    UserProfileEntity entity = UserProfilePersistenceMapper.toEntity(dto);

    when(repository.save(any(UserProfileEntity.class))).thenReturn(entity);

    UserProfileDto result = provider.save(dto);

    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo("user-1");
    assertThat(result.theme()).isEqualTo("dark");
    assertThat(result.voiceModel()).isEqualTo("shimmer");
    assertThat(result.primaryIndustry()).isEqualTo("finance");
    assertThat(result.aiBehavior()).isEqualTo("creative");

    verify(repository).save(any(UserProfileEntity.class));
  }

  @Test
  @DisplayName("save throws NullPointerException on null DTO")
  void saveNull() {
    assertThatThrownBy(() -> provider.save(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserProfileDto cannot be null");
  }
}
