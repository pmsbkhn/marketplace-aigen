package vn.marketplace.inventory.domain.shared;

import tech.vsf.ptnt.msfw.domain.exception.DomainException;

/**
 * Single domain exception type for the Inventory context, parameterised by an {@link InventoryErrorCode}.
 *
 * <p>Extends msfw's abstract {@link DomainException} so the framework's GlobalExceptionHandler maps it
 * to the right HTTP status (via the base {@code DomainErrorCode}); the precise business reason is
 * carried by {@link #inventoryErrorCode()}.
 */
public class InventoryDomainException extends DomainException {

    private final InventoryErrorCode inventoryErrorCode;

    public InventoryDomainException(InventoryErrorCode code) {
        super(code.baseErrorCode(), code.defaultMessage());
        this.inventoryErrorCode = code;
    }

    public InventoryDomainException(InventoryErrorCode code, String message) {
        super(code.baseErrorCode(), message);
        this.inventoryErrorCode = code;
    }

    public InventoryErrorCode inventoryErrorCode() {
        return inventoryErrorCode;
    }
}
