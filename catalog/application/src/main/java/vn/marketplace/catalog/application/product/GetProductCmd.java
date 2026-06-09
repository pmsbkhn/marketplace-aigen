package vn.marketplace.catalog.application.product;

import vn.marketplace.catalog.domain.shared.Actor;

/** Request product detail. {@code caller} drives visibility (tenant scope + moderation gate + IDOR). */
public record GetProductCmd(String productId, Actor caller) {
}
