package com.orasaka.identity.architecture;

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
 * ArchUnit boundary enforcement for the orasaka-identity module.
 *
 * <p>Validates that identity remains a pure domain hexagon with no outward dependencies on
 * gateway, core, or tools modules (ERR-102, §1.A).
 */
class IdentityBoundaryTest {

  private static JavaClasses identityClasses;

  @BeforeAll
  static void importClasses() {
    identityClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.identity");
  }

  @Test
  @DisplayName("[ERR-102] Identity must not depend on gateway")
  void identityDoesNotDependOnGateway() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.gateway..")
        .because("Identity is a pure domain hexagon — gateway imports are forbidden [ERR-102]")
        .check(identityClasses);
  }

  @Test
  @DisplayName("[ERR-102] Identity must not depend on core")
  void identityDoesNotDependOnCore() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.core..")
        .because("Identity must remain decoupled from the AI core engine [ERR-102]")
        .check(identityClasses);
  }

  @Test
  @DisplayName("[ERR-102] Identity must not depend on tools")
  void identityDoesNotDependOnTools() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.tools..")
        .because("Identity must remain decoupled from the tools module [ERR-102]")
        .check(identityClasses);
  }

  @Test
  @DisplayName("[ERR-106] No field injection in identity classes")
  void noFieldInjection() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .should()
        .beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .because("Field injection is prohibited — constructor-based DI is mandatory [ADR-012]")
        .check(identityClasses);
  }

  @Test
  @DisplayName("[ERR-102] Service implementations must not be public")
  void serviceImplNotPublic() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.identity.service..")
        .and()
        .haveSimpleNameEndingWith("Impl")
        .should()
        .notBePublic()
        .because(
            "Service implementations are package-private — only interfaces are public [Ports & Adapters]")
        .check(identityClasses);
  }
}
