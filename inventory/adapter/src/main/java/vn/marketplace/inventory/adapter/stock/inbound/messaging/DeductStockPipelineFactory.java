package vn.marketplace.inventory.adapter.stock.inbound.messaging;

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

/**
 * Consumes {@code Order/OrderCompleted} and permanently deducts the reserved stock
 * (reservation {@code HELD → CONSUMED}).
 */
@Component
public class DeductStockPipelineFactory extends AbstractSpringFiveStepsPipelineFactory<OrderCompletedData> {

    private final StockEventsFacade facade;

    public DeductStockPipelineFactory(SpringKafkaEventConsumer eventConsumer,
                                      PostProcessor postProcessor,
                                      IdempotencyGuard idempotencyGuard,
                                      RoutingManager routingManager,
                                      JsonCloudEventDeserializer jsonCloudEventDeserializer,
                                      AvroCloudEventDeserializer avroCloudEventDeserializer,
                                      ObjectMapper msfwConsumptionObjectMapper,
                                      StockEventsFacade facade) {
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
        return OrderCompletedData.class;
    }

    @Override
    protected ApplicationInputPreparer<OrderCompletedData> declareApplicationInputPreparer() {
        return eventData -> (OrderCompletedData) eventData;
    }

    @Override
    protected ApplicationTarget declareApplicationTarget() {
        return new ApplicationTarget(facade, "onOrderCompleted", OrderCompletedData.class);
    }
}
