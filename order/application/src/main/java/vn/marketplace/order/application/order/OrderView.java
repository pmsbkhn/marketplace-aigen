package vn.marketplace.order.application.order;

import java.time.LocalDateTime;
import java.util.List;

/** Detailed read model for one order (owner/admin only — see {@code GetOrderUc} visibility). */
public record OrderView(String orderId,
                        String checkoutRef,
                        String buyerId,
                        String merchantId,
                        String status,
                        long totalAmount,
                        String currency,
                        String trackingNumber,
                        String cancelReason,
                        AddressView shippingAddress,
                        List<ItemView> items,
                        List<HistoryView> statusHistory,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {

    public record AddressView(String fullName, String phone, String addressLine,
                              String ward, String district, String city) {
    }

    public record ItemView(String skuCode, String productName, long priceSnapshot, int qty, long subtotal) {
    }

    public record HistoryView(String fromStatus, String toStatus, String triggeredBy,
                              String reason, LocalDateTime timestamp) {
    }
}
