package vn.marketplace.payment.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import tech.vsf.ea.archrules.FitnessHarness;
import tech.vsf.ea.archrules.MsfwFitness;

/**
 * Fitness functions via the registry-driven harness ({@link FitnessHarness}): each rule runs in the
 * mode the EA registry assigns this system. Rule bodies + modes live in the independent
 * ea-archrules library / registry; this class only binds them to the service package.
 */
class FitnessFunctionsTest {

    private static final FitnessHarness EA = FitnessHarness.forSystem("marketplace-payment");
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("vn.marketplace.payment");

    @Test
    void domain_is_pure() {
        EA.evaluate("domainIsPure", MsfwFitness.domainIsPure("vn.marketplace.payment"), CLASSES);
    }

    @Test
    void use_case_slices_do_not_cross_depend() {
        EA.evaluate("useCaseSliceIsolation", MsfwFitness.useCaseSlices("vn.marketplace.payment.application.(*).."), CLASSES);
    }

    @Test
    void aggregates_encapsulated() {
        EA.evaluate("aggregateEncapsulation", MsfwFitness.aggregatesEncapsulated(), CLASSES);
    }

    @Test
    void entities_encapsulated() {
        EA.evaluate("entityEncapsulation", MsfwFitness.entitiesEncapsulated(), CLASSES);
    }

    @Test
    void state_writers_publish_via_outbox() {
        EA.evaluate("stateWritersPublish", MsfwFitness.stateWritersPublish("vn.marketplace.payment.application"), CLASSES);
    }

    @Test
    void quantum_sync_boundary() {
        EA.evaluate("quantumSyncBoundary",
                MsfwFitness.quantumSyncBoundary("vn.marketplace.payment", EA.allowedSyncQuanta()), CLASSES);
    }

    /** The escrow event store stays inside the Payment quantum (no synchronous cross-quantum persistence). */
    @Test
    void event_store_in_quantum() {
        EA.evaluate("eventStoreInQuantum", MsfwFitness.eventStoreInQuantum("vn.marketplace.payment"), CLASSES);
    }
}
