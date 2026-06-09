package vn.marketplace.catalog.adapter.product.management.inbound.restful;

import vn.marketplace.catalog.domain.shared.Actor;
import vn.marketplace.catalog.domain.shared.Role;

/**
 * Builds a domain {@link Actor} from gateway-forwarded identity headers.
 *
 * <p>In production the API Gateway verifies the JWT and forwards the verified claims; here we read
 * {@code X-User-Id} / {@code X-User-Role} as a stand-in. Identity is never taken from the request body.
 */
final class RequestActor {

    private RequestActor() {
    }

    static Actor from(String userId, String role) {
        String id = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        Role parsed = parseRole(role);
        return new Actor(id, parsed);
    }

    private static Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return Role.BUYER;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.BUYER;
        }
    }
}
