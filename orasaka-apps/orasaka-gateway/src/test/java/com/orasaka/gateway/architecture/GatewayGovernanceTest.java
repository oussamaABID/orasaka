package com.orasaka.gateway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
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
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Governance guardrails for the orasaka-gateway module.
 *
 * <p>Mirrors core governance rules: anonymous class ban (with framework exemptions) and localhost
 * source scanning.
 */
class GatewayGovernanceTest {

  /** Framework interfaces that legitimately require anonymous implementations. */
  private static final Set<String> EXEMPT_SUPERTYPES =
      Set.of("jakarta.servlet.ServletInputStream", "graphql.schema.Coercing");

  private static JavaClasses gatewayClasses;

  @BeforeAll
  static void importClasses() {
    gatewayClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.gateway");
  }

  @Test
  @DisplayName("[GOV-001] No anonymous classes in gateway (framework exempt)")
  void noAnonymousClassesInProduction() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.gateway..")
        .should()
        .notBeAnonymousClasses()
        .orShould(beFrameworkAnonymousClass())
        .because(
            "Anonymous classes are banned in production — use explicit named implementations"
                + " or lambda expressions. Framework-required patterns are exempt.")
        .check(gatewayClasses);
  }

  @Test
  @DisplayName(
      "[GOV-004] No classes in gateway production should access standard streams (System.out/System.err)")
  void noStandardStreams() {
    com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("Use SLF4J loggers instead of standard output/error streams")
        .check(gatewayClasses);
  }

  @Test
  @DisplayName("[GOV-003] No hardcoded localhost or dummy keys in gateway production source")
  void noHardcodedLocalhostInProductionSource() throws IOException {
    Path sourceRoot = Path.of("orasaka-gateway/src/main/java");
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
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
    }

    assertTrue(
        violations.isEmpty(),
        "Gateway production source contains banned literals:\n" + String.join("\n", violations));
  }

  private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass>
      beFrameworkAnonymousClass() {
    return new ArchCondition<>("be a framework-required anonymous class") {
      @Override
      public void check(
          com.tngtech.archunit.core.domain.JavaClass javaClass, ConditionEvents events) {
        boolean isExempt =
            javaClass.getSuperclass().isPresent()
                && EXEMPT_SUPERTYPES.contains(javaClass.getSuperclass().get().getName());
        if (!isExempt) {
          // Check implemented interfaces — use toErasure() to handle parameterized types
          boolean implementsExempt =
              javaClass.getInterfaces().stream()
                  .anyMatch(
                      i ->
                          EXEMPT_SUPERTYPES.contains(i.getName())
                              || EXEMPT_SUPERTYPES.contains(i.toErasure().getName()));
          isExempt = implementsExempt;
        }
        if (!isExempt) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass,
                  "Anonymous class <" + javaClass.getName() + "> is not framework-exempt"));
        }
      }
    };
  }
}
