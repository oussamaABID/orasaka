package com.orasaka.core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityProperties")
class SecurityPropertiesTest {

  @Test
  @DisplayName("default constructor sets disableAi to false")
  void defaultConstructorDisableAiFalse() {
    SecurityProperties props = new SecurityProperties();
    assertThat(props.disableAi()).isFalse();
  }

  @Test
  @DisplayName("explicit constructor with true enables kill-switch")
  void explicitConstructorWithTrueEnablesKillSwitch() {
    SecurityProperties props = new SecurityProperties(true);
    assertThat(props.disableAi()).isTrue();
  }

  @Test
  @DisplayName("explicit constructor with false keeps kill-switch disabled")
  void explicitConstructorWithFalseKeepsKillSwitchDisabled() {
    SecurityProperties props = new SecurityProperties(false);
    assertThat(props.disableAi()).isFalse();
  }
}
