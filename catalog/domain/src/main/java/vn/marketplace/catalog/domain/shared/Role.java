package vn.marketplace.catalog.domain.shared;

/**
 * The three marketplace roles. Used by the domain to enforce authority rules (e.g. moderation is
 * an Admin-only action) independently of any transport/JWT concern.
 */
public enum Role {
    ADMIN,
    MERCHANT,
    BUYER
}
