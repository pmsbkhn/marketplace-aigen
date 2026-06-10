package vn.marketplace.checkout.domain.checkout.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import vn.marketplace.checkout.domain.checkout.Currency;
import vn.marketplace.checkout.domain.checkout.Money;
import vn.marketplace.checkout.domain.shared.CheckoutDomainException;
import vn.marketplace.checkout.domain.shared.CheckoutErrorCode;

class OrderSplitterTest {

    private LineItem line(String sku, String merchantId, long price, int qty) {
        return new LineItem(sku, qty, merchantId, Money.of(price, Currency.VND));
    }

    @Test
    void splitsCartIntoOneGroupPerMerchantPreservingOrder() {
        CartSnapshot cart = new CartSnapshot(List.of(
                line("SKU-1", "M-1", 100, 2),
                line("SKU-2", "M-2", 50, 1),
                line("SKU-3", "M-1", 30, 3)));

        List<MerchantGroup> groups = new OrderSplitter().split(cart);

        assertEquals(2, groups.size());
        assertEquals("M-1", groups.get(0).merchantId());
        assertEquals(List.of("SKU-1", "SKU-3"),
                groups.get(0).items().stream().map(LineItem::sku).toList());
        assertEquals(290, groups.get(0).subtotal().amount()); // 2×100 + 3×30
        assertEquals("M-2", groups.get(1).merchantId());
        assertEquals(50, groups.get(1).subtotal().amount());
    }

    @Test
    void groupSubtotalsSumToCartGrandTotal() {
        CartSnapshot cart = new CartSnapshot(List.of(
                line("SKU-1", "M-1", 100, 2),
                line("SKU-2", "M-2", 50, 1)));

        long sumOfGroups = new OrderSplitter().split(cart).stream()
                .mapToLong(g -> g.subtotal().amount())
                .sum();

        assertEquals(cart.grandTotal().amount(), sumOfGroups);
        assertEquals(250, sumOfGroups);
    }

    @Test
    void emptyCartIsRejected() {
        CheckoutDomainException ex = assertThrows(CheckoutDomainException.class,
                () -> new CartSnapshot(List.of()));
        assertEquals(CheckoutErrorCode.EMPTY_CART, ex.checkoutErrorCode());
    }

    @Test
    void clientCannotSmuggleNonPositivePriceOrQuantity() {
        CheckoutDomainException badQty = assertThrows(CheckoutDomainException.class,
                () -> line("SKU-1", "M-1", 100, 0));
        assertEquals(CheckoutErrorCode.INVALID_QUANTITY, badQty.checkoutErrorCode());

        CheckoutDomainException badPrice = assertThrows(CheckoutDomainException.class,
                () -> line("SKU-1", "M-1", 0, 1));
        assertEquals(CheckoutErrorCode.INVALID_PRICE, badPrice.checkoutErrorCode());
    }
}
