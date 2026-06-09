package vn.marketplace.catalog.adapter.product.management.dto;

/** Body for POST /v1/admin/products/{id}/reject: the rejection {@code reason} (required by the domain). */
public record RejectProductRequest(String reason) {
}
