package vn.marketplace.catalog.application.product;

import java.time.LocalDateTime;

/** Result of a moderation decision. */
public record ProductModerationView(String id,
                                    String status,
                                    String moderatedBy,
                                    String moderationReason,
                                    LocalDateTime moderatedAt) {
}
