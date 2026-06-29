package vn.marketplace.checkout.application.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.vsf.ptnt.msfw.domain.core.IdempotencyKey;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd.CheckoutItemInput;
import vn.marketplace.checkout.application.checkout.SubmitCheckoutCmd.ShippingAddressInput;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

class SubmitCheckoutUcTest {

    private static final ShippingAddressInput ADDRESS =
            new ShippingAddressInput("Nguyen Van A", "0900000000", "1 Le Loi", "Ben Nghe", "Q1", "HCMC");

    private CheckoutTestFakes fakes;
    private SubmitCheckoutUc uc;

    @BeforeEach
    void setUp() {
        fakes = new CheckoutTestFakes();
        uc = new SubmitCheckoutUc(fakes.catalog, fakes.inventory, fakes.order, fakes.payment,
                fakes.sessions, 50, 10);
        fakes.catalog.price("SKU-A", 100, "M-1");
        fakes.catalog.price("SKU-B", 50, "M-2");
    }

    private SubmitCheckoutCmd cmd(CheckoutItemInput... items) {
        return new SubmitCheckoutCmd(new IdempotencyKey("IK-1"), "B-1", ADDRESS, List.of(items));
    }

    private CheckoutItemInput item(String sku, int qty, String merchantId) {
        return new CheckoutItemInput(sku, qty, merchantId);
    }

    @Test
    void happyPath_splitsPerMerchant_oneEscrowForWholeCart() {
        CheckoutResultView view = uc.execute(cmd(item("SKU-A", 2, "M-1"), item("SKU-B", 1, "M-2")));

        assertEquals(List.of("O-1", "O-2"), view.orderIds(), "one order per merchant");
        assertEquals(250, view.grandTotalAmount());
        assertEquals("https://pg.example.com/pay/IK-1", view.paymentUrl());

        assertEquals(250, fakes.payment.lastTotal, "ONE escrow covers the whole cart");
        assertEquals(2, fakes.payment.lastAllocations.size());
        assertEquals(200, fakes.payment.lastAllocations.get(0).amount());
        assertEquals(50, fakes.payment.lastAllocations.get(1).amount());

        assertEquals("IK-1:M-1", fakes.order.created.get(0).checkoutRef(),
                "per-order idempotency key = key:merchantId");
        assertTrue(fakes.sessions.find("IK-1").orElseThrow().isRedirected());
    }

    @Test
    void escrowFailureCompensatesInStrictReverseOrder() { // TC-CHK-02
        fakes.payment.toThrow = new IllegalStateException("escrow 500");

        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute(cmd(item("SKU-A", 1, "M-1"), item("SKU-B", 1, "M-2"))));
        assertEquals(CheckoutErrorCode.CHECKOUT_FAILED, ex.checkoutErrorCode());

        // Orders cancelled FIRST (newest first), reservation released LAST.
        List<String> log = fakes.callLog;
        int cancel2 = log.indexOf("cancelOrder:O-2");
        int cancel1 = log.indexOf("cancelOrder:O-1");
        int release = log.indexOf("releaseStock:RES-IK-1");
        assertTrue(cancel2 >= 0 && cancel1 >= 0 && release >= 0, "all compensation steps ran: " + log);
        assertTrue(cancel2 < cancel1, "newest order cancelled first: " + log);
        assertTrue(cancel1 < release, "cancelOrder BEFORE releaseStock: " + log);

        assertEquals(CheckoutSessionPort.CheckoutSession.STATE_FAILED,
                fakes.sessions.find("IK-1").orElseThrow().state(), "failure cached");
    }

    @Test
    void priceAuthority_catalogPriceWinsOverAnyClientClaim() { // TC-CHK-03
        fakes.catalog.price("SKU-A", 100, "M-1"); // catalog says 100 — the cmd cannot even carry 50

        uc.execute(cmd(item("SKU-A", 1, "M-1")));

        assertEquals(100, fakes.order.created.get(0).items().get(0).priceSnapshot(),
                "order receives the Catalog price, never a client figure");
        assertEquals(100, fakes.payment.lastTotal);
    }

    @Test
    void duplicateKeyIsIdempotent_noSecondSaga() { // TC-CHK-04 (cached-result fast-path)
        CheckoutResultView first = uc.execute(cmd(item("SKU-A", 1, "M-1")));
        fakes.callLog.clear();

        CheckoutResultView second = uc.execute(cmd(item("SKU-A", 1, "M-1")));

        assertEquals(first.paymentUrl(), second.paymentUrl());
        assertEquals(first.orderIds(), second.orderIds());
        assertTrue(fakes.callLog.isEmpty(), "no port is called on the cached path: " + fakes.callLog);
    }

    @Test
    void heldLockRejectsConcurrentDuplicate() { // TC-CHK-04 (lock branch)
        fakes.sessions.lockDenied = true;

        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute(cmd(item("SKU-A", 1, "M-1"))));

        assertEquals(CheckoutErrorCode.CHECKOUT_IN_PROGRESS, ex.checkoutErrorCode());
        assertTrue(fakes.callLog.isEmpty(), "no port is called while the lock is held");
    }

    @Test
    void outOfStockFailsFast_nothingCreated_nothingReleased_notCached() {
        fakes.inventory.allReserved = false;

        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute(cmd(item("SKU-A", 1, "M-1"))));

        assertEquals(CheckoutErrorCode.OUT_OF_STOCK, ex.checkoutErrorCode());
        assertTrue(fakes.order.created.isEmpty(), "no order on shortage");
        assertTrue(fakes.inventory.released.isEmpty(), "all-or-nothing reserve held nothing");
        assertTrue(fakes.sessions.find("IK-1").isEmpty(), "restock retry with the same key stays possible");
    }

    @Test
    void unknownOrInactiveSkuIsVetoedBeforeAnySideEffect() {
        fakes.catalog.inactive("SKU-X", "M-1");

        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute(cmd(item("SKU-X", 1, "M-1"))));

        assertEquals(CheckoutErrorCode.SKU_UNAVAILABLE, ex.checkoutErrorCode());
        assertTrue(fakes.callLog.contains("fetchPrices") && !fakes.callLog.contains("reserveStock"));
    }

    @Test
    void merchantClaimMismatchingCatalogIsVetoed() {
        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute(cmd(item("SKU-A", 1, "M-999")))); // catalog says SKU-A is M-1's

        assertEquals(CheckoutErrorCode.SKU_UNAVAILABLE, ex.checkoutErrorCode());
    }

    @Test
    void duplicateSkuLinesAreAggregatedForTheReserve() {
        uc.execute(cmd(item("SKU-A", 2, "M-1"), item("SKU-A", 3, "M-1")));

        assertEquals(1, fakes.inventory.lastItems.size(), "one reserve line per SKU");
        assertEquals(5, fakes.inventory.lastItems.get(0).quantity());
    }

    @Test
    void emptyCartIsRejected() {
        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> uc.execute(new SubmitCheckoutCmd(new IdempotencyKey("IK-1"), "B-1", ADDRESS, List.of())));
        assertEquals(CheckoutErrorCode.EMPTY_CART, ex.checkoutErrorCode());
    }
}
