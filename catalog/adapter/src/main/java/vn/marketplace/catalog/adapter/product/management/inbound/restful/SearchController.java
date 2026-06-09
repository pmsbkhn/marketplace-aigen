package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.catalog.adapter.product.management.facade.ProductFacade;
import vn.marketplace.catalog.application.product.ProductSearchResult;
import vn.marketplace.catalog.application.product.SearchProductsCmd;

/**
 * Public storefront search. Pagination is 0-based ({@code page=0} = first page) — the use-case clamps
 * negatives and caps the size.
 */
@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductFacade facade;

    @GetMapping
    public ResponseEntity<CommonHttpResponse> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "priceMin", required = false) Long priceMin,
            @RequestParam(value = "priceMax", required = false) Long priceMax,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "24") int size) {
        ProductSearchResult result = facade.search(
                new SearchProductsCmd(q, category, brand, priceMin, priceMax, page, size));
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, result));
    }
}
