package vn.marketplace.catalog.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainException;

/**
 * Single domain exception type for the Catalog context, parameterised by a {@link CatalogErrorCode}.
 *
 * <p>Extends msfw's abstract {@link DomainException} so the framework's GlobalExceptionHandler maps
 * it to the right HTTP status (via the base {@code DomainErrorCode}); the precise business reason is
 * carried by {@link #catalogErrorCode()}.
 */
public class CatalogDomainException extends DomainException {

    private final CatalogErrorCode catalogErrorCode;

    public CatalogDomainException(CatalogErrorCode code) {
        super(code.baseErrorCode(), code.defaultMessage());
        this.catalogErrorCode = code;
    }

    public CatalogDomainException(CatalogErrorCode code, String message) {
        super(code.baseErrorCode(), message);
        this.catalogErrorCode = code;
    }

    public CatalogErrorCode catalogErrorCode() {
        return catalogErrorCode;
    }
}
