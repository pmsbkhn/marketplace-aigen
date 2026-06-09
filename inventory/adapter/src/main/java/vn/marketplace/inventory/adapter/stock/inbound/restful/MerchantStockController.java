package vn.marketplace.inventory.adapter.stock.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.inventory.adapter.stock.dto.UpdateStockRequest;
import vn.marketplace.inventory.adapter.stock.facade.StockFacade;
import vn.marketplace.inventory.application.stock.StockView;

/**
 * Merchant stock management. Tenant scope (owning merchant only) is enforced in the domain; this
 * controller only forwards the verified merchant id from the gateway header. No try/catch → 500.
 */
@RestController
@RequestMapping("/v1/merchant/stock")
@RequiredArgsConstructor
public class MerchantStockController {

    private final StockFacade facade;

    @PutMapping("/{sku}")
    public ResponseEntity<CommonHttpResponse> updateStock(
            @PathVariable String sku,
            @RequestBody UpdateStockRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String merchantId) {
        StockView view = facade.updateMerchantStock(sku, request.available(), merchantId);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }
}
