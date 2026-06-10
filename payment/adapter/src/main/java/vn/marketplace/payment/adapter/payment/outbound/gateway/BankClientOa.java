package vn.marketplace.payment.adapter.payment.outbound.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import vn.marketplace.payment.application.payment.BankPort;

/**
 * Outbound adapter to the merchant bank (payout). Production: bank API over HTTPS from the
 * restricted-egress subnet; standalone: records the instruction in the log — with the account
 * number masked to its last 4 digits, the full number (L4) is never logged.
 */
@Component
public class BankClientOa implements BankPort {

    private static final Logger log = LoggerFactory.getLogger(BankClientOa.class);

    @Override
    public void submitPayout(String payoutId, String merchantId, String bankAccount, long amount,
                             String currency) {
        log.info("Payout {} submitted: merchant={}, account=****{}, amount={} {}",
                payoutId, merchantId, last4(bankAccount), amount, currency);
    }

    private String last4(String account) {
        return account == null || account.length() <= 4 ? "????"
                : account.substring(account.length() - 4);
    }
}
