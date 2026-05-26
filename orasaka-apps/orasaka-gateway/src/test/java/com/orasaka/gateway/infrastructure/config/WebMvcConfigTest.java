package com.orasaka.gateway.infrastructure.config;

import static org.mockito.Mockito.*;

import java.util.List;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@ExtendWith(MockitoExtension.class)
class WebMvcConfigTest {

  private GatewayProperties gatewayProperties;
  private GraphQlCorsProperties corsProperties;
  private WebMvcConfig webMvcConfig;

  @BeforeEach
  void setUp() {
    gatewayProperties =
        new GatewayProperties(
            new GatewayProperties.UploadsConfig("var/orasaka-uploads", "/uploads/**", 3600),
            new GatewayProperties.AsyncConfig(30000));
    corsProperties =
        new GraphQlCorsProperties(
            List.of("http://localhost:3000"), List.of("GET"), List.of("*"), false);
    webMvcConfig = new WebMvcConfig(gatewayProperties, corsProperties);
  }

  @Test
  void configureAsyncSupport_setsDefaultTimeout() {
    AsyncSupportConfigurer configurer = mock(AsyncSupportConfigurer.class);
    webMvcConfig.configureAsyncSupport(configurer);
    verify(configurer).setDefaultTimeout(30000);
  }

  @Test
  void addResourceHandlers_registersUploadsPath() {
    ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
    ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

    when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);
    when(registration.addResourceLocations(anyString())).thenReturn(registration);

    webMvcConfig.addResourceHandlers(registry);

    verify(registry).addResourceHandler("/uploads/**");
    verify(registration).addResourceLocations(contains("var/orasaka-uploads"));
    verify(registration).setCachePeriod(3600);
  }

  @Test
  void addCorsMappings_addsUploadsCors() {
    CorsRegistry registry = mock(CorsRegistry.class);
    CorsRegistration registration = mock(CorsRegistration.class);

    when(registry.addMapping("/uploads/**")).thenReturn(registration);
    when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
    when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
    when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
    when(registration.exposedHeaders(any(String[].class))).thenReturn(registration);
    when(registration.allowCredentials(false)).thenReturn(registration);

    webMvcConfig.addCorsMappings(registry);

    verify(registry).addMapping("/uploads/**");
    verify(registration).allowedOrigins("http://localhost:3000");
    verify(registration).allowedMethods("GET", "OPTIONS");
    verify(registration).allowCredentials(false);
  }

  @Test
  void webServerFactoryCustomizer_configuresMimeTypes() {
    var customizer = webMvcConfig.webServerFactoryCustomizer();
    ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);

    customizer.customize(factory);

    verify(factory).setMimeMappings(any());
  }

  @Test
  void webServerFactoryCustomizer_withTomcatFactory_configuresAsyncTimeout() {
    var customizer = webMvcConfig.webServerFactoryCustomizer();
    TomcatServletWebServerFactory tomcatFactory = mock(TomcatServletWebServerFactory.class);
    Connector connector = mock(Connector.class);

    customizer.customize(tomcatFactory);

    // Verify connector customizers are added
    ArgumentCaptor<TomcatConnectorCustomizer> captor =
        ArgumentCaptor.forClass(TomcatConnectorCustomizer.class);
    verify(tomcatFactory).addConnectorCustomizers(captor.capture());

    // Execute the customizer and verify timeout is set
    captor.getValue().customize(connector);
    verify(connector).setAsyncTimeout(30000);
  }
}
