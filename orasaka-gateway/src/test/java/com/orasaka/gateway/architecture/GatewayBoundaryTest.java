package com.orasaka.gateway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Gateway-specific architectural boundary tests. Enforces that gateway infrastructure classes
 * maintain proper encapsulation and that Spring AI types do not leak into the gateway layer.
 */
class GatewayBoundaryTest {

  private static JavaClasses gatewayClasses;

  @BeforeAll
  static void importClasses() {
    gatewayClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.gateway");
  }

  @Test
  @DisplayName("[ERR-110] Config infrastructure classes must not be public")
  void configClassesMustBePackagePrivate() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.gateway.config..")
        .and()
        .areNotInterfaces()
        .and()
        .areNotAnnotatedWith(org.springframework.context.annotation.Configuration.class)
        .and()
        .haveSimpleNameNotEndingWith("Properties")
        .should()
        .notBePublic()
        .because(
            "Infrastructure filters and request wrappers must be package-private [ERR-110, ADR-009]."
                + " @Configuration and *Properties classes are exempt (Spring Binder requirement).")
        .check(gatewayClasses);
  }

  @Test
  @DisplayName("[Section 1.A] Gateway must not depend on Spring AI framework types")
  void gatewayMustNotDependOnSpringAi() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.gateway..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.ai..")
        .because(
            "Spring AI types must remain encapsulated inside orasaka-core."
                + " Gateway interacts only through AiClient facade [Section 1.A, ADR-005]")
        .check(gatewayClasses);
  }

  @Test
  @DisplayName("[Gate-DI] Gateway classes must not use @Autowired on fields")
  void noFieldInjection() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("com.orasaka.gateway..")
        .should()
        .beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .because(
            "Field injection is prohibited. Constructor-based DI is mandatory [ADR-012, AGENTS.md]")
        .check(gatewayClasses);
  }

  @Test
  @DisplayName("[ERR-106] Gateway DTO package must not depend on identity domain types")
  void gatewayDtoIsDomainBlind() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.gateway.dto..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity.domain..")
        .because(
            "DTO layer must be domain-blind — domain-to-DTO mapping happens at factory boundary [ERR-106]")
        .check(gatewayClasses);
  }

  @Test
  @DisplayName("[ERR-102] Gateway endpoints must not import identity entities")
  void gatewayEndpointsDoNotLeakEntities() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.gateway.endpoint..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity.entity..")
        .because(
            "Endpoints must interact with identity only through service interfaces and DTOs [ERR-102]")
        .check(gatewayClasses);
  }
}
