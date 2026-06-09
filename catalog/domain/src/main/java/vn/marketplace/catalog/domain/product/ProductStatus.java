package vn.marketplace.catalog.domain.product;

/**
 * Product moderation state machine:
 * <pre>
 *   PENDING --approve--> ACTIVE --deactivate--> DEACTIVATED (terminal)
 *   PENDING --reject---> REJECTED --resubmit--> PENDING
 * </pre>
 * Only an Admin may move PENDING → ACTIVE/REJECTED (moderation gate).
 */
public enum ProductStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    DEACTIVATED
}
