package com.orasaka.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared ArchUnit rule library for governance enforcement across all Orasaka modules.
 *
 * <p>Contains exclusively ArchUnit-based rules operating on {@link JavaClasses}. For file-system
 * source scanning utilities, see {@link SourceFileScanner}.
 */
public final class GovernanceRules {

  private static final String MAPPER_SUFFIX = "Mapper";
  private static final String DOMAIN_LAYER = "Domain";
  private static final String APPLICATION_LAYER = "Application";
  private static final String APPLICATION_PKG_PATTERN = ".application..";

  private GovernanceRules() {}

  // ─── Class Import Helpers ───

  /** Imports production classes for the given base package (excludes test sources). */
  public static JavaClasses importProductionClasses(String basePackage) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(basePackage);
  }

  /** Imports all classes (production + test) for the given base package. */
  public static JavaClasses importAllClasses(String basePackage) {
    return new ClassFileImporter().importPackages(basePackage);
  }

  // ─── ERR-102: Module Boundary Enforcement ───

  /** Asserts that classes in {@code modulePackage} do not depend on {@code forbiddenPackage}. */
  public static void assertNoDependencyOn(
      JavaClasses classes, String modulePackage, String forbiddenPackage, String reason) {
    noClasses()
        .that()
        .resideInAPackage(modulePackage + "..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(forbiddenPackage + "..")
        .because(reason + " [ERR-102]")
        .check(classes);
  }

  // ─── ERR-103: One Top-Level Class Per File ───

  /** Asserts every top-level class resides in a dedicated file matching its simple name. */
  public static void assertOneTopLevelClassPerFile(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .doNotHaveModifier(JavaModifier.SYNTHETIC)
        .should(
            new ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                "reside in a dedicated file matching their class name") {
              @Override
              public void check(
                  com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
                if (javaClass.isAnonymousClass() || javaClass.isMemberClass()) {
                  return;
                }
                String sourceFileName =
                    javaClass.getSource().flatMap(Source::getFileName).orElse(null);
                if (sourceFileName != null && !sourceFileName.equals("Unknown Source")) {
                  String expectedFileName = javaClass.getSimpleName() + ".java";
                  if (!sourceFileName.equals(expectedFileName)) {
                    events.add(
                        SimpleConditionEvent.violated(
                            javaClass,
                            String.format(
                                "Architecture Violation [ERR-103]: Class '%s' is illegally bundled"
                                    + " inside '%s'.",
                                javaClass.getFullName(), sourceFileName)));
                  }
                }
              }
            })
        .because("Every top-level class must reside in its own dedicated .java file [ERR-103]")
        .check(classes);
  }

  // ─── ERR-104: No Redundant Project Prefix ───

  /** Asserts no class names start with 'Orasaka' prefix. */
  public static void assertNoRedundantPrefix(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .should()
        .haveSimpleNameNotStartingWith("Orasaka")
        .because("Project name must not be prepended to class names [ERR-104]")
        .check(classes);
  }

  // ─── ERR-105: *Impl Package-Private Visibility ───

  /** Asserts *Impl classes in the given service package are not public. */
  public static void assertImplClassesPackagePrivate(JavaClasses classes, String servicePackage) {
    classes()
        .that()
        .resideInAPackage(servicePackage + "..")
        .and()
        .haveSimpleNameEndingWith("Impl")
        .should()
        .notBePublic()
        .because(
            "Implementation classes (*Impl) must be package-private."
                + " Cross-module interaction through interfaces only [ERR-105]")
        .check(classes);
  }

  // ─── ERR-107: Mapper Visibility ───

  /** Asserts *Mapper classes in the given package are final. */
  public static void assertMappersFinal(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .haveSimpleNameEndingWith(MAPPER_SUFFIX)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .because("Mapper classes must be final static utility classes [ERR-107]")
        .check(classes);
  }

  /** Asserts *Mapper classes in the given package are not public. */
  public static void assertMappersPackagePrivate(JavaClasses classes, String servicePackage) {
    classes()
        .that()
        .resideInAPackage(servicePackage + "..")
        .and()
        .haveSimpleNameEndingWith(MAPPER_SUFFIX)
        .should()
        .notBePublic()
        .because("Mapper utilities are internal package-private details [ERR-107]")
        .check(classes);
  }

  // ─── ERR-109: Persistence Package Hygiene ───

  /** Asserts JPA components reside in correct sub-packages. */
  public static void assertPersistencePackageHygiene(JavaClasses classes) {
    classes()
        .that()
        .implement(jakarta.persistence.AttributeConverter.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.converter..")
        .allowEmptyShould(true)
        .because("JPA AttributeConverters must live in .converter package [ERR-109]")
        .check(classes);

    classes()
        .that()
        .areAnnotatedWith(jakarta.persistence.Entity.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.entity..")
        .because("JPA Entity classes must live in .entity package [ERR-109]")
        .check(classes);

    classes()
        .that()
        .areAssignableTo(org.springframework.data.repository.Repository.class)
        .should()
        .resideInAPackage("..infrastructure.adapter.persistence.repository..")
        .because("Spring Data Repositories must live in .repository package [ERR-109]")
        .check(classes);
  }

  // ─── ADR-007: Collection Field Immutability ───

  /** Asserts collection fields (List, Map, Set) in non-record classes are private final. */
  public static void assertCollectionFieldsPrivateFinal(JavaClasses classes) {
    fields()
        .that()
        .haveRawType(List.class)
        .or()
        .haveRawType(Map.class)
        .or()
        .haveRawType(Set.class)
        .and()
        .areDeclaredInClassesThat()
        .areNotRecords()
        .and()
        .areDeclaredInClassesThat()
        .areNotEnums()
        .and()
        .areDeclaredInClassesThat()
        .areNotInterfaces()
        .and()
        .areDeclaredInClassesThat()
        .haveSimpleNameNotContaining("Abstract")
        .should(bePrivateAndFinal())
        .because("Collection fields must be private final for immutability [ADR-007, ADR-008]")
        .check(classes);
  }

  // ─── ADR-009: Fields Must Be Private ───

  /** Asserts instance fields in concrete non-record classes are private. */
  public static void assertFieldsPrivate(JavaClasses classes) {
    fields()
        .that()
        .areDeclaredInClassesThat()
        .areNotRecords()
        .and()
        .areDeclaredInClassesThat()
        .areNotEnums()
        .and()
        .areDeclaredInClassesThat()
        .areNotInterfaces()
        .and()
        .areDeclaredInClassesThat()
        .haveSimpleNameNotContaining("Abstract")
        .and()
        .areNotStatic()
        .should(
            new ArchCondition<JavaField>("be private") {
              @Override
              public void check(JavaField field, ConditionEvents events) {
                if (!field.getModifiers().contains(JavaModifier.PRIVATE)) {
                  events.add(
                      SimpleConditionEvent.violated(
                          field,
                          String.format(
                              "Field <%s> in <%s> must be private [ADR-009]",
                              field.getName(), field.getOwner().getName())));
                }
              }
            })
        .because("Instance fields in concrete classes must be private [ADR-009]")
        .check(classes);
  }

  // ─── GOV-001: No Anonymous Classes ───

  /** Asserts no anonymous classes in production (with enum/TypeReference exemption). */
  public static void assertNoAnonymousClasses(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .should()
        .notBeAnonymousClasses()
        .orShould(beEnumConstantOrTypeReference())
        .because("Anonymous classes are banned in production [GOV-001]")
        .check(classes);
  }

  // ─── GOV-004: No Standard Streams ───

  /** Asserts no classes access System.out or System.err. */
  public static void assertNoStandardStreams(JavaClasses classes) {
    com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("Use SLF4J loggers instead of standard output/error streams [GOV-004]")
        .check(classes);
  }

  // ─── GOV-005: No Field Injection ───

  /** Asserts no @Autowired field injection in the given module package. */
  public static void assertNoFieldInjection(JavaClasses classes, String modulePackage) {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage(modulePackage + "..")
        .should()
        .beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .because("Field injection is prohibited — constructor-based DI is mandatory [GOV-005]")
        .check(classes);
  }

  // ─── ERR-112: No Web Controllers in Non-Gateway ───

  /** Asserts no @RestController or @Controller annotations exist in the given module. */
  public static void assertNoWebControllers(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + "..")
        .should()
        .notBeAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .andShould()
        .notBeAnnotatedWith(org.springframework.stereotype.Controller.class)
        .because("Web controllers belong only in orasaka-gateway [ERR-112]")
        .check(classes);
  }

  // ═══════════════════════════════════════════════════════════════════════
  // HEX-001 — HEX-004: STRICT HEXAGONAL ARCHITECTURE ENFORCEMENT
  // ═══════════════════════════════════════════════════════════════════════

  /**
   * [HEX-001] Enforces strict hexagonal layer dependencies using ArchUnit's layeredArchitecture.
   *
   * <ul>
   *   <li>Domain depends on NOTHING (pure POJO/Record territory).
   *   <li>Application may access Domain. Infrastructure may access Application and Domain.
   *   <li>No reverse flow is permitted.
   * </ul>
   */
  public static void assertStrictHexagonalBoundaries(JavaClasses classes, String modulePackage) {
    com.tngtech.archunit.library.Architectures.layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer(DOMAIN_LAYER)
        .definedBy(modulePackage + ".domain..")
        .layer(APPLICATION_LAYER)
        .definedBy(modulePackage + APPLICATION_PKG_PATTERN)
        .layer("Infrastructure")
        .definedBy(modulePackage + ".infrastructure..")
        .whereLayer(DOMAIN_LAYER)
        .mayNotAccessAnyLayer()
        .whereLayer(APPLICATION_LAYER)
        .mayOnlyAccessLayers(DOMAIN_LAYER)
        .whereLayer("Infrastructure")
        .mayOnlyAccessLayers(APPLICATION_LAYER, DOMAIN_LAYER)
        .because(
            "Hexagonal architecture mandates: Domain → nothing, Application → Domain only,"
                + " Infrastructure → Application + Domain [HEX-001]")
        .check(classes);
  }

  /**
   * [HEX-002] Prohibits domain classes from depending on framework packages.
   *
   * <p>The domain layer must remain pristine POJO/Record territory — zero Spring, JPA, or Jackson
   * dependencies allowed.
   */
  public static void assertDomainPurity(JavaClasses classes, String modulePackage) {
    noClasses()
        .that()
        .resideInAPackage(modulePackage + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..")
        .because(
            "Domain layer must remain framework-free POJO/Record territory."
                + " No Spring, JPA, or Jackson allowed [HEX-002]")
        .check(classes);
  }

  /**
   * [HEX-003] Mandates that all DTOs, Commands, and Requests inside the application layer are Java
   * Records to guarantee immutability at compilation boundary.
   */
  public static void assertApplicationInputsAreRecords(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + ".application.dto..")
        .should()
        .beRecords()
        .because("Application DTOs must be Java Records for immutability [HEX-003]")
        .allowEmptyShould(true)
        .check(classes);

    classes()
        .that()
        .resideInAPackage(modulePackage + APPLICATION_PKG_PATTERN)
        .and()
        .haveSimpleNameEndingWith("Command")
        .should()
        .beRecords()
        .because("Application Commands must be Java Records for immutability [HEX-003]")
        .allowEmptyShould(true)
        .check(classes);

    classes()
        .that()
        .resideInAPackage(modulePackage + APPLICATION_PKG_PATTERN)
        .and()
        .haveSimpleNameEndingWith("Request")
        .should()
        .beRecords()
        .because("Application Requests must be Java Records for immutability [HEX-003]")
        .allowEmptyShould(true)
        .check(classes);
  }

  /**
   * [HEX-004] Enforces that every concrete non-abstract class inside infrastructure.adapter must
   * implement at least one interface from an application.ports sub-package.
   */
  public static void assertAdaptersImplementPorts(JavaClasses classes, String modulePackage) {
    classes()
        .that()
        .resideInAPackage(modulePackage + ".infrastructure.adapter..")
        .and()
        .areNotInterfaces()
        .and()
        .doNotHaveModifier(JavaModifier.ABSTRACT)
        .and()
        .haveSimpleNameNotEndingWith("Properties")
        .and()
        .haveSimpleNameNotEndingWith("Config")
        .and()
        .haveSimpleNameNotEndingWith("Configuration")
        .and()
        .haveSimpleNameNotEndingWith(MAPPER_SUFFIX)
        .and()
        .haveSimpleNameNotEndingWith("Entity")
        .and()
        .haveSimpleNameNotEndingWith("Converter")
        .and()
        .areNotAnnotatedWith(jakarta.persistence.Entity.class)
        .should(implementAtLeastOnePortInterface(modulePackage))
        .allowEmptyShould(true)
        .because(
            "Adapter classes must implement at least one port interface"
                + " from .application.ports or extend a Spring Data Repository [HEX-004]")
        .check(classes);
  }

  // ─── Private ArchCondition Helpers ───

  private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
      implementAtLeastOnePortInterface(String modulePackage) {
    return new ArchCondition<>(
        "implement at least one interface from .application.ports or extend Repository") {
      @Override
      public void check(
          com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
        boolean implementsPort =
            javaClass.getAllRawInterfaces().stream()
                .anyMatch(
                    iface ->
                        iface.getPackageName().startsWith(modulePackage + ".application.ports")
                            || iface
                                .getPackageName()
                                .startsWith(modulePackage + ".application.port")
                            || iface.isAssignableTo(
                                org.springframework.data.repository.Repository.class));
        if (!implementsPort) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass,
                  String.format(
                      "Adapter class <%s> does not implement any port interface [HEX-004]",
                      javaClass.getName())));
        }
      }
    };
  }

  /**
   * [HEX-005] Scope guard — prohibits production modules from depending on heavy utility libraries
   * that are restricted to test scope (e.g., Apache Commons IO).
   *
   * <p>Infrastructure adapters are exempt since they may legitimately use IO utilities for file
   * processing adapters.
   */
  public static void assertNoUnapprovedUtilsInProduction(
      JavaClasses classes, String modulePackage) {
    noClasses()
        .that()
        .resideInAPackage(modulePackage + "..")
        .and()
        .resideOutsideOfPackage("..infrastructure.adapter..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.apache.commons.io..")
        .because(
            "Production modules must remain lean. Apache Commons IO is strictly restricted"
                + " to the test boundary [HEX-005]")
        .check(classes);
  }

  private static ArchCondition<JavaField> bePrivateAndFinal() {
    return new ArchCondition<>("be private and final") {
      @Override
      public void check(JavaField field, ConditionEvents events) {
        boolean isPrivate = field.getModifiers().contains(JavaModifier.PRIVATE);
        boolean isFinal = field.getModifiers().contains(JavaModifier.FINAL);
        if (!isPrivate || !isFinal) {
          events.add(
              SimpleConditionEvent.violated(
                  field,
                  String.format(
                      "Field <%s> in <%s> is not private final (private=%s, final=%s)",
                      field.getName(), field.getOwner().getName(), isPrivate, isFinal)));
        }
      }
    };
  }

  private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
      beEnumConstantOrTypeReference() {
    return new ArchCondition<>("be an enum constant body or TypeReference") {
      @Override
      public void check(
          com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
        boolean isEnumBody = javaClass.getEnclosingClass().map(JavaClass::isEnum).orElse(false);
        boolean isTypeRef =
            javaClass
                .getSuperclass()
                .map(superClass -> superClass.getName().contains("TypeReference"))
                .orElse(false);
        if (!isEnumBody && !isTypeRef) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, "Anonymous class <" + javaClass.getName() + "> is not exempt"));
        }
      }
    };
  }
}
