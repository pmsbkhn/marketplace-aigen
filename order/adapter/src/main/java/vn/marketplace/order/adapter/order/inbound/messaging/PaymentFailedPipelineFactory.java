package vn.marketplace.order.adapter.order.inbound.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tech.vsf.ptnt.msfw.consumption.IdempotencyGuard;
import tech.vsf.ptnt.msfw.consumption.PostProcessor;
import tech.vsf.ptnt.msfw.consumption.spring.AbstractSpringFiveStepsPipelineFactory;
import tech.vsf.ptnt.msfw.consumption.spring.SpringKafkaEventConsumer;
import tech.vsf.ptnt.msfw.consumption.spring.deserializer.AvroCloudEventDeserializer;
import tech.vsf.ptnt.msfw.consumption.spring.deserializer.JsonCloudEventDeserializer;
import tech.vsf.ptnt.msfw.consumption.step.ApplicationInputPreparer;
import tech.vsf.ptnt.msfw.consumption.step.ApplicationTarget;
import tech.vsf.ptnt.msfw.domain.core.DomainEventType;
import tech.vsf.ptnt.msfw.publication.kafka.RoutingManager;

/** Consumes {@code Payment/PaymentFailed} → cancel the checkout's pending orders. */
@Component
public class PaymentFailedPipelineFactory extends AbstractSpringFiveStepsPipelineFactory<PaymentFailedData> {

    private final PaymentEventsFacade facade;

    public PaymentFailedPipelineFactory(SpringKafkaEventConsumer eventConsumer,
                                        PostProcessor postProcessor,
                                        IdempotencyGuard idempotencyGuard,
                                        RoutingManager routingManager,
                                        JsonCloudEventDeserializer jsonCloudEventDeserializer,
                                        AvroCloudEventDeserializer avroCloudEventDeserializer,
                                        ObjectMapper msfwConsumptionObjectMapper,
                                        PaymentEventsFacade facade) {
        super(eventConsumer, postProcessor, idempotencyGuard, routingManager,
                jsonCloudEventDeserializer, avroCloudEventDeserializer, msfwConsumptionObjectMapper);
        this.facade = facade;
        initializeAndRegisterPipeline(eventConsumer);
    }

    @Override
    protected DomainEventType declareDomainEventType() {
        return DomainEventType.of("Payment", "PaymentFailed");
    }

    @Override
    protected Class<?> declareEventDataType() {
        return PaymentFailedData.class;
    }

    @Override
    protected ApplicationInputPreparer<PaymentFailedData> declareApplicationInputPreparer() {
        return eventData -> (PaymentFailedData) eventData;
    }

    @Override
    protected ApplicationTarget declareApplicationTarget() {
        return new ApplicationTarget(facade, "onPaymentFailed", PaymentFailedData.class);
    }
}
