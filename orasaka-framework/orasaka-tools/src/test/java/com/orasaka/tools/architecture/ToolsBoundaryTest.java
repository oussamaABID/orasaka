package com.orasaka.tools.architecture;

import static com.orasaka.test.TestConstants.*;
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
 * ArchUnit boundary enforcement for the orasaka-tools module.
 *
 * <p>Validates that tools remain identity-blind and gateway-blind, communicating only through
 * core-defined ports (ERR-102, §1.A). Tools may implement interfaces defined in {@code
 * orasaka-core.pipeline.*} but must never import from identity or gateway.
 */
class ToolsBoundaryTest {

  private static JavaClasses toolsClasses;

  @BeforeAll
  static void importClasses() {
    toolsClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.tools");
  }

  @Test
  @DisplayName("[ERR-102] Tools must not depend on identity")
  void toolsAreIdentityBlind() {
    noClasses()
        .that()
        .resideInAPackage(PKG_TOOLS)
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .because("Tools are identity-blind — absolute ban on importing identity types [ERR-102]")
        .check(toolsClasses);
  }

  @Test
  @DisplayName("[ERR-102] Tools must not depend on gateway")
  void toolsAreGatewayBlind() {
    noClasses()
        .that()
        .resideInAPackage(PKG_TOOLS)
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.gateway..")
        .because("Tools are gateway-blind — absolute ban on importing gateway types [ERR-102]")
        .check(toolsClasses);
  }

  @Test
  @DisplayName("[ERR-106] No field injection in tools classes")
  void noFieldInjectionInTools() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage(PKG_TOOLS)
        .should()
        .beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .because(
            "Field injection is prohibited — constructor-based DI is mandatory [ADR-012, ERR-106]")
        .check(toolsClasses);
  }

  @Test
  @DisplayName(
      "[ERR-109] Persistence components must reside strictly under infrastructure.adapter.persistence package hierarchy")
  void persistenceComponentsPackageHygiene() {
    classes()
        .that()
        .implement(jakarta.persistence.AttributeConverter.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.converter..")
        .allowEmptyShould(true)
        .because(
            "JPA AttributeConverters must live in the infrastructure.adapter.persistence.converter package")
        .check(toolsClasses);

    classes()
        .that()
        .areAnnotatedWith(jakarta.persistence.Entity.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.entity..")
        .because(
            "JPA Entity classes must live in the infrastructure.adapter.persistence.entity package")
        .check(toolsClasses);

    classes()
        .that()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.repository..")
        .because(
            "Spring Data Repositories must live in the infrastructure.adapter.persistence.repository package")
        .check(toolsClasses);
  }

  @Test
  @DisplayName("[ERR-109] Entity packages must not depend on outer service layers")
  void entitiesDoNotDependOnOuterLayers() {
    noClasses()
        .that()
        .resideInAPackage("..infrastructure.adapter.persistence.entity..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.orasaka.tools.application.service..", "com.orasaka.gateway..")
        .because("Entities must not import or depend on outer service or gateway layers [ERR-109]")
        .check(toolsClasses);
  }
}
