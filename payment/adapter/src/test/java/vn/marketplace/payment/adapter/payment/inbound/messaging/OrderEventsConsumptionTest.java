package vn.marketplace.payment.adapter.payment.inbound.messaging;

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
import vn.marketplace.payment.adapter.payment.dto.OrderCompletedEvent;
import vn.marketplace.payment.adapter.payment.facade.PaymentFacade;
import vn.marketplace.payment.adapter.payment.outbound.gateway.MerchantBankDirectory;
import vn.marketplace.payment.application.payment.ProcessPayoutCmd;
import vn.marketplace.payment.application.payment.ProcessSettlementCmd;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full consumption slice without a broker: OrderCompleted → settlement (with adapter-resolved
 * merchant bank account) → payout, both recorded through the facade.
 */
class OrderEventsConsumptionTest {

    private static final String TOPIC = "order-events";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ProcessSettlementCmd> settlements = new ArrayList<>();
    private final List<ProcessPayoutCmd> payouts = new ArrayList<>();
    private SpringKafkaEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SpringKafkaEventConsumer(new DefaultErrorClassifier(), new ConsumptionMetrics(null));

        RoutingManager routingManager = new RoutingManager();
        routingManager.appendConfig("Order", "OrderCompleted", TOPIC, "JSON");

        PostProcessor postProcessor =
                new DefaultPostProcessor(new LoggingResultStore(), new JacksonDataToJsonConverter(objectMapper));

        PaymentFacade paymentFacade = new PaymentFacade(
                cmd -> { throw new UnsupportedOperationException(); },
                cmd -> { throw new UnsupportedOperationException(); },
                cmd -> { settlements.add(cmd); return null; },
                cmd -> { payouts.add(cmd); return null; },
                cmd -> { throw new UnsupportedOperationException(); },
                new MerchantBankDirectory());

        ConsumptionPipelines pipelines = new ConsumptionPipelines(consumer, postProcessor,
                new InMemoryIdempotencyGuard(), routingManager, new JsonCloudEventDeserializer(objectMapper),
                new AvroCloudEventDeserializer(null), objectMapper, new UpcasterChain(java.util.List.of()));
        pipelines.subscribe("Order", "OrderCompleted", OrderCompletedEvent.class)
                .handle(new OrderEventsFacade(paymentFacade), "onOrderCompleted");
    }

    @Test
    void orderCompletedSettlesAndPaysOut() {
        String envelope = """
                {"specversion":"1.0","id":"evt-oc-9","source":"Order","type":"OrderCompleted",
                 "subject":"O-9","datacontenttype":"application/json",
                 "data":{"orderId":"O-9","merchantId":"M-9",
                         "items":[{"sku":"SKU-A","qty":2,"priceSnapshot":50000}]}}
                """;
        consumer.consume(record(envelope), TOPIC);

        assertEquals(1, settlements.size());
        ProcessSettlementCmd cmd = settlements.get(0);
        assertEquals("O-9", cmd.orderId());
        assertEquals("M-9", cmd.merchantId());
        assertEquals(1, cmd.items().size());
        assertEquals("SKU-A", cmd.items().get(0).skuCode());
        assertEquals(2, cmd.items().get(0).quantity());
        assertNotNull(cmd.merchantBankAccount(), "bank account resolved by the adapter directory");
        assertEquals(1, payouts.size());
        assertEquals("O-9", payouts.get(0).orderId());
    }

    @Test
    void duplicateRedeliveryIsSkippedByTheInbox() {
        String envelope = """
                {"specversion":"1.0","id":"evt-oc-dup","source":"Order","type":"OrderCompleted",
                 "subject":"O-10","datacontenttype":"application/json",
                 "data":{"orderId":"O-10","merchantId":"M-9","items":[]}}
                """;
        consumer.consume(record(envelope), TOPIC);
        consumer.consume(record(envelope), TOPIC);

        assertEquals(1, settlements.size());
    }

    private ConsumerRecord<String, byte[]> record(String envelope) {
        return new ConsumerRecord<>(TOPIC, 0, 0L, "k", envelope.getBytes(StandardCharsets.UTF_8));
    }
}
