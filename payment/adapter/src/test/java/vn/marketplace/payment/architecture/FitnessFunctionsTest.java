package vn.marketplace.payment.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import tech.vsf.ptnt.msfw.domain.core.Aggregate;
import tech.vsf.ptnt.msfw.domain.core.Entity;
import tech.vsf.ptnt.msfw.event.handling.EventPublishHandler;

/**
 * Architectural fitness functions (fitness-funcs.txt) — automated, deployment-gating checks.
 */
class FitnessFunctionsTest {

    private static JavaClasses paymentClasses;

    @BeforeAll
    static void importClasses() {
        paymentClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("vn.marketplace.payment");
    }

    @Test
    void dependencyInversion_domainDependsOnNothingOutward() {
        noClasses()
                .that().resideInAPackage("vn.marketplace.payment.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "vn.marketplace.payment.application..",
                        "vn.marketplace.payment.adapter..",
                        "org.springframework..",
                        "jakarta..",
                        "tech.vsf.ptnt.springcore..",
                        "tech.vsf.ptnt.msfw.infrastructure..")
                .because("the domain must not depend on application, adapter, or any framework/Spring code")
                .check(paymentClasses);
    }

    @Test
    void encapsulation_aggregatesHaveNoPublicSetters() {
        noMethods()
                .that().areDeclaredInClassesThat().areAssignableTo(Aggregate.class)
                .should().bePublic().andShould().haveNameMatching("set[A-Z].*")
                .because("aggregate state must change only through verb-based methods, never public setters")
                .check(paymentClasses);
    }

    @Test
    void encapsulation_entitiesHaveNoPublicSetters() {
        noMethods()
                .that().areDeclaredInClassesThat().areAssignableTo(Entity.class)
                .should().bePublic().andShould().haveNameMatching("set[A-Z].*")
                .because("entity state must change only through verb-based methods, never public setters")
                .check(paymentClasses);
    }

    @Test
    void eventPublish_stateWritingUseCasesAreAnnotated() {
        ArchCondition<JavaMethod> beAnnotatedWhenWritingState =
                new ArchCondition<>("be annotated with @EventPublishHandler when they persist a state change") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        boolean writesState = method.getMethodCallsFromSelf().stream().anyMatch(call -> {
                            String owner = call.getTargetOwner().getFullName();
                            String name = call.getName();
                            return owner.equals("tech.vsf.ptnt.msfw.domain.core.Repository")
                                    && (name.equals("save") || name.equals("delete"));
                        });
                        if (writesState && !method.isAnnotatedWith(EventPublishHandler.class)) {
                            events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName()
                                            + " writes state via Repository but is not annotated with @EventPublishHandler"));
                        }
                    }
                };

        methods()
                .that().areDeclaredInClassesThat().resideInAPackage("vn.marketplace.payment.application..")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Uc")
                .should(beAnnotatedWhenWritingState)
                .because("use cases that change persistent state must publish via the @EventPublishHandler outbox path")
                .check(paymentClasses);
    }
}
