package vn.marketplace.order.adapter.order.inbound.messaging;

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
import vn.marketplace.order.application.order.CancelOrderCmd;
import vn.marketplace.order.application.order.TransitionEvent;
import vn.marketplace.order.application.order.TransitionOrderCmd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Full consumption slice without a broker: PaymentReceived fans out one transition per allocated
 * order (eventId threaded via EventCausation); PaymentFailed cancels every order of the checkout.
 */
class PaymentEventsConsumptionTest {

    private static final String TOPIC = "payment-events";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<TransitionOrderCmd> transitions = new ArrayList<>();
    private final List<CancelOrderCmd> cancellations = new ArrayList<>();
    private SpringKafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SpringKafkaEventConsumer(new DefaultErrorClassifier(), new ConsumptionMetrics(null));

        RoutingManager routingManager = new RoutingManager();
        routingManager.appendConfig("Payment", "PaymentReceived", TOPIC, "JSON");
        routingManager.appendConfig("Payment", "PaymentFailed", TOPIC, "JSON");

        PostProcessor postProcessor =
                new DefaultPostProcessor(new LoggingResultStore(), new JacksonDataToJsonConverter(objectMapper));
        JsonCloudEventDeserializer json = new JsonCloudEventDeserializer(objectMapper);
        AvroCloudEventDeserializer avro = new AvroCloudEventDeserializer(null);

        PaymentEventsFacade facade = new PaymentEventsFacade(transitions::add, cancellations::add);

        new PaymentReceivedPipelineFactory(consumer, postProcessor, new InMemoryIdempotencyGuard(),
                routingManager, json, avro, objectMapper, facade);
        new PaymentFailedPipelineFactory(consumer, postProcessor, new InMemoryIdempotencyGuard(),
                routingManager, json, avro, objectMapper, facade);
    }

    @Test
    void paymentReceivedTransitionsEveryAllocatedOrder() {
        String envelope = """
                {"specversion":"1.0","id":"evt-pr-1","source":"Payment","type":"PaymentReceived",
                 "subject":"PAY-1","datacontenttype":"application/json",
                 "data":{"paymentId":"PAY-1","orderRef":"CHK-1","gatewayTxnId":"TXN-1","amount":130000,
                         "allocations":[{"orderId":"O-1","merchantId":"M-1","amount":50000},
                                        {"orderId":"O-2","merchantId":"M-2","amount":80000}]}}
                """;
        consumer.consume(record(envelope), TOPIC);

        assertEquals(2, transitions.size());
        TransitionOrderCmd first = transitions.get(0);
        assertEquals("O-1", first.orderId());
        assertEquals(TransitionEvent.PAYMENT_RECEIVED, first.event());
        assertEquals("evt-pr-1", first.eventId());
        assertEquals("O-2", transitions.get(1).orderId());
    }

    @Test
    void paymentFailedCancelsEveryOrderOfTheCheckout() {
        String envelope = """
                {"specversion":"1.0","id":"evt-pf-1","source":"Payment","type":"PaymentFailed",
                 "subject":"PAY-2","datacontenttype":"application/json",
                 "data":{"paymentId":"PAY-2","orderRef":"CHK-2","reason":"CARD_DECLINED",
                         "orderIds":["O-3","O-4"]}}
                """;
        consumer.consume(record(envelope), TOPIC);

        assertEquals(2, cancellations.size());
        assertEquals("O-3", cancellations.get(0).orderId());
        assertEquals("PAYMENT_FAILED: CARD_DECLINED", cancellations.get(0).reason());
    }

    @Test
    void duplicateRedeliveryIsSkippedByTheInbox() {
        String envelope = """
                {"specversion":"1.0","id":"evt-pr-dup","source":"Payment","type":"PaymentReceived",
                 "subject":"PAY-3","datacontenttype":"application/json",
                 "data":{"paymentId":"PAY-3","orderRef":"CHK-3",
                         "allocations":[{"orderId":"O-5","merchantId":"M-1","amount":10000}]}}
                """;
        consumer.consume(record(envelope), TOPIC);
        consumer.consume(record(envelope), TOPIC);

        assertEquals(1, transitions.size());
    }

    private ConsumerRecord<String, byte[]> record(String envelope) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "k", envelope.getBytes(StandardCharsets.UTF_8));
    }
}
