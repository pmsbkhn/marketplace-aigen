package vn.marketplace.order.adapter.order.inbound.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.vsf.ptnt.msfw.consumption.DefaultErrorClassifier;
import tech.vsf.ptnt.msfw.consumption.PostProcessor;
import tech.vsf.ptnt.msfw.consumption.postprocess.DefaultPostProcessor;
import tech.vsf.ptnt.msfw.consumption.spring.ConsumptionPipelines;
import tech.vsf.ptnt.msfw.consumption.spring.SpringKafkaEventConsumer;
import tech.vsf.ptnt.msfw.consumption.spring.deserializer.AvroCloudEventDeserializer;
import tech.vsf.ptnt.msfw.consumption.spring.deserializer.JsonCloudEventDeserializer;
import tech.vsf.ptnt.msfw.consumption.spring.metrics.ConsumptionMetrics;
import tech.vsf.ptnt.msfw.consumption.spring.postprocess.JacksonDataToJsonConverter;
import tech.vsf.ptnt.msfw.consumption.spring.postprocess.LoggingResultStore;
import tech.vsf.ptnt.msfw.consumption.upcast.UpcasterChain;
import tech.vsf.ptnt.msfw.publication.kafka.RoutingManager;
import tech.vsf.ptnt.msfw.test.InMemoryIdempotencyGuard;
import vn.marketplace.order.application.order.ExpirePendingOrderCmd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Full consumption slice without a broker: our own {@code Order/OrderPendingTimedOut} delayed
 * timer comes back at its deliver-after instant and lands on the expiry use-case exactly once —
 * a redelivery is absorbed by the inbox.
 */
class OrderTimeoutsConsumptionTest {

    private static final String TOPIC = "order-timeouts";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ExpirePendingOrderCmd> expirations = new ArrayList<>();
    private SpringKafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SpringKafkaEventConsumer(new DefaultErrorClassifier(), new ConsumptionMetrics(null));

        RoutingManager routingManager = new RoutingManager();
        routingManager.appendConfig("Order", "OrderPendingTimedOut", TOPIC, "JSON", "both");

        PostProcessor postProcessor =
                new DefaultPostProcessor(new LoggingResultStore(), new JacksonDataToJsonConverter(objectMapper));
        JsonCloudEventDeserializer json = new JsonCloudEventDeserializer(objectMapper);
        AvroCloudEventDeserializer avro = new AvroCloudEventDeserializer(null);

        OrderTimeoutsFacade facade = new OrderTimeoutsFacade(expirations::add);

        ConsumptionPipelines pipelines = new ConsumptionPipelines(consumer, postProcessor,
                new InMemoryIdempotencyGuard(), routingManager, json, avro, objectMapper,
                new UpcasterChain(List.of()));
        pipelines.subscribe("Order", "OrderPendingTimedOut", OrderPendingTimedOutData.class)
                .handle(facade, "onPendingTimedOut");
    }

    @Test
    void timerLandsOnTheExpiryUseCase() {
        consumer.consume(record(timerEnvelope("evt-t1", "O-1")), TOPIC);

        assertEquals(1, expirations.size());
        assertEquals("O-1", expirations.get(0).orderId());
    }

    @Test
    void duplicateRedeliveryIsSkippedByTheInbox() {
        consumer.consume(record(timerEnvelope("evt-t2", "O-2")), TOPIC);
        consumer.consume(record(timerEnvelope("evt-t2", "O-2")), TOPIC);

        assertEquals(1, expirations.size(), "the inbox must absorb the at-least-once redelivery");
    }

    private String timerEnvelope(String eventId, String orderId) {
        return """
                {"specversion":"1.0","id":"%s","source":"Order","type":"OrderPendingTimedOut",
                 "subject":"%s","datacontenttype":"application/json",
                 "data":{"orderId":"%s"}}
                """.formatted(eventId, orderId, orderId);
    }

    private ConsumerRecord<String, byte[]> record(String envelope) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "k", envelope.getBytes(StandardCharsets.UTF_8));
    }
}
