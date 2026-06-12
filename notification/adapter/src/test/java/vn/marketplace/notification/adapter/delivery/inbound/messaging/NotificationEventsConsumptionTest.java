package vn.marketplace.notification.adapter.delivery.inbound.messaging;

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
import vn.marketplace.notification.application.delivery.AcceptNotificationCmd;
import vn.marketplace.notification.application.delivery.DispatchNotificationCmd;
import vn.marketplace.notification.application.delivery.NotificationIdView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Full consumption slice without a broker: PaymentReceived → one merchant notification per
 * allocation (accepted + dispatched), with the spec'd idempotency key shape.
 */
class NotificationEventsConsumptionTest {

    private static final String TOPIC = "payment-events";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<AcceptNotificationCmd> accepted = new ArrayList<>();
    private final List<DispatchNotificationCmd> dispatched = new ArrayList<>();
    private SpringKafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SpringKafkaEventConsumer(new DefaultErrorClassifier(), new ConsumptionMetrics(null));

        RoutingManager routingManager = new RoutingManager();
        routingManager.appendConfig("Payment", "PaymentReceived", TOPIC, "JSON");

        PostProcessor postProcessor =
                new DefaultPostProcessor(new LoggingResultStore(), new JacksonDataToJsonConverter(objectMapper));

        NotificationEventsFacade facade = new NotificationEventsFacade(
                cmd -> {
                    accepted.add(cmd);
                    return new NotificationIdView("N-" + accepted.size(), "PENDING");
                },
                dispatched::add);

        new PaymentReceivedPipelineFactory(consumer, postProcessor, new InMemoryIdempotencyGuard(),
                routingManager, new JsonCloudEventDeserializer(objectMapper),
                new AvroCloudEventDeserializer(null), objectMapper, facade);
    }

    @Test
    void paymentReceivedNotifiesEachMerchantAndDispatches() {
        String envelope = """
                {"specversion":"1.0","id":"evt-pr-9","source":"Payment","type":"PaymentReceived",
                 "subject":"PAY-9","datacontenttype":"application/json",
                 "data":{"paymentId":"PAY-9","orderRef":"CHK-9","amount":130000,
                         "allocations":[{"orderId":"O-1","merchantId":"M-1","amount":50000},
                                        {"orderId":"O-2","merchantId":"M-2","amount":80000}]}}
                """;
        consumer.consume(record(envelope), TOPIC);

        assertEquals(2, accepted.size());
        AcceptNotificationCmd first = accepted.get(0);
        assertEquals("PaymentReceived:evt-pr-9:MERCHANT:O-1", first.idempotencyKey());
        assertEquals("M-1", first.userId());
        assertEquals("AUTO", first.channel());
        assertEquals(NotificationEventsFacade.PAYMENT_RECEIVED_MERCHANT_TEMPLATE, first.templateId());
        assertEquals("50000", first.variables().get("amount"));
        assertEquals("TRANSACTIONAL", first.priority());
        assertEquals("evt-pr-9", first.sourceEventId());
        assertEquals(2, dispatched.size());
        assertEquals("N-1", dispatched.get(0).notificationId());
    }

    @Test
    void duplicateRedeliveryIsSkippedByTheInbox() {
        String envelope = """
                {"specversion":"1.0","id":"evt-pr-dup","source":"Payment","type":"PaymentReceived",
                 "subject":"PAY-10","datacontenttype":"application/json",
                 "data":{"paymentId":"PAY-10","orderRef":"CHK-10","amount":10000,
                         "allocations":[{"orderId":"O-3","merchantId":"M-1","amount":10000}]}}
                """;
        consumer.consume(record(envelope), TOPIC);
        consumer.consume(record(envelope), TOPIC);

        assertEquals(1, accepted.size());
    }

    private ConsumerRecord<String, byte[]> record(String envelope) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "k", envelope.getBytes(StandardCharsets.UTF_8));
    }
}
