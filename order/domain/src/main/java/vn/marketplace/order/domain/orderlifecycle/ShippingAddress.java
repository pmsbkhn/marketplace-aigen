package vn.marketplace.order.domain.orderlifecycle;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * Buyer shipping address (PII). In production this is stored field-level-encrypted (AES-256/KMS) and
 * never logged in plaintext; here it is carried as a value object.
 */
public record ShippingAddress(String fullName,
                              String phone,
                              String addressLine,
                              String ward,
                              String district,
                              String city) implements DomainValue {
}
