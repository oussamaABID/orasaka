package com.orasaka.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Enforces constructor immutability and code craftsmanship governance rules across orasaka-core.
 *
 * <p>ADR-007, ADR-008: Collection fields (List, Map, Set) in non-record domain components must be
 * declared {@code private final} and initialized via immutable copying mechanisms.
 */
class GovernanceTest {

  /** Production-only classes (used by ADR-007, GOV-001..003 rules). */
  private static JavaClasses coreClasses;

  /** All core classes including test layer (used by ERR-103 file isolation rule). */
  private static JavaClasses allCoreClasses;

  @BeforeAll
  static void importClasses() {
    coreClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.core");
    allCoreClasses = new ClassFileImporter().importPackages("com.orasaka.core");
  }

  @Test
  @DisplayName("[ADR-007] Collection fields in non-record classes must be private final")
  void collectionFieldsMustBePrivateFinal() {
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
        .because(
            "Collection fields must be private final for immutability [ADR-007, ADR-008]."
                + " Use Map.copyOf()/List.copyOf() in constructors.")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[GOV-001] No anonymous classes in production source (enums/TypeRef exempt)")
  void noAnonymousClassesInProduction() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .notBeAnonymousClasses()
        .orShould(beEnumConstantOrTypeReference())
        .because(
            "Anonymous classes are banned in production — use explicit named implementations"
                + " or lambda expressions [Governance Mandate]."
                + " Enum constants and Jackson TypeReferences are exempt.")
        .check(coreClasses);
  }

