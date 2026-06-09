package vn.marketplace.inventory.adapter.stock.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.inventory.adapter.stock.dto.OrderCompletedEvent;
import vn.marketplace.inventory.adapter.stock.dto.ProductCreatedEvent;
import vn.marketplace.inventory.adapter.stock.facade.StockFacade;
import vn.marketplace.inventory.application.stock.DeductStockCmd;
import vn.marketplace.inventory.application.stock.InitSkuCmd;

/**
 * Inbound event handlers. In production these are Kafka consumers ({@code ProductCreated} from Catalog,
 * {@code OrderCompleted} from Order) via an msfw {@code FiveStepsPipelineFactory}; here they are REST
 * stand-ins so the flow is exercisable on the standalone profile. Both use-cases are idempotent.
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InventoryEventController {

    private final StockFacade facade;

    @PostMapping("/product-created")
    public ResponseEntity<CommonHttpResponse> productCreated(@RequestBody ProductCreatedEvent event) {
        facade.init(new InitSkuCmd(event.eventId(), event.productId(), event.skuCodes(), event.merchantId()));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "initialised"));
    }

    @PostMapping("/order-completed")
    public ResponseEntity<CommonHttpResponse> orderCompleted(@RequestBody OrderCompletedEvent event) {
        facade.deduct(new DeductStockCmd(event.eventId(), event.orderId(), event.skus()));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "deducted"));
    }
}
