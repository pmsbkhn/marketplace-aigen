package vn.marketplace.checkout.adapter.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import tech.vsf.ptnt.msfw.domain.workflow.WorkflowObserver;
import tech.vsf.ptnt.springcore.exception.GlobalExceptionHandler;
import tech.vsf.ptnt.springcore.workflow.MicrometerWorkflowObserver;
import vn.marketplace.checkout.application.checkout.CatalogPort;
import vn.marketplace.checkout.application.checkout.CheckoutSessionPort;
import vn.marketplace.checkout.application.checkout.GetCheckoutSession;
import vn.marketplace.checkout.application.checkout.GetCheckoutSessionUc;
import vn.marketplace.checkout.application.checkout.InventoryPort;
import vn.marketplace.checkout.application.checkout.OrderPort;
import vn.marketplace.checkout.application.checkout.PaymentPort;
import vn.marketplace.checkout.application.checkout.SubmitCheckout;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutUc;

/**
 * Checkout wiring. Deliberately does NOT import msfw's SpringCoreConfiguration/OutboxConfiguration:
 * Checkout persists no aggregate and publishes no events (stateless saga, session in Redis), so the
 * domain-registry/outbox machinery has nothing to manage. The msfw GlobalExceptionHandler is
 * registered explicitly so domain exceptions still map to HTTP like every other service.
 */
@Configuration
@ComponentScan(basePackages = {"vn.marketplace.checkout.adapter"})
public class AdapterConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public SubmitCheckout submitCheckout(CatalogPort catalog, InventoryPort inventory,
                                         OrderPort order, PaymentPort payment,
                                         CheckoutSessionPort sessions,
                                         @Value("${checkout.max-items-per-cart:50}") int maxItems,
                                         @Value("${checkout.max-merchants-per-cart:10}") int maxMerchants,
                                         ObjectProvider<MeterRegistry> meterRegistry) {
        // msfw.workflow.runs{workflow=checkout,outcome,step} — alert on outcome=compensation_failed.
        MeterRegistry registry = meterRegistry.getIfAvailable();
        WorkflowObserver observer = registry != null
                ? new MicrometerWorkflowObserver(registry)
                : WorkflowObserver.NOOP;
        return new SubmitCheckoutUc(catalog, inventory, order, payment, sessions, maxItems,
                maxMerchants, observer);
    }

    @Bean
    public GetCheckoutSession getCheckoutSession(CheckoutSessionPort sessions) {
        return new GetCheckoutSessionUc(sessions);
    }
}
