package vn.marketplace.order.adapter.order.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.order.adapter.order.dto.CancelOrderRequest;
import vn.marketplace.order.adapter.order.dto.CreateOrderRequest;
import vn.marketplace.order.adapter.order.facade.OrderFacade;
import vn.marketplace.order.application.order.CreatePendingOrderCmd;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.OrderItemInput;
import vn.marketplace.order.application.order.CreatePendingOrderCmd.ShippingAddressInput;
import vn.marketplace.order.application.order.OrderIdView;

/**
 * Internal order operations for Checkout. The SAD specifies gRPC {@code Order.CreatePendingOrder} /
 * {@code CancelPendingOrder}; these REST endpoints are the transport stand-in for the standalone
 * profile (consistent with Catalog/Inventory). No try/catch → 500.
 */
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderFacade facade;

    @PostMapping
    public ResponseEntity<CommonHttpResponse> create(@RequestBody CreateOrderRequest request) {
        CreateOrderRequest.Address a = request.shippingAddress();
        ShippingAddressInput address = a == null ? null
                : new ShippingAddressInput(a.fullName(), a.phone(), a.addressLine(), a.ward(), a.district(), a.city());
        var items = request.items().stream()
                .map(i -> new OrderItemInput(i.productId(), i.variantId(), i.skuCode(), i.productName(),
                        i.priceSnapshot(), i.currency(), i.quantity()))
                .toList();
        OrderIdView view = facade.create(new CreatePendingOrderCmd(
                request.checkoutRef(), request.buyerId(), request.merchantId(), address, items));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommonHttpResponse(HttpStatus.CREATED, view));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommonHttpResponse> cancelPending(
            @PathVariable String id,
            @RequestBody CancelOrderRequest request) {
        facade.cancelBySystem(id, request.reason());
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "cancelled"));
    }
}
