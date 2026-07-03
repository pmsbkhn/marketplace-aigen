package vn.marketplace.order.adapter.order.inbound.restful;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.marketplace.order.application.order.OrderIdView;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of the synchronous {@code Order.CreatePendingOrder} HTTP contract — the REST
 * stand-in for the gRPC call Checkout makes. Mirrors the JSON event contracts (see
 * {@code contracts/README.md}): writes {@code contracts/http/Order.CreatePendingOrder.response.json},
 * a committed, deterministic fixture; Checkout's binding test binds {@code OrderIdBody} from it.
 */
class CreatePendingOrderHttpContractTest {

    static final Path HTTP_CONTRACTS = Path.of("..", "..", "contracts", "http");
    static final ObjectMapper MAPPER =
            new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static final String FIXED_TIMESTAMP = "2026-06-12T12:00:00.000Z";

    static Path writeResponseFixture(String rpc, int status, Object data) throws Exception {
        ObjectNode envelope = MAPPER.createObjectNode();
        envelope.put("status", status);
        envelope.put("message", "Success");
        envelope.put("timestamp", FIXED_TIMESTAMP);
        envelope.set("data", MAPPER.valueToTree(data));
        Files.createDirectories(HTTP_CONTRACTS);
        Path fixture = HTTP_CONTRACTS.resolve(rpc + ".response.json");
        Files.writeString(fixture, MAPPER.writeValueAsString(envelope));
        return fixture;
    }

    @Test
    void publishesTheCreatePendingOrderContract() throws Exception {
        // 201 CREATED, as InternalOrderController returns.
        OrderIdView view = new OrderIdView("O-1", "PENDING", 130_000);

        Path fixture = writeResponseFixture("Order.CreatePendingOrder", 201, view);

        assertTrue(Files.readString(fixture).contains("\"orderId\":\"O-1\""));
    }
}
