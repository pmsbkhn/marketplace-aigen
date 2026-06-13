package vn.marketplace.order.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import tech.vsf.ea.archrules.MsfwFitness;

/**
 * Architectural fitness functions — automated, deployment-gating checks. The rule bodies live in
 * the independent {@code ea-archrules} library ({@link MsfwFitness}); this class only binds them
 * to the order service's package root. One source of truth across every service replaces the
 * per-service copies.
 */
@AnalyzeClasses(packages = "vn.marketplace.order", importOptions = ImportOption.DoNotIncludeTests.class)
class FitnessFunctionsTest {

    @ArchTest
    static final ArchRule domain_is_pure = MsfwFitness.domainIsPure("vn.marketplace.order");

    @ArchTest
    static final ArchRule use_case_slices_do_not_cross_depend =
            MsfwFitness.useCaseSlices("vn.marketplace.order.application.(*)..");

    @ArchTest
    static final ArchRule aggregates_encapsulated = MsfwFitness.aggregatesEncapsulated();

    @ArchTest
    static final ArchRule entities_encapsulated = MsfwFitness.entitiesEncapsulated();

    @ArchTest
    static final ArchRule state_writers_publish_via_outbox =
            MsfwFitness.stateWritersPublish("vn.marketplace.order.application");
}
