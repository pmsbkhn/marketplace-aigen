package vn.marketplace.payment.adapter.payment.outbound.gateway;

import org.springframework.stereotype.Component;

/**
 * Resolves a merchant's payout bank account. Stand-in for the Identity/merchant-profile context
 * (which owns merchant banking data) — deterministic per merchant so the standalone flow works
 * end-to-end. The resolved value is L4 data: passed straight into the settlement aggregate and
 * persisted encrypted; never logged.
 */
@Component
public class MerchantBankDirectory {

    public String bankAccountFor(String merchantId) {
        return String.format("9704%012d", Math.abs((long) merchantId.hashCode()));
    }
}
