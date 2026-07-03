package vn.marketplace.inventory.adapter.stock.inbound.restful;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.marketplace.inventory.application.stock.ReserveStockResult;
import vn.marketplace.inventory.application.stock.ReserveStockResult.ItemReserveDetail;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of the synchronous {@code Inventory.ReserveStock} HTTP contract — the REST stand-in
 * for the gRPC call Checkout makes. Mirrors the JSON <b>event</b> contracts (see
 * {@code <repo>/contracts/README.md}): serialises a representative {@code CommonHttpResponse} body
 * into {@code <repo>/contracts/http/Inventory.ReserveStock.response.json}, a committed fixture whose
 * git diff IS the contract-change signal. Checkout's {@code OutboundHttpContractBindingTest} binds
 * its wire DTO from it. The {@code timestamp} is pinned (the real envelope stamps {@code now()}) so
 * the fixture is deterministic — a re-run rewrites an identical file unless the shape changed.
 */
class ReserveStockHttpContractTest {

    static final Path HTTP_CONTRACTS = Path.of("..", "..", "contracts", "http");
    static final ObjectMapper MAPPER =
            new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    static final String FIXED_TIMESTAMP = "2026-06-12T12:00:00.000Z";

    /** Writes the deterministic {@code CommonHttpResponse}-shaped envelope fixture for one response. */
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
    void publishesTheReserveStockContract() throws Exception {
        ReserveStockResult result = new ReserveStockResult("CHK-1", true,
                List.of(new ItemReserveDetail("SKU-1", ItemReserveDetail.OK, 42)));

        Path fixture = writeResponseFixture("Inventory.ReserveStock", 200, result);

        assertTrue(Files.readString(fixture).contains("\"reservationId\":\"CHK-1\""));
    }
}
