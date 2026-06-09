package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.catalog.adapter.product.management.dto.CreateProductRequest;
import vn.marketplace.catalog.adapter.product.management.dto.UpdateSkuPriceRequest;
import vn.marketplace.catalog.adapter.product.management.facade.ProductFacade;
import vn.marketplace.catalog.application.product.ProductIdView;
import vn.marketplace.catalog.application.product.ProductView;
import vn.marketplace.catalog.application.product.SkuPriceView;
import vn.marketplace.catalog.domain.shared.Actor;

/**
 * Merchant/Buyer product endpoints. No try/catch → 500: domain exceptions propagate to msfw's
 * GlobalExceptionHandler, which maps them to the right HTTP status + {@link CommonHttpResponse}.
 */
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductFacade facade;

    @PostMapping
    public ResponseEntity<CommonHttpResponse> create(
            @RequestBody CreateProductRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Actor merchant = RequestActor.from(userId, role);
        ProductIdView view = facade.create(request, merchant);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CommonHttpResponse(HttpStatus.CREATED, view));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonHttpResponse> get(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Actor caller = RequestActor.from(userId, role);
        ProductView view = facade.get(id, caller);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }

    @PutMapping("/{id}/skus/{skuCode}")
    public ResponseEntity<CommonHttpResponse> updateSkuPrice(
            @PathVariable String id,
            @PathVariable String skuCode,
            @RequestBody UpdateSkuPriceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Actor caller = RequestActor.from(userId, role);
        SkuPriceView view = facade.updateSkuPrice(id, skuCode, request, caller);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }
}
