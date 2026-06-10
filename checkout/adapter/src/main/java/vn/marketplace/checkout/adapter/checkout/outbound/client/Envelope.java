package vn.marketplace.checkout.adapter.checkout.outbound.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The {@code CommonHttpResponse} wrapper every sibling service returns — we only need {@code data}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Envelope<T>(T data) {
}
