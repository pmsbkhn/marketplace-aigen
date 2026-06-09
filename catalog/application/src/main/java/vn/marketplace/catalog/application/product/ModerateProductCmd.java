package vn.marketplace.catalog.application.product;

import vn.marketplace.catalog.domain.shared.Actor;

/**
 * Command for the moderation endpoint (POST /v1/admin/products/{id}/approve). {@code moderator} is
 * built from the caller's JWT at the adapter; the domain enforces the Admin-only rule.
 */
public record ModerateProductCmd(String productId, Actor moderator, ModerationAction action, String reason) {
}
