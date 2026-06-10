package vn.marketplace.order.domain.shared;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * The authenticated principal performing an order action, carrying its {@link Role}. Built from JWT
 * claims at the adapter; the domain only sees identity + role.
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
