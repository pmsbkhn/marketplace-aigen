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
 * Consumes {@code Catalog/ProductCreated} and initialises the product's SKUs at quantity 0.
 * Infrastructure (deserializers, topic registration, inbox dedup, retry/DLQ) comes from msfw's
 * {@code ConsumptionConfiguration}; only the business pieces are declared here.
 */
@Component
public class InitSkuPipelineFactory extends AbstractSpringFiveStepsPipelineFactory<ProductCreatedData> {

    private final StockEventsFacade facade;

    public InitSkuPipelineFactory(SpringKafkaEventConsumer eventConsumer,
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
        return DomainEventType.of("Catalog", "ProductCreated");
    }

    @Override
    protected Class<?> declareEventDataType() {
        return ProductCreatedData.class;
    }

    @Override
    protected ApplicationInputPreparer<ProductCreatedData> declareApplicationInputPreparer() {
        return eventData -> (ProductCreatedData) eventData;
    }

    @Override
    protected ApplicationTarget declareApplicationTarget() {
        return new ApplicationTarget(facade, "onProductCreated", ProductCreatedData.class);
    }
}
