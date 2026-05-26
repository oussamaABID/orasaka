package com.orasaka.persistence.identity.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit boundary enforcement for the orasaka-persistence-identity module. Validates that
 * persistence remains isolated and package topology rules are strictly followed.
 */
class PersistenceIdentityBoundaryTest {

  private static JavaClasses persistenceClasses;

  @BeforeAll
  static void importClasses() {
    persistenceClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.persistence.identity");
  }

  @Test
  @DisplayName("[ERR-102] Identity Persistence must not depend on gateway")
  void persistenceDoesNotDependOnGateway() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.persistence.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.gateway..")
        .because("Identity Persistence is independent of the gateway module")
        .check(persistenceClasses);
  }

  @Test
  @DisplayName("[ERR-102] Identity Persistence must not depend on core")
  void persistenceDoesNotDependOnCore() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.persistence.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.core..")
        .because("Identity Persistence is independent of the core AI module")
        .check(persistenceClasses);
  }

  @Test
  @DisplayName("[ERR-102] Identity Persistence must not depend on identity domain")
  void persistenceDoesNotDependOnIdentity() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.persistence.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .because("Identity Persistence must remain decoupled from identity domain models and ports")
        .check(persistenceClasses);
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
        .because(
            "JPA AttributeConverters must live in the infrastructure.adapter.persistence.converter package")
        .check(persistenceClasses);

    classes()
        .that()
        .areAnnotatedWith(jakarta.persistence.Entity.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.entity..")
        .because(
            "JPA Entity classes must live in the infrastructure.adapter.persistence.entity package")
        .check(persistenceClasses);

    classes()
        .that()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.repository..")
        .because(
            "Spring Data Repositories must live in the infrastructure.adapter.persistence.repository package")
        .check(persistenceClasses);
  }
}