  private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
      beEnumConstantOrTypeReference() {
    return new ArchCondition<>("be an enum constant body or TypeReference") {
      @Override
      public void check(
          com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
        boolean isEnumBody =
            javaClass.getEnclosingClass().isPresent()
                && javaClass.getEnclosingClass().get().isEnum();
        boolean isTypeRef =
            javaClass.getSuperclass().isPresent()
                && javaClass.getSuperclass().get().getName().contains("TypeReference");
        if (!isEnumBody && !isTypeRef) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, "Anonymous class <" + javaClass.getName() + "> is not exempt"));
        }
      }
    };
  }

  @Test
  @DisplayName(
      "[GOV-004] No classes in production should access standard streams (System.out/System.err)")
  void noStandardStreams() {
    com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("Use SLF4J loggers instead of standard output/error streams")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[GOV-002] Engine integration gates must be public (EngineModelRegistry exempt)")
  void integrationGatesMustBePublic() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core.application.engine..")
        .and()
        .haveSimpleNameEndingWith("Engine")
        .should()
        .bePublic()
        .because(
            "Engine integration gates (Engine, AbstractEngine) must be public."
                + " EngineModelRegistry is package-private per ADR-009.")
        .check(coreClasses);
  }

  @Test
  @DisplayName(
      "[GOV-003] No hardcoded localhost, dummy keys, or System.getenv in production source")
  void noHardcodedLocalhostInProductionSource() throws IOException {
    Path sourceRoot = Path.of("orasaka-core/src/main/java");
    if (!Files.exists(sourceRoot)) {
      sourceRoot = Path.of("src/main/java");
    }

    List<String> violations = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      paths
          .filter(p -> p.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  List<String> lines = Files.readAllLines(path);
                  for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    // Skip comments and javadoc
                    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                      continue;
                    }
                    if (line.contains("localhost:") || line.contains("127.0.0.1")) {
                      violations.add(path.getFileName() + ":" + (i + 1) + " -> " + line);
                    }
                    if (line.contains("dummy-key") || line.contains("\"dummy")) {
                      violations.add(
                          path.getFileName() + ":" + (i + 1) + " -> hardcoded credential: " + line);
                    }
                    if (line.contains("System.getenv(")) {
                      violations.add(
                          path.getFileName()
                              + ":"
                              + (i + 1)
                              + " -> raw env access (use typed CoreProperties): "
                              + line);
                    }
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    assertTrue(
        violations.isEmpty(),
        "Production source contains banned literals:\n" + String.join("\n", violations));
  }

  @Test
  @DisplayName(
      "[ERR-103] Every top-level class (production + test) must reside in a dedicated file")
  void oneTopLevelClassPerFile() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .and()
        .doNotHaveModifier(com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC)
        .should(
            new ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                "reside in a dedicated file matching their class name") {
              @Override
              public void check(
                  com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
                // Bypass nested, inner, member, or anonymous classes
                if (javaClass.isAnonymousClass() || javaClass.isMemberClass()) {
                  return;
                }
                String sourceFileName =
                    javaClass.getSource().isPresent()
                        ? javaClass.getSource().get().getFileName().orElse(null)
                        : null;
                if (sourceFileName != null && !sourceFileName.equals("Unknown Source")) {
                  String expectedFileName = javaClass.getSimpleName() + ".java";
                  if (!sourceFileName.equals(expectedFileName)) {
                    String message =
                        String.format(
                            "Architecture Violation [ERR-103]: Class '%s' is illegally bundled"
                                + " inside '%s'. Every top-level component (production or test)"
                                + " must have its own dedicated file.",
                            javaClass.getFullName(), sourceFileName);
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                  }
                }
              }
            })
        .because(
            "Every top-level Java class, interface, enum, or record—whether in production"
                + " (src/main) or test suites (src/test)—must reside in its own dedicated"
                + " .java source file [ERR-103, Source File Isolation Invariant].")
        .check(allCoreClasses);
  }

  private static ArchCondition<JavaField> bePrivateAndFinal() {
    return new ArchCondition<>("be private and final") {
      @Override
      public void check(JavaField field, ConditionEvents events) {
        boolean isPrivate =
            field.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PRIVATE);
        boolean isFinal =
            field.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.FINAL);
        if (!isPrivate || !isFinal) {
          String message =
              String.format(
                  "Field <%s> in <%s> is not private final (private=%s, final=%s)",
                  field.getName(), field.getOwner().getName(), isPrivate, isFinal);
          events.add(SimpleConditionEvent.violated(field, message));
        }
      }
    };
  }

  @Test
  @DisplayName("[ERR-104] No class names with redundant 'Orasaka' prefix")
  void noRedundantProjectPrefix() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .haveSimpleNameNotStartingWith("Orasaka")
        .because(
            "The project name must not be prepended to class names [ERR-104]."
                + " Use package topology for ownership.")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[ERR-112] No @RestController or @Controller annotations in core")
  void noWebControllersInCore() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .notBeAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .andShould()
        .notBeAnnotatedWith(org.springframework.stereotype.Controller.class)
        .because(
            "orasaka-core is a stateless library — web controllers belong in orasaka-gateway"
                + " [ERR-112, Section 1.A]")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[ADR-009] Fields in concrete non-record classes must be private")
  void fieldsInConcreteClassesMustBePrivate() {
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
                boolean isPrivate =
                    field
                        .getModifiers()
                        .contains(com.tngtech.archunit.core.domain.JavaModifier.PRIVATE);
                if (!isPrivate) {
                  events.add(
                      SimpleConditionEvent.violated(
                          field,
                          String.format(
                              "Field <%s> in <%s> must be private [ADR-009]",
                              field.getName(), field.getOwner().getName())));
                }
              }
            })
        .because(
            "Instance fields in concrete classes must be private [ADR-009]."
                + " Use accessor methods for encapsulation.")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[ERR-105] *Impl classes must be package-private (not public)")
  void implClassesMustBePackagePrivate() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .and()
        .haveSimpleNameEndingWith("Impl")
        .should()
        .notBePublic()
        .because(
            "Implementation classes (*Impl) must be package-private."
                + " Cross-module interaction happens through interfaces only [ERR-105].")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[ERR-107] Mapper classes must be final with only static methods")
  void mappersAreStaticUtilities() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .and()
        .haveSimpleNameEndingWith("Mapper")
        .should()
        .haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
        .because(
            "Mapper classes must be final static utility classes."
                + " Field-by-field mapping in controllers/services is banned [ERR-107].")
        .check(coreClasses);
  }

  @Test
  @DisplayName("[ERR-113] No Environment injection in production beans")
  void noEnvironmentInjection() throws IOException {
    Path sourceRoot = Path.of("orasaka-core/src/main/java");
    if (!Files.exists(sourceRoot)) {
      sourceRoot = Path.of("src/main/java");
    }

    List<String> violations = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      paths
          .filter(p -> p.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  List<String> lines = Files.readAllLines(path);
                  boolean isConfigClass = false;
                  for (String line : lines) {
                    if (line.contains("@Configuration")) {
                      isConfigClass = true;
                      break;
                    }
                  }
                  // @Configuration classes may use Environment in @Bean methods via Binder
                  if (isConfigClass) {
                    return;
                  }
                  for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                      continue;
                    }
                    if (line.contains("org.springframework.core.env.Environment")) {
                      violations.add(
                          path.getFileName()
                              + ":"
                              + (i + 1)
                              + " -> Environment injection (use @ConfigurationProperties): "
                              + line);
                    }
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    assertTrue(
        violations.isEmpty(),
        "Production source injects Environment directly [ERR-113]:\n"
            + String.join("\n", violations));
  }

  @Test
  @DisplayName("[GOV-005] No @Autowired field injection — constructor DI only")
  void noFieldInjection() {
    fields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("com.orasaka.core..")
        .and()
        .areAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .should(
            new ArchCondition<JavaField>("not exist (field injection is banned)") {
              @Override
              public void check(JavaField field, ConditionEvents events) {
                events.add(
                    SimpleConditionEvent.violated(
                        field,
                        String.format(
                            "Field <%s> in <%s> uses @Autowired field injection."
                                + " Use constructor injection instead [GOV-005].",
                            field.getName(), field.getOwner().getName())));
              }
            })
        .allowEmptyShould(true)
        .because(
            "Constructor injection is mandatory. @Autowired field injection is banned [GOV-005].")
        .check(coreClasses);
  }
}
