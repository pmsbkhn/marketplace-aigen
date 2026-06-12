package vn.marketplace.inventory.adapter.stock.inbound.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.vsf.ptnt.msfw.consumption.DefaultErrorClassifier;
import tech.vsf.ptnt.msfw.consumption.PostProcessor;
import tech.vsf.ptnt.msfw.consumption.postprocess.DefaultPostProcessor;
import tech.vsf.ptnt.msfw.consumption.spring.SpringKafkaEventConsumer;
import tech.vsf.ptnt.msfw.consumption.spring.deserializer.AvroCloudEventDeserializer;
import tech.vsf.ptnt.msfw.consumption.spring.deserializer.JsonCloudEventDeserializer;
import tech.vsf.ptnt.msfw.consumption.spring.metrics.ConsumptionMetrics;
import tech.vsf.ptnt.msfw.consumption.spring.postprocess.JacksonDataToJsonConverter;
import tech.vsf.ptnt.msfw.consumption.spring.postprocess.LoggingResultStore;
import tech.vsf.ptnt.msfw.publication.kafka.RoutingManager;
import tech.vsf.ptnt.msfw.test.InMemoryIdempotencyGuard;
import vn.marketplace.inventory.application.stock.DeductStockCmd;
import vn.marketplace.inventory.application.stock.InitSkuCmd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Full consumption slice without a broker: CloudEvent-JSON record → msfw deserializers →
 * facade → use-case command, including the consumed event id threaded via EventCausation.
 */
class StockEventsConsumptionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<InitSkuCmd> initialised = new ArrayList<>();
    private final List<DeductStockCmd> deducted = new ArrayList<>();
    private SpringKafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SpringKafkaEventConsumer(new DefaultErrorClassifier(), new ConsumptionMetrics(null));

        RoutingManager routingManager = new RoutingManager();
        routingManager.appendConfig("Catalog", "ProductCreated", "catalog-events", "JSON");
        routingManager.appendConfig("Order", "OrderCompleted", "order-events", "JSON");

        PostProcessor postProcessor =
                new DefaultPostProcessor(new LoggingResultStore(), new JacksonDataToJsonConverter(objectMapper));
        JsonCloudEventDeserializer json = new JsonCloudEventDeserializer(objectMapper);
        AvroCloudEventDeserializer avro = new AvroCloudEventDeserializer(null);

        StockEventsFacade facade = new StockEventsFacade(initialised::add, deducted::add);

        new InitSkuPipelineFactory(consumer, postProcessor, new InMemoryIdempotencyGuard(),
                routingManager, json, avro, objectMapper, facade);
        new DeductStockPipelineFactory(consumer, postProcessor, new InMemoryIdempotencyGuard(),
                routingManager, json, avro, objectMapper, facade);
    }

    @Test
    void productCreatedInitialisesSkusWithTheConsumedEventId() {
        String envelope = """
                {"specversion":"1.0","id":"evt-pc-1","source":"Catalog","type":"ProductCreated",
                 "subject":"P-1","datacontenttype":"application/json",
                 "data":{"productId":"P-1","merchantId":"M-1","skuCodes":["SKU-A","SKU-B"],
                         "type":{"boundedContext":"Catalog","name":"ProductCreated"},"subject":"P-1"}}
                """;
        consumer.consume(record("catalog-events", envelope), "catalog-events");

        assertEquals(1, initialised.size());
        InitSkuCmd cmd = initialised.get(0);
        assertEquals("evt-pc-1", cmd.eventId());
        assertEquals("P-1", cmd.productId());
        assertEquals(List.of("SKU-A", "SKU-B"), cmd.skuCodes());
        assertEquals("M-1", cmd.merchantId());
    }

    @Test
    void orderCompletedDeductsTheOrderedSkus() {
        String envelope = """
                {"specversion":"1.0","id":"evt-oc-1","source":"Order","type":"OrderCompleted",
                 "subject":"O-1","datacontenttype":"application/json",
                 "data":{"orderId":"O-1","merchantId":"M-1",
                         "items":[{"sku":"SKU-A","qty":2,"priceSnapshot":50000},
                                  {"sku":"SKU-B","qty":1,"priceSnapshot":80000}],
                         "type":{"boundedContext":"Order","name":"OrderCompleted"},"subject":"O-1"}}
                """;
        consumer.consume(record("order-events", envelope), "order-events");

        assertEquals(1, deducted.size());
        DeductStockCmd cmd = deducted.get(0);
        assertEquals("evt-oc-1", cmd.eventId());
        assertEquals("O-1", cmd.orderId());
        assertEquals(List.of("SKU-A", "SKU-B"), cmd.skus());
    }

    @Test
    void duplicateRedeliveryIsSkippedByTheInbox() {
        String envelope = """
                {"specversion":"1.0","id":"evt-dup","source":"Catalog","type":"ProductCreated",
                 "subject":"P-2","datacontenttype":"application/json",
                 "data":{"productId":"P-2","merchantId":"M-1","skuCodes":["SKU-C"]}}
                """;
        consumer.consume(record("catalog-events", envelope), "catalog-events");
        consumer.consume(record("catalog-events", envelope), "catalog-events");

        assertEquals(1, initialised.size());
    }

    private ConsumerRecord<String, byte[]> record(String topic, String envelope) {
        return new ConsumerRecord<>(topic, 0, 0L, "k", envelope.getBytes(StandardCharsets.UTF_8));
    }
}
