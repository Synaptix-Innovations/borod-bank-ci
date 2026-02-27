package com.vlad774.borodbank.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture rules enforced via ArchUnit.
 * Validates layering, dependency direction, and coding conventions.
 */
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.vlad774.borodbank";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("Controllers should not access repositories directly")
    void controllersShouldNotAccessRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Service layer should not depend on controller layer")
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Repository layer should not depend on service or controller layers")
    void repositoriesShouldNotDependOnUpperLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");

        rule.check(classes);
    }

    @Test
    @DisplayName("No cyclic dependencies between packages")
    void noCyclicDependencies() {
        ArchRule rule = slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles();

        rule.check(classes);
    }

    @Test
    @DisplayName("No System.out or System.err in production code")
    void noSystemOutOrErr() {
        ArchRule rule = noClasses()
                .should().callMethod(System.class, "out")
                .orShould().callMethod(System.class, "err")
                .because("Use a logger instead of System.out/err");

        rule.check(classes);
    }

    @Test
    @DisplayName("JPA entities should reside in model package only")
    void entitiesShouldBeInModelPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..model..");

        rule.check(classes);
    }
}
