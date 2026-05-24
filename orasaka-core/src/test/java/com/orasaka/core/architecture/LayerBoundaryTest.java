package com.orasaka.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enforces ADR-005 and Section 1.A: orasaka-core must remain 100% agnostic of gateway, identity,
 * web protocols, servlet infrastructure, and Spring Boot auto-configuration starters.
 */
class LayerBoundaryTest {

  private static JavaClasses coreClasses;

  @BeforeAll
  static void importClasses() {
    coreClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.core");
  }

  @Test
  @DisplayName("[Gate-1] Core must not depend on gateway layer")
  void coreMustNotDependOnGateway() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.gateway..")
        .because("orasaka-core is a stateless library and must remain gateway-agnostic [ADR-005]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-1] Core must not depend on identity layer")
  void coreMustNotDependOnIdentity() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .because("orasaka-core must not leak identity domain types [Section 1.A]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-1] Core must not depend on Servlet API")
  void coreMustNotDependOnServletApi() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("jakarta.servlet..")
        .because("orasaka-core must remain HTTP/protocol agnostic [Section 1.A]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-1] Core engine must not depend on Spring Web (infrastructure exempt)")
  void coreMustNotDependOnSpringWeb() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .and()
        .resideOutsideOfPackage("com.orasaka.core.infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.web..")
        .because(
            "orasaka-core must remain web-agnostic [Section 1.A]."
                + " Infrastructure adapters (RestClient wrappers) are exempt.")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-1] Core must not depend on Spring Boot AutoConfiguration")
  void coreMustNotDependOnAutoConfiguration() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.boot.autoconfigure..")
        .because(
            "orasaka-core must not import Spring Boot starter auto-configuration [Section 1.A]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-DI] Core classes must not use @Autowired on fields")
  void noFieldInjection() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .because(
            "Field injection is prohibited. Constructor-based DI is mandatory [ADR-012, AGENTS.md]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-2] Engine must never directly depend on infrastructure")
  void engineNeverTouchesInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core.engine..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.core.infrastructure..")
        .because(
            "Engine must remain media-agnostic and may only consume ingest ports [ERR-101, Section 1.A]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[Gate-2] Engine is media-agnostic — depends only on ingest ports, not infra")
  void engineIsMediaAgnostic() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core.engine..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.orasaka.core.infrastructure..", "javax.sound..", "java.awt.image..")
        .because(
            "Engine must be blind to concrete infrastructure tools and media libraries [ERR-101]")
        .check(coreClasses);
  }
}
