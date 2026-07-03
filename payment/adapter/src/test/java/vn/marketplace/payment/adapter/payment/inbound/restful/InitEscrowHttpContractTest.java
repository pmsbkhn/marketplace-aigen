package vn.marketplace.payment.adapter.payment.inbound.restful;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.marketplace.payment.application.payment.InitEscrowResult;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of the synchronous {@code Payment.InitEscrow} HTTP contract — the REST stand-in for
 * the gRPC call Checkout makes. Mirrors the JSON event contracts (see {@code contracts/README.md}):
 * writes {@code contracts/http/Payment.InitEscrow.response.json}, a committed, deterministic fixture
 * (pinned {@code timestamp}); Checkout's binding test binds {@code EscrowBody} from it.
 *
 * <p>{@code expiresAt} is a {@code LocalDateTime}; the {@code data} node is built explicitly (rather
 * than reflectively) so the fixture carries the ISO string the real Spring Jackson emits, without a
 * java-time datatype module on the test classpath. Checkout reads {@code expiresAt} as an opaque
 * String.</p>
 */
class InitEscrowHttpContractTest {

    static final Path HTTP_CONTRACTS = Path.of("..", "..", "contracts", "http");
    static final ObjectMapper MAPPER = new ObjectMapper();
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
    void publishesTheInitEscrowContract() throws Exception {
        // 201 CREATED, as InternalPaymentController returns; escrowId == paymentId.
        InitEscrowResult result = new InitEscrowResult(
                "PAY-1", "https://pay.example/redirect/PAY-1", LocalDateTime.of(2026, 6, 12, 12, 30, 0));

        ObjectNode data = MAPPER.createObjectNode();
        data.put("escrowId", result.escrowId());
        data.put("paymentUrl", result.paymentUrl());
        data.put("expiresAt", result.expiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

        Path fixture = writeResponseFixture("Payment.InitEscrow", 201, data);

        assertTrue(Files.readString(fixture).contains("\"escrowId\":\"PAY-1\""));
    }
}
