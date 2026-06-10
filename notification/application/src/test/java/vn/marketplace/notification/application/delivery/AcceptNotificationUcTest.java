package vn.marketplace.notification.application.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.marketplace.notification.domain.delivery.management.Notification;

class AcceptNotificationUcTest {

    /** Reversible test cipher: ciphertext is clearly not the plaintext and carries a prefix. */
    private static final EncryptionPort CIPHER = new EncryptionPort() {
        @Override public String encrypt(String plaintext) {
            return "ENC:" + new StringBuilder(plaintext).reverse();
        }
        @Override public String decrypt(String ciphertext) {
            return new StringBuilder(ciphertext.substring(4)).reverse().toString();
        }
    };

    private InMemoryNotificationRepository repository;
    private AcceptNotificationUc acceptUc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
        acceptUc = new AcceptNotificationUc(repository, CIPHER);
    }

    @Test
    void sensitiveVariablesAreCiphertextWhenSaved() { // TC-NOT-03: OTP not plaintext at persistence
        acceptUc.execute(new AcceptNotificationCmd("idem-1", "U-1", "SMS", "otp-verify",
                Map.of("otp", "123456"), "TRANSACTIONAL", "OtpRequested", "evt-1"));

        Notification saved = repository.saved.get(0);
        String storedOtp = saved.variables().get("otp");
        assertFalse(storedOtp.contains("123456"), "plaintext OTP must not be stored");
        assertTrue(storedOtp.startsWith("ENC:"), "OTP must be stored as ciphertext");
        assertEquals("123456", CIPHER.decrypt(storedOtp)); // still recoverable for rendering
    }

    @Test
    void acceptIsIdempotentByKey() {
        var first = acceptUc.execute(new AcceptNotificationCmd("idem-1", "U-1", "EMAIL", "order-completed",
                Map.of("orderId", "O-1"), "TRANSACTIONAL", "OrderCompleted", "evt-1"));
        var second = acceptUc.execute(new AcceptNotificationCmd("idem-1", "U-1", "EMAIL", "order-completed",
                Map.of("orderId", "O-1"), "TRANSACTIONAL", "OrderCompleted", "evt-1"));

        assertEquals(first.notificationId(), second.notificationId());
        assertEquals(1, repository.store.size());
    }
}
