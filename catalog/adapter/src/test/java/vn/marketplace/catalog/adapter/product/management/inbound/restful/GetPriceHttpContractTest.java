package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.marketplace.catalog.application.product.SkuPriceView;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of the synchronous {@code Catalog.GetPrice} HTTP contract — the REST stand-in for
 * the gRPC call Checkout makes. Mirrors the JSON event contracts (see {@code contracts/README.md}):
 * writes {@code contracts/http/Catalog.GetPrice.response.json}, a committed, deterministic fixture;
 * Checkout's binding test binds a {@code List<PriceLine>} from its {@code data} array.
 */
class GetPriceHttpContractTest {

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
    void publishesTheGetPriceContract() throws Exception {
        // data is a JSON array of price lines (one per requested SKU).
        List<SkuPriceView> prices = List.of(
                new SkuPriceView("SKU-1", 100_000, "VND", "M-1", true),
                new SkuPriceView("SKU-2", 0, "VND", "M-1", false));

        Path fixture = writeResponseFixture("Catalog.GetPrice", 200, prices);

        assertTrue(Files.readString(fixture).contains("\"skuCode\":\"SKU-1\""));
    }
}
