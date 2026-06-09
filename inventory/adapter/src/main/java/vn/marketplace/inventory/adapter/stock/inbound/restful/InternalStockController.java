package vn.marketplace.inventory.adapter.stock.inbound.restful;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.inventory.adapter.stock.dto.ReleaseStockRequest;
import vn.marketplace.inventory.adapter.stock.dto.ReserveStockRequest;
import vn.marketplace.inventory.adapter.stock.dto.StockLevelsRequest;
import vn.marketplace.inventory.adapter.stock.facade.StockFacade;
import vn.marketplace.inventory.application.stock.ReserveStockCmd;
import vn.marketplace.inventory.application.stock.ReserveStockCmd.StockItemInput;
import vn.marketplace.inventory.application.stock.ReserveStockResult;
import vn.marketplace.inventory.application.stock.StockLevelView;

/**
 * Internal stock operations for Checkout (saga) and the storefront. The SAD specifies gRPC
 * {@code Inventory.ReserveStock/ReleaseStock/GetStockLevel}; these REST endpoints are the transport
 * stand-in for the standalone profile (consistent with Catalog's PriceController). No try/catch → 500.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalStockController {

    private final StockFacade facade;

    @PostMapping("/reservations")
    public ResponseEntity<CommonHttpResponse> reserve(@RequestBody ReserveStockRequest request) {
        List<StockItemInput> items = request.items().stream()
                .map(i -> new StockItemInput(i.sku(), i.quantity()))
                .toList();
        ReserveStockResult result = facade.reserve(new ReserveStockCmd(request.orderRef(), items));
        // 200 with allReserved flag; the caller (Checkout) treats allReserved=false as a fail-fast.
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, result));
    }

    @PostMapping("/reservations/release")
    public ResponseEntity<CommonHttpResponse> release(@RequestBody ReleaseStockRequest request) {
        facade.release(request.reservationId());
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, "released"));
    }

    @PostMapping("/stock-levels")
    public ResponseEntity<CommonHttpResponse> levels(@RequestBody StockLevelsRequest request) {
        List<StockLevelView> levels = facade.levels(request.skuCodes());
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, levels));
    }
}
