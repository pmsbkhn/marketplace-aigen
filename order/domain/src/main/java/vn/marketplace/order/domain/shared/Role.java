package vn.marketplace.order.domain.shared;

/** The three marketplace roles, used by the domain to enforce authority/visibility rules. */
public enum Role {
    ADMIN,
    MERCHANT,
    BUYER
}
