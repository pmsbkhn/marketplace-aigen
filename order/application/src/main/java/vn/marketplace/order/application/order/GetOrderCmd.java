package vn.marketplace.order.application.order;

import vn.marketplace.order.domain.shared.Actor;

/** Request order detail. {@code caller} drives visibility (tenant scope + IDOR-as-404). */
public record GetOrderCmd(String orderId, Actor caller) {
}
