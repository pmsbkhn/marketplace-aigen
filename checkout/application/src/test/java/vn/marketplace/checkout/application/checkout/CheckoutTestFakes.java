package vn.marketplace.checkout.application.checkout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hand-written fakes for the saga ports. All fakes append to one shared {@code callLog} so tests
 * can assert the GLOBAL cross-port ordering — the heart of the reverse-order compensation rule
 * (TC-CHK-02: {@code cancelOrder} entries must precede {@code releaseStock}).
 */
final class CheckoutTestFakes {

    final List<String> callLog = new ArrayList<>();

    final FakeCatalog catalog = new FakeCatalog();
    final FakeInventory inventory = new FakeInventory();
    final FakeOrder order = new FakeOrder();
    final FakePayment payment = new FakePayment();
    final FakeSessions sessions = new FakeSessions();

    final class FakeCatalog implements CatalogPort {
        final Map<String, SkuPriceDto> prices = new LinkedHashMap<>();

        void price(String sku, long amount, String merchantId) {
            prices.put(sku, new SkuPriceDto(sku, amount, "VND", merchantId, true));
        }

        void inactive(String sku, String merchantId) {
            prices.put(sku, new SkuPriceDto(sku, 1, "VND", merchantId, false));
        }

        @Override
        public List<SkuPriceDto> fetchPrices(List<String> skuCodes) {
            callLog.add("fetchPrices");
            return skuCodes.stream().map(prices::get).filter(java.util.Objects::nonNull).toList();
        }
    }

    final class FakeInventory implements InventoryPort {
        boolean allReserved = true;
        String lastOrderRef;
        List<ReserveItemDto> lastItems;
        final List<String> released = new ArrayList<>();

        @Override
        public ReservationDto reserveStock(String orderRef, List<ReserveItemDto> items) {
            callLog.add("reserveStock");
            this.lastOrderRef = orderRef;
            this.lastItems = items;
            return new ReservationDto("RES-" + orderRef, allReserved,
                    allReserved ? List.of() : List.of(new ShortageDto(items.get(0).sku(), 0)));
        }

        @Override
        public void releaseStock(String reservationId) {
            callLog.add("releaseStock:" + reservationId);
            released.add(reservationId);
        }
    }

    final class FakeOrder implements OrderPort {
        final AtomicInteger seq = new AtomicInteger(0);
        final List<CreateOrderDto> created = new ArrayList<>();
        final List<String> cancelled = new ArrayList<>();
        RuntimeException createThrows;

        @Override
        public String createPendingOrder(CreateOrderDto dto) {
            if (createThrows != null) {
                throw createThrows;
            }
            created.add(dto);
            String orderId = "O-" + seq.incrementAndGet();
            callLog.add("createOrder:" + orderId);
            return orderId;
        }

        @Override
        public void cancelOrder(String orderId, String reason) {
            callLog.add("cancelOrder:" + orderId);
            cancelled.add(orderId);
        }
    }

    final class FakePayment implements PaymentPort {
        RuntimeException toThrow;
        long lastTotal;
        List<EscrowAllocationDto> lastAllocations;

        @Override
        public EscrowDto initEscrow(String orderRef, long totalAmount, String currency,
                                    List<EscrowAllocationDto> allocations, String buyerId) {
            callLog.add("initEscrow");
            if (toThrow != null) {
                throw toThrow;
            }
            this.lastTotal = totalAmount;
            this.lastAllocations = allocations;
            return new EscrowDto("PAY-" + orderRef, "https://pg.example.com/pay/" + orderRef);
        }
    }

    final class FakeSessions implements CheckoutSessionPort {
        final Map<String, CheckoutSession> store = new LinkedHashMap<>();
        final Map<String, Boolean> locks = new LinkedHashMap<>();
        boolean lockDenied;

        @Override
        public Optional<CheckoutSession> find(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public boolean tryLock(String key) {
            if (lockDenied) {
                return false;
            }
            return locks.putIfAbsent(key, Boolean.TRUE) == null;
        }

        @Override
        public void unlock(String key) {
            locks.remove(key);
        }

        @Override
        public void saveCompleted(String key, String buyerId, List<String> orderIds,
                                  String paymentUrl, long grandTotal) {
            store.put(key, new CheckoutSession(key, buyerId, CheckoutSession.STATE_REDIRECTED,
                    orderIds, paymentUrl, grandTotal, null));
        }

        @Override
        public void saveFailed(String key, String buyerId, String reason) {
            store.put(key, new CheckoutSession(key, buyerId, CheckoutSession.STATE_FAILED,
                    List.of(), null, 0, reason));
        }
    }
}
