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

/** Consumes {@code Payment/PaymentReceived} → each allocated order PENDING → TO_SHIP. */
@Component
public class PaymentReceivedPipelineFactory extends AbstractSpringFiveStepsPipelineFactory<PaymentReceivedData> {

    private final PaymentEventsFacade facade;

    public PaymentReceivedPipelineFactory(SpringKafkaEventConsumer eventConsumer,
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
        return DomainEventType.of("Payment", "PaymentReceived");
    }

    @Override
    protected Class<?> declareEventDataType() {
        return PaymentReceivedData.class;
    }

    @Override
    protected ApplicationInputPreparer<PaymentReceivedData> declareApplicationInputPreparer() {
        return eventData -> (PaymentReceivedData) eventData;
    }

    @Override
    protected ApplicationTarget declareApplicationTarget() {
        return new ApplicationTarget(facade, "onPaymentReceived", PaymentReceivedData.class);
    }
}
