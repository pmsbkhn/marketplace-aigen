package vn.marketplace.checkout.adapter.checkout.outbound.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import vn.marketplace.checkout.application.checkout.OrderPort;

/**
 * Outbound client to Order ({@code Order.CreatePendingOrder/CancelPendingOrder}) — REST stand-in
 * for the gRPC + mTLS contract. Cancel is the saga-compensation edge.
 */
@Component
public class OrderClientOa implements OrderPort {

    private final RestClient rest;

    public OrderClientOa(@Value("${checkout.clients.order-url:http://localhost:8082}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String createPendingOrder(CreateOrderDto dto) {
        ShippingAddressDtoBody address = dto.shippingAddress() == null ? null
                : new ShippingAddressDtoBody(dto.shippingAddress().fullName(), dto.shippingAddress().phone(),
                        dto.shippingAddress().addressLine(), dto.shippingAddress().ward(),
                        dto.shippingAddress().district(), dto.shippingAddress().city());
        List<ItemBody> items = dto.items().stream()
                .map(i -> new ItemBody(i.productId(), i.variantId(), i.skuCode(), i.productName(),
                        i.priceSnapshot(), i.currency(), i.quantity()))
                .toList();
        Envelope<OrderIdBody> envelope = rest.post()
                .uri("/internal/orders")
                .body(new CreateOrderBody(dto.checkoutRef(), dto.buyerId(), dto.merchantId(), address, items))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return envelope.data().orderId();
    }

    @Override
    public void cancelOrder(String orderId, String reason) {
        rest.post()
                .uri("/internal/orders/{id}/cancel", orderId)
                .body(new CancelBody(reason))
                .retrieve()
                .toBodilessEntity();
    }

    record CreateOrderBody(String checkoutRef, String buyerId, String merchantId,
                           ShippingAddressDtoBody shippingAddress, List<ItemBody> items) {
    }

    record ShippingAddressDtoBody(String fullName, String phone, String addressLine,
                                  String ward, String district, String city) {
    }

    record ItemBody(String productId, String variantId, String skuCode, String productName,
                    long priceSnapshot, String currency, int quantity) {
    }

    record CancelBody(String reason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OrderIdBody(String orderId, String status, long totalAmount) {
    }
}
