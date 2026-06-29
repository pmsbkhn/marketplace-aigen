package vn.marketplace.catalog.domain.product;

import tech.vsf.ptnt.msfw.domain.exception.InvalidArgumentException;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.DomainValue;

/**
 * A product image reference (S3 URL) with display ordering. EXIF/metadata stripping and CDN
 * fronting are adapter/infra concerns; the domain only holds the URL + sort order.
 */
public record ProductImage(String url, int sortOrder) implements DomainValue {
    public ProductImage {
        Objects.requireNonNull(url, "image url cannot be null");
        if (url.isBlank()) {
            throw new InvalidArgumentException("image url cannot be blank");
        }
        if (sortOrder < 0) {
            throw new InvalidArgumentException("sortOrder cannot be negative");
        }
    }
}
