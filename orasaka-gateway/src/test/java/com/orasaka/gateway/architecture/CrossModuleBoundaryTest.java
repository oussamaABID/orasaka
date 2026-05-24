package com.orasaka.gateway.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cross-module architectural boundary enforcement. Guarantees strict unidirectional dependency flow
 * across all Orasaka modules.
 *
 * <p>This test lives in orasaka-gateway because only the gateway has compile-time classpath
 * visibility into all sibling modules (core, identity, tools).
 *
 * <p>Enforces the Module Separation Invariant [ERR-102]:
 *
 * <ul>
 *   <li>core ↛ identity, core ↛ tools (core is fully isolated)
 *   <li>tools ↛ identity (tools is identity-blind)
 *   <li>identity ↛ core, identity ↛ tools (identity is fully isolated)
 *   <li>tools → core is ALLOWED (Ports & Adapters interface binding per ADR-007)
 * </ul>
 *
 * @see <a href="../../../../../../AGENTS.md">AGENTS.md §1.A, ERR-102</a>
 */
class CrossModuleBoundaryTest {

  private static JavaClasses allModuleClasses;

  @BeforeAll
  static void importClasses() {
    allModuleClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.core", "com.orasaka.identity", "com.orasaka.tools");
  }

  // ── core isolation ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("[ERR-102] Core must not depend on identity layer")
  void coreIsIdentityBlind() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .because(
            "orasaka-core must remain 100% identity-agnostic. "
                + "Only orasaka-gateway may call identity services [ERR-102, §1.A]")
        .check(allModuleClasses);
  }

  @Test
  @DisplayName("[ERR-102] Core must not depend on tools layer")
  void coreIsToolsBlind() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.core..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.tools..")
        .because(
            "orasaka-core defines ports (interfaces); it must never import tool adapters [ERR-102]")
        .check(allModuleClasses);
  }

  // ── tools isolation ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("[ERR-102] Tools must not depend on identity layer")
  void toolsAreIdentityBlind() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.tools..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.identity..")
        .because(
            "orasaka-tools is stateless regarding user identity. "
                + "Context is injected by the gateway as primitives [ERR-102]")
        .check(allModuleClasses);
  }

  // ── identity isolation ──────────────────────────────────────────────────────

  @Test
  @DisplayName("[ERR-102] Identity must not depend on core layer")
  void identityIsCoreBlind() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.core..")
        .because(
            "orasaka-identity is a pure IAM domain hexagon and must never "
                + "import AI orchestration types from orasaka-core [ERR-102]")
        .check(allModuleClasses);
  }

  @Test
  @DisplayName("[ERR-102] Identity must not depend on tools layer")
  void identityIsToolsBlind() {
    noClasses()
        .that()
        .resideInAPackage("com.orasaka.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.orasaka.tools..")
        .because("orasaka-identity must never import tool implementations [ERR-102]")
        .check(allModuleClasses);
  }

  // ── naming governance ─────────────────────────────────────────────────────

  @Test
  @DisplayName("[ERR-104] No redundant 'Orasaka' prefix on internal types")
  void noRedundantProjectPrefix() {
    classes()
        .that()
        .resideInAPackage("com.orasaka..")
        .and()
        .haveSimpleNameNotStartingWith("OrasakaCoreConfiguration")
        .should()
        .haveSimpleNameNotStartingWith("Orasaka")
        .because(
            "The package namespace already establishes ownership. "
                + "Prefixes add semantic noise [ERR-104, Zero-Prefix Invariant].")
        .check(allModuleClasses);
  }

  @Test
  @DisplayName("[GOV-004] Pipeline interceptors must carry role-broadcasting suffixes")
  void interceptorNamingConvention() {
    classes()
        .that()
        .resideInAPackage("com.orasaka.core.pipeline..")
        .and()
        .implement(com.orasaka.core.pipeline.ContextInterceptor.class)
        .should()
        .haveSimpleNameEndingWith("Interceptor")
        .because(
            "Pipeline processors must explicitly broadcast their pattern role "
                + "via their suffix [GOV-004].")
        .check(allModuleClasses);
  }
}
