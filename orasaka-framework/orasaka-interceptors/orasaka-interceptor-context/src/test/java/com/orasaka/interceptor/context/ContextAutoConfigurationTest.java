package com.orasaka.interceptor.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.orasaka.core.infrastructure.config.CoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContextAutoConfigurationTest {

  @Test
  @DisplayName(
      "ContextAutoConfiguration registers UserContextResolver, UserContextInterceptor, and SystemContextInjector")
  void registersBeans() {
    ContextAutoConfiguration config = new ContextAutoConfiguration();
    CoreProperties props = mock(CoreProperties.class);

    UserContextResolver resolver = config.userContextResolver();
    UserContextInterceptor interceptor = config.userContextInterceptor(props);
    SystemContextInjector injector = config.systemContextInjector();

    assertThat(resolver).isNotNull();
    assertThat(interceptor).isNotNull();
    assertThat(injector).isNotNull();
  }
}
