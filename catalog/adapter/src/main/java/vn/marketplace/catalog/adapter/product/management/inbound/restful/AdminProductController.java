package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.vsf.ptnt.springcore.web.common.CommonHttpResponse;
import vn.marketplace.catalog.adapter.product.management.dto.RejectProductRequest;
import vn.marketplace.catalog.adapter.product.management.facade.ModerationFacade;
import vn.marketplace.catalog.application.product.ProductModerationView;
import vn.marketplace.catalog.domain.shared.Actor;

/**
 * Admin moderation endpoints (approve / reject). Authority (Admin-only) is enforced in the domain;
 * this controller only forwards the verified actor. No try/catch → 500.
 */
@RestController
@RequestMapping("/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ModerationFacade facade;

    @PostMapping("/{id}/approve")
    public ResponseEntity<CommonHttpResponse> approve(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Actor moderator = RequestActor.from(userId, role);
        ProductModerationView view = facade.approve(id, moderator);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CommonHttpResponse> reject(
            @PathVariable String id,
            @RequestBody RejectProductRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Actor moderator = RequestActor.from(userId, role);
        ProductModerationView view = facade.reject(id, request.reason(), moderator);
        return ResponseEntity.ok(new CommonHttpResponse(HttpStatus.OK, view));
    }
}
