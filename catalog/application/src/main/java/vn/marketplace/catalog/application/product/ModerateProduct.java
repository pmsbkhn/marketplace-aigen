package vn.marketplace.catalog.application.product;

/** Use-case port: an Admin approves or rejects a PENDING product (moderation gate). */
public interface ModerateProduct {
    ProductModerationView execute(ModerateProductCmd cmd);
}
