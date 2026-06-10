package vn.marketplace.checkout.adapter.checkout.outbound.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import vn.marketplace.checkout.application.checkout.InventoryPort;

/**
 * Outbound client to Inventory ({@code Inventory.ReserveStock/ReleaseStock}) — REST stand-in for
 * the gRPC + mTLS contract.
 */
@Component
public class InventoryClientOa implements InventoryPort {

    private final RestClient rest;

    public InventoryClientOa(@Value("${checkout.clients.inventory-url:http://localhost:8081}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ReservationDto reserveStock(String orderRef, List<ReserveItemDto> items) {
        List<ReserveLine> lines = items.stream()
                .map(i -> new ReserveLine(i.sku(), i.quantity()))
                .toList();
        Envelope<ReserveResult> envelope = rest.post()
                .uri("/internal/reservations")
                .body(new ReserveRequest(orderRef, lines))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        ReserveResult result = envelope.data();
        List<ShortageDto> shortages = result.details() == null ? List.of()
                : result.details().stream()
                        .filter(d -> !"OK".equals(d.status()))
                        .map(d -> new ShortageDto(d.sku(), d.available()))
                        .toList();
        return new ReservationDto(result.reservationId(), result.allReserved(), shortages);
    }

    @Override
    public void releaseStock(String reservationId) {
        rest.post()
                .uri("/internal/reservations/release")
                .body(new ReleaseRequest(reservationId))
                .retrieve()
                .toBodilessEntity();
    }

    record ReserveRequest(String orderRef, List<ReserveLine> items) {
    }

    record ReserveLine(String sku, int quantity) {
    }

    record ReleaseRequest(String reservationId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReserveResult(String reservationId, boolean allReserved, List<Detail> details) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Detail(String sku, String status, int available) {
    }
}
