package vn.marketplace.payment.application.payment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hand-written fakes for the outbound ports (gateway / bank / WORM doc store). */
final class PaymentTestFakes {

    private PaymentTestFakes() {
    }

    static final class FakeGateway implements PaymentGatewayPort {
        final List<String> createdFor = new ArrayList<>();

        @Override
        public String createTransaction(String paymentId, String orderRef, long amount, String currency) {
            createdFor.add(paymentId);
            return "https://pg.example.com/pay/" + paymentId;
        }
    }

    static final class FakeBank implements BankPort {
        record Instruction(String payoutId, String merchantId, String bankAccount, long amount) {
        }

        final List<Instruction> instructions = new ArrayList<>();
        RuntimeException toThrow;

        @Override
        public void submitPayout(String payoutId, String merchantId, String bankAccount, long amount,
                                 String currency) {
            if (toThrow != null) {
                throw toThrow;
            }
            instructions.add(new Instruction(payoutId, merchantId, bankAccount, amount));
        }
    }

    /** Write-once store: same key + same content → same URI; different content → denied. */
    static final class FakeWormDocStore implements SettlementDocWriter {
        final Map<String, String> objects = new LinkedHashMap<>();
        int writes = 0;

        @Override
        public String writeOnce(String documentKey, String content) {
            writes++;
            String existing = objects.get(documentKey);
            if (existing != null && !existing.equals(content)) {
                throw new IllegalStateException("AccessDenied: WORM object already exists: " + documentKey);
            }
            objects.put(documentKey, content);
            return "s3://settlement-docs/" + documentKey;
        }
    }
}
