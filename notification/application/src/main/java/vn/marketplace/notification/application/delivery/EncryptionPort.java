package vn.marketplace.notification.application.delivery;

/**
 * Outbound port for field-level encryption of sensitive template variables (OTP, etc.) — L4. The
 * adapter implements it with AES-256/KMS in production; the application ciphers values BEFORE the
 * aggregate is built/saved so plaintext never reaches persistence.
 */
public interface EncryptionPort {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
