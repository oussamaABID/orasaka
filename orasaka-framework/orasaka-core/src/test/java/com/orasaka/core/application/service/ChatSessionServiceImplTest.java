package com.orasaka.core.application.service;

import static com.orasaka.test.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orasaka.core.domain.model.chat.ChatSessionInfo;
import com.orasaka.persistence.domain.model.ChatSessionDto;
import com.orasaka.persistence.domain.ports.inbound.ChatSessionPersistenceProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceImplTest {

  @Mock private ChatSessionPersistenceProvider provider;

  private ChatSessionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ChatSessionServiceImpl(provider);
  }

  @Test
  void constructorShouldThrowOnNullProvider() {
    assertThatThrownBy(() -> new ChatSessionServiceImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ChatSessionPersistenceProvider cannot be null");
  }

  @Test
  void shouldSaveSession() {
    Instant now = Instant.now();
    ChatSessionInfo info = new ChatSessionInfo(S1, U1, TITLE, now);
    ChatSessionDto dto = new ChatSessionDto(S1, U1, TITLE, now);
    when(provider.save(dto)).thenReturn(dto);

    ChatSessionInfo result = service.save(info);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(S1);
    verify(provider).save(dto);
  }

  @Test
  void shouldGetSession() {
    Instant now = Instant.now();
    ChatSessionDto dto = new ChatSessionDto(S1, U1, TITLE, now);
    when(provider.findById(S1)).thenReturn(Optional.of(dto));

    Optional<ChatSessionInfo> result = service.getSession(S1);

    assertThat(result).isPresent();
    assertThat(result.get().title()).isEqualTo(TITLE);
    verify(provider).findById(S1);
  }

  @Test
  void shouldGetSessionsByUserId() {
    Instant now = Instant.now();
    ChatSessionDto dto = new ChatSessionDto(S1, U1, TITLE, now);
    when(provider.findAllByUserId(U1)).thenReturn(List.of(dto));

    List<ChatSessionInfo> result = service.getSessionsByUserId(U1);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).userId()).isEqualTo(U1);
    verify(provider).findAllByUserId(U1);
  }

  @Test
  void shouldDeleteSession() {
    service.deleteSession(S1);
    verify(provider).deleteById(S1);
  }

  @Test
  void shouldPurgeSessionsByUserId() {
    service.purgeSessionsByUserId(U1);
    verify(provider).purgeByUserId(U1);
  }
}
