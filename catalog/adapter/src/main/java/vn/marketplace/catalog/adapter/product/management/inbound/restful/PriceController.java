package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.catalog.adapter.product.management.facade.ProductFacade;
import vn.marketplace.catalog.application.product.SkuPriceView;

/**
 * Internal price lookup for Checkout. The SAD specifies gRPC {@code Catalog.GetPrice}; this REST
 * endpoint is the transport stand-in for the standalone profile. Price comes from the DB source of
 * truth (never ES).
 */
@RestController
@RequestMapping("/internal/prices")
@RequiredArgsConstructor
public class PriceController {

    private final ProductFacade facade;

    @PostMapping
    public ResponseEntity<CommonHttpResponse> prices(@RequestBody GetPriceRequest request) {
        List<SkuPriceView> prices = facade.prices(request.skuCodes());
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, prices));
    }

    public record GetPriceRequest(List<String> skuCodes) {
    }
}
