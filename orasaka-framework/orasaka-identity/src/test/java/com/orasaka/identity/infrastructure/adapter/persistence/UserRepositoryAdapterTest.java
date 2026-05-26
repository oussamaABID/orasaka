package com.orasaka.identity.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.orasaka.identity.domain.model.Persona;
import com.orasaka.identity.domain.model.User;
import com.orasaka.identity.domain.model.UserSecurityInfo;
import com.orasaka.persistence.identity.domain.model.UserDto;
import com.orasaka.persistence.identity.domain.ports.UserPersistenceProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

  @Mock private UserPersistenceProvider provider;

  @InjectMocks private UserRepositoryAdapter adapter;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserRepositoryAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserPersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("findById retrieves and maps UserDto")
  void findById() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findById(user.id().toString())).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findById(user.id().toString());

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("findByEmailAndEnabledTrue retrieves and maps UserDto")
  void findByEmailAndEnabledTrue() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByEmailAndEnabledTrue(user.email())).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findByEmailAndEnabledTrue(user.email());

    assertThat(result).isPresent();
    assertThat(result.get().email()).isEqualTo(user.email());
  }

  @Test
  @DisplayName("findByProviderAndProviderId retrieves and maps UserDto")
  void findByProviderAndProviderId() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByProviderAndProviderId("google", "google-id")).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findByProviderAndProviderId("google", "google-id");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("findSecurityInfoByEmail retrieves and returns UserSecurityInfo")
  void findSecurityInfoByEmail() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByEmailAndEnabledTrue(user.email())).thenReturn(Optional.of(dto));

    Optional<UserSecurityInfo> result = adapter.findSecurityInfoByEmail(user.email());

    assertThat(result).isPresent();
    assertThat(result.get().passwordHash()).isEqualTo("hash");
    assertThat(result.get().user().id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("save updates existing user")
  void save() {
    User user = Persona.freeUser();
    UserDto existing =
        new UserDto(
            user.id().toString(),
            user.username(),
            "password-hash",
            user.email(),
            user.enabled(),
            Map.of(),
            Set.of("ROLE_USER"),
            List.of(),
            "local",
            "provider-id",
            "free",
            Instant.now());

    when(provider.findById(user.id().toString())).thenReturn(Optional.of(existing));
    when(provider.save(any(UserDto.class))).thenReturn(existing);

    User result = adapter.save(user);

    assertThat(result).isNotNull();
    verify(provider)
        .save(
            argThat(
                dto ->
                    dto.passwordHash().equals("password-hash") && dto.provider().equals("local")));
  }

  @Test
  @DisplayName("save throws IllegalArgumentException when user not found")
  void saveNotFound() {
    User user = Persona.freeUser();
    when(provider.findById(user.id().toString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.save(user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found:");
  }

  @Test
  @DisplayName("create creates a new user")
  void create() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.create(any(UserDto.class), eq("hash"))).thenReturn(dto);

    User result = adapter.create(user, "hash");

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("updatePasswordHashByEmail delegates to provider")
  void updatePassword() {
    adapter.updatePasswordHashByEmail("test@test.com", "new-hash");

    verify(provider).updatePasswordHashByEmail("test@test.com", "new-hash");
  }

  @Test
  @DisplayName("findByEmail retrieves and maps UserDto")
  void findByEmail() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByEmail(user.email())).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findByEmail(user.email());

    assertThat(result).isPresent();
    assertThat(result.get().email()).isEqualTo(user.email());
  }
}
