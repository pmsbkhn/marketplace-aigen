package vn.marketplace.payment.domain.payment.management;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.DomainEventPublisher;
import tech.vsf.ptnt.msfw.domain.type.DTime;
import tech.vsf.ptnt.msfw.test.JsonEventContract;
import vn.marketplace.payment.domain.payment.Currency;
import vn.marketplace.payment.domain.payment.MerchantId;
import vn.marketplace.payment.domain.payment.Money;
import vn.marketplace.payment.domain.payment.PaymentId;
import vn.marketplace.payment.domain.payment.PayoutId;
import vn.marketplace.payment.domain.payment.SettlementId;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Producer side of Payment's JSON contracts ({@code PaymentReceived}, {@code PaymentFailed},
 * {@code PayoutCompleted}): writes the exact wire envelopes of representative events into
 * {@code <repo>/contracts}. Committed files — a git diff on one IS the contract-change signal;
 * consumers (order, notification) bind from them.
 */
class PaymentEventsContractTest {

    private static final Path CONTRACTS = Path.of("..", "..", "contracts");
    private static final DTime FIXED_TIME = DTime.of(LocalDateTime.of(2026, 6, 12, 12, 0));

    @BeforeEach
    @AfterEach
    void clearEvents() {
        DomainEventPublisher.clear();
    }

    private Payment payment() {
        return Payment.initEscrow(new PaymentId("PAY-1"), "CHK-1", Money.of(130_000, Currency.VND),
                List.of(new Payment.NewAllocation("H-1", "O-1", "M-1", 130_000)));
    }

    private Settlement settlement() {
        // TC-PAY-05 worked example: gross 1.000.000 − 2% commission = net 980.000 payout.
        return Settlement.start(new SettlementId("SET-1"), "O-1", new MerchantId("M-1"),
                Money.of(1_000_000, Currency.VND), CommissionPolicy.standard(),
                new PayoutId("PO-1"), "999912345678");
    }

    @Test
    void publishesThePaymentReceivedContract() throws Exception {
        Payment payment = payment();
        payment.confirmPayment("TXN-1", 130_000); // a confirmed payment carries its gateway txn

        Path fixture = JsonEventContract.writeFixture(new PaymentReceived(payment, FIXED_TIME), CONTRACTS);

        assertTrue(Files.readString(fixture).contains("\"allocations\""));
    }

    @Test
    void publishesThePaymentFailedContract() throws Exception {
        Path fixture = JsonEventContract.writeFixture(
                new PaymentFailed(payment(), "GATEWAY_TIMEOUT", FIXED_TIME), CONTRACTS);

        assertTrue(Files.readString(fixture).contains("\"orderIds\""));
    }

    @Test
    void publishesThePayoutCompletedContract() throws Exception {
        Path fixture = JsonEventContract.writeFixture(
                new PayoutCompleted(settlement(), FIXED_TIME), CONTRACTS);

        String json = Files.readString(fixture);
        assertTrue(json.contains("\"bankLast4\":\"5678\"")); // only the last 4 digits leave the aggregate
        assertTrue(json.contains("\"amount\":980000"));      // net payout after 2% commission
    }
}
