package vn.marketplace.payment.adapter.payment.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import tech.vsf.ptnt.msfw.domain.core.Criteria;
import tech.vsf.ptnt.springcore.persistence.CriteriaSpecifications;
import vn.marketplace.payment.adapter.payment.outbound.persistence.entity.SettlementEntity;

/**
 * TC-PAY-INT-02 — L4 data encryption at rest: persist a settlement carrying the merchant bank
 * account, then read the {@code bank_account_encrypted} column back with NATIVE SQL (H2). The raw
 * column value must be ciphertext (crypto prefix, no plaintext anywhere in it); reading through
 * JPA decrypts transparently.
 */
@DataJpaTest
class BankAccountEncryptionJpaTest {

    private static final String PLAINTEXT_ACCOUNT = "9704123456789012";

    @Autowired
    private SettlementJpaRepository settlements;

    @Autowired
    private JdbcTemplate jdbc;

    private SettlementEntity settlement() {
        SettlementEntity e = new SettlementEntity();
        e.setSettlementId("SET-1");
        e.setOrderId("O-1");
        e.setMerchantId("M-1");
        e.setGrossAmount(1_000_000);
        e.setCommission(20_000);
        e.setNetAmount(980_000);
        e.setCurrency("VND");
        e.setStatus("PROCESSING");
        e.setCreatedAt(LocalDateTime.now());
        e.setPayoutId("PO-1");
        e.setBankAccount(PLAINTEXT_ACCOUNT);
        e.setPayoutAmount(980_000);
        e.setPayoutStatus("PENDING");
        return e;
    }

    @Test
    void bankAccountColumnHoldsOnlyCiphertext() {
        settlements.saveAndFlush(settlement());

        String rawColumn = jdbc.queryForObject(
                "SELECT bank_account_encrypted FROM settlements WHERE settlement_id = 'SET-1'", String.class);

        assertTrue(rawColumn.startsWith(BankAccountCryptoConverter.PREFIX),
                "column must carry the encryption prefix, got: " + rawColumn.substring(0, 10) + "…");
        assertFalse(rawColumn.contains(PLAINTEXT_ACCOUNT), "plaintext account must never reach the DB");
        assertFalse(rawColumn.substring(BankAccountCryptoConverter.PREFIX.length()).contains("9704"),
                "no recognizable account fragment in the ciphertext");
    }

    @Test
    void jpaReadDecryptsTransparently() {
        settlements.saveAndFlush(settlement());

        SettlementEntity reloaded = settlements
                .findOne(CriteriaSpecifications.toSpecification(Criteria.where("settlementId").eq("SET-1")))
                .orElseThrow();

        assertEquals(PLAINTEXT_ACCOUNT, reloaded.getBankAccount());
    }
}
