package vn.marketplace.checkout.adapter.checkout.outbound.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consumer side of the synchronous HTTP contracts Checkout depends on — the exact analogue of the
 * JSON <b>event</b> {@code *ContractBindingTest}s, for the REST stand-ins of the gRPC calls. Binds
 * each outbound client's wire DTO from the producer's committed fixture in {@code contracts/http/}
 * with the same {@code data}-unwrap the real {@link RestClient} does, then asserts the values.
 *
 * <p>Value assertions — not just the bind — are what catch a silently renamed provider field on
 * these {@code @JsonIgnoreProperties(ignoreUnknown = true)} DTOs: a rename still binds, but the
 * field goes null/empty and an assertion here fails at build time instead of at runtime in the saga.</p>
 */
class OutboundHttpContractBindingTest {

    private static final Path HTTP_CONTRACTS = Path.of("..", "..", "contracts", "http");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The {@code data} payload of a committed response fixture (the part {@link Envelope} unwraps). */
    private JsonNode data(String rpc) throws Exception {
        JsonNode root = MAPPER.readTree(Files.readString(HTTP_CONTRACTS.resolve(rpc + ".response.json")));
        assertTrue(root.hasNonNull("data"), "fixture missing 'data': " + rpc);
        return root.get("data");
    }

    @Test
    void bindsInventoryReserveStock() throws Exception {
        InventoryClientOa.ReserveResult result =
                MAPPER.treeToValue(data("Inventory.ReserveStock"), InventoryClientOa.ReserveResult.class);

        assertEquals("CHK-1", result.reservationId());
        assertTrue(result.allReserved());
        assertEquals(1, result.details().size());
        assertEquals("SKU-1", result.details().get(0).sku());
        assertEquals("OK", result.details().get(0).status());
        assertEquals(42, result.details().get(0).available());
    }

    @Test
    void bindsPaymentInitEscrow() throws Exception {
        PaymentClientOa.EscrowBody body =
                MAPPER.treeToValue(data("Payment.InitEscrow"), PaymentClientOa.EscrowBody.class);

        assertEquals("PAY-1", body.escrowId());
        assertEquals("https://pay.example/redirect/PAY-1", body.paymentUrl());
        assertEquals("2026-06-12T12:30:00", body.expiresAt());
    }

    @Test
    void bindsOrderCreatePendingOrder() throws Exception {
        OrderClientOa.OrderIdBody body =
                MAPPER.treeToValue(data("Order.CreatePendingOrder"), OrderClientOa.OrderIdBody.class);

        assertEquals("O-1", body.orderId());
        assertEquals("PENDING", body.status());
        assertEquals(130_000, body.totalAmount());
    }

    @Test
    void bindsCatalogGetPrice() throws Exception {
        List<CatalogClientOa.PriceLine> lines =
                MAPPER.convertValue(data("Catalog.GetPrice"), new TypeReference<List<CatalogClientOa.PriceLine>>() {});

        assertEquals(2, lines.size());
        assertEquals("SKU-1", lines.get(0).skuCode());
        assertEquals(100_000, lines.get(0).priceAmount());
        assertEquals("VND", lines.get(0).currency());
        assertEquals("M-1", lines.get(0).merchantId());
        assertTrue(lines.get(0).active());
        assertFalse(lines.get(1).active()); // SKU-2: missing/inactive → active=false
    }
}
