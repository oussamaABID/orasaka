package com.orasaka.persistence.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit boundary enforcement for the orasaka-persistence-app module. Validates that persistence
 * remains isolated and package topology rules are strictly followed.
 */
class PersistenceBoundaryTest {

  private static JavaClasses persistenceClasses;

  @BeforeAll
  static void importClasses() {
    persistenceClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.persistence");
  }

  @Test
  @DisplayName("[ERR-102] Persistence must not depend on gateway")
  void persistenceDoesNotDependOnGateway() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.gateway..")
        .because("Persistence is independent of the gateway module")
        .check(persistenceClasses);
  }

  @Test
  @DisplayName("[ERR-102] Persistence must not depend on core")
  void persistenceDoesNotDependOnCore() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.core..")
        .because("Persistence is independent of the core AI module")
        .check(persistenceClasses);
  }

  @Test
  @DisplayName("[ERR-102] Persistence must not depend on identity")
  void persistenceDoesNotDependOnIdentity() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.persistence..")
        .and()
        .haveSimpleNameNotEndingWith("EventAdapter")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .because(
            "Persistence must remain decoupled from identity domains, except for EventAdapters")
        .check(persistenceClasses);
  }

  @Test
  @DisplayName("[ERR-102] Provider implementations must not be public")
  void providerImplNotPublic() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.persistence.application.service..")
        .and()
        .haveSimpleNameEndingWith("Impl")
        .should()
        .notBePublic()
        .because("Provider implementations are package-private — only interfaces are public")
        .check(persistenceClasses);
  }

  @Test
  @DisplayName("[ERR-107] Mapper classes must not be public")
  void mapperNotPublic() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.persistence.application.service..")
        .and()
        .haveSimpleNameEndingWith("Mapper")
        .should()
        .notBePublic()
        .because("Mapper utilities are internal package-private details")
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
