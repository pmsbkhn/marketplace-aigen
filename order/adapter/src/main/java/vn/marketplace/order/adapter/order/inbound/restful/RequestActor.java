package vn.marketplace.order.adapter.order.inbound.restful;

import vn.marketplace.order.domain.shared.Actor;
import vn.marketplace.order.domain.shared.Role;

/**
 * Builds a domain {@link Actor} from gateway-forwarded identity headers ({@code X-User-Id} /
 * {@code X-User-Role}). In production the API Gateway verifies the JWT and forwards the claims;
 * identity is never taken from the request body.
 */
final class RequestActor {

    private RequestActor() {
    }

    static Actor from(String userId, String role) {
        String id = (userId == null || userId.isBlank()) ? "anonymous" : userId;
        return new Actor(id, parseRole(role));
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
