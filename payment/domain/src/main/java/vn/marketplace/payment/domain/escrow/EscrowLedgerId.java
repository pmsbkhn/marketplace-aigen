package vn.marketplace.payment.domain.escrow;

import java.util.Objects;

import tech.vsf.ptnt.msfw.domain.core.Identity;

public final class EscrowLedgerId extends Identity<String> {

    private final String value;

    public EscrowLedgerId(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EscrowLedgerId other && Objects.equals(value, other.value);
    }
}
