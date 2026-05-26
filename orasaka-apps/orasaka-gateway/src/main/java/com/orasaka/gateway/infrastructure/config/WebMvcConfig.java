package com.orasaka.gateway.infrastructure.config;

import com.orasaka.gateway.infrastructure.support.PathResolver;
import java.nio.file.Path;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration exposing standard static resources mapping for user uploaded and
 * AI-generated media files stored inside the centralized {@code var/orasaka-uploads/} monorepo
 * space.
 */
@Configuration
class WebMvcConfig implements WebMvcConfigurer {

  private final GatewayProperties gatewayProperties;
  private final GraphQlCorsProperties corsProperties;

  WebMvcConfig(GatewayProperties gatewayProperties, GraphQlCorsProperties corsProperties) {
    this.gatewayProperties = gatewayProperties;
    this.corsProperties = corsProperties;
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setDefaultTimeout(gatewayProperties.async().timeoutMs());
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path uploadDir = PathResolver.resolve(gatewayProperties.uploads().directory());
    registry
        .addResourceHandler(gatewayProperties.uploads().handlerPath())
        .addResourceLocations("file:" + uploadDir + "/")
        .setCachePeriod(gatewayProperties.uploads().cachePeriod());
  }

  /**
   * CORS for static uploads — restricted to the same origins as the GraphQL API. Credentials are
   * disabled; only safe read methods (GET/OPTIONS) are allowed.
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping(gatewayProperties.uploads().handlerPath())
        .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Access-Control-Allow-Origin")
        .allowCredentials(false);
  }

  @Bean
  public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
      webServerFactoryCustomizer() {
    return factory -> {
      MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
      mappings.add("mp4", "video/mp4");
      mappings.add("png", "image/png");
      factory.setMimeMappings(mappings);
      if (factory instanceof TomcatServletWebServerFactory tomcatFactory) {
        tomcatFactory.addConnectorCustomizers(
            connector -> connector.setAsyncTimeout(gatewayProperties.async().timeoutMs()));
      }
    };
  }
}
