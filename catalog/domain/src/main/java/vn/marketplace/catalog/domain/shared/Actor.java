package vn.marketplace.catalog.domain.shared;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * The authenticated principal performing a domain action, carrying its {@link Role}.
 *
 * <p>At the adapter boundary this is built from JWT claims (subject + role/scope). The domain only
 * sees identity + role, never the network position — consistent with the zero-trust SAD.
 */
public record Actor(String id, Role role) implements DomainValue {
    public Actor {
        Objects.requireNonNull(id, "actor id cannot be null");
        Objects.requireNonNull(role, "actor role cannot be null");
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
