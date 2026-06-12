package vn.marketplace.payment.adapter.payment.inbound.messaging;

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
import vn.marketplace.payment.adapter.payment.dto.OrderCompletedEvent;

/** Consumes {@code Order/OrderCompleted} → settle the order and pay the merchant out. */
@Component
public class OrderCompletedPipelineFactory extends AbstractSpringFiveStepsPipelineFactory<OrderCompletedEvent> {

    private final OrderEventsFacade facade;

    public OrderCompletedPipelineFactory(SpringKafkaEventConsumer eventConsumer,
                                         PostProcessor postProcessor,
                                         IdempotencyGuard idempotencyGuard,
                                         RoutingManager routingManager,
                                         JsonCloudEventDeserializer jsonCloudEventDeserializer,
                                         AvroCloudEventDeserializer avroCloudEventDeserializer,
                                         ObjectMapper msfwConsumptionObjectMapper,
                                         OrderEventsFacade facade) {
        super(eventConsumer, postProcessor, idempotencyGuard, routingManager,
                jsonCloudEventDeserializer, avroCloudEventDeserializer, msfwConsumptionObjectMapper);
        this.facade = facade;
        initializeAndRegisterPipeline(eventConsumer);
    }

    @Override
    protected DomainEventType declareDomainEventType() {
        return DomainEventType.of("Order", "OrderCompleted");
    }

    @Override
    protected Class<?> declareEventDataType() {
        return OrderCompletedEvent.class;
    }

    @Override
    protected ApplicationInputPreparer<OrderCompletedEvent> declareApplicationInputPreparer() {
        return eventData -> (OrderCompletedEvent) eventData;
    }

    @Override
    protected ApplicationTarget declareApplicationTarget() {
        return new ApplicationTarget(facade, "onOrderCompleted", OrderCompletedEvent.class);
    }
}
