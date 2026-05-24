package com.orasaka.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static JavaClasses coreClasses;

  @BeforeAll
  static void importClasses() {
    coreClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orasaka.core");
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
}
