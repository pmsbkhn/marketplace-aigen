package vn.marketplace.payment.adapter.payment.outbound.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Field-level encryption for the merchant bank account (L4 data) — the column stores
 * {@code enc:v1:<base64(iv ‖ ciphertext)>}, never plaintext (TC-PAY-INT-02). AES-256-GCM with a
 * random 12-byte IV per value.
 *
 * <p><b>Key management:</b> the key is derived from {@code PAYMENT_CRYPTO_KEY} (env) — in production
 * this comes from KMS/Vault with rotation + cryptographic-erasure support; the standalone default
 * exists only so the no-infra profile boots. The key never appears in logs or persisted data.
 */
@Converter
public class BankAccountCryptoConverter implements AttributeConverter<String, String> {

    static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final SecretKeySpec KEY = deriveKey(
            System.getenv().getOrDefault("PAYMENT_CRYPTO_KEY", "standalone-only-payment-crypto-key"));

    private static SecretKeySpec deriveKey(String passphrase) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot derive payment crypto key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Bank account encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        if (!dbValue.startsWith(PREFIX)) {
            throw new IllegalStateException("Bank account column is not encrypted (missing prefix)");
        }
        try {
            byte[] in = Base64.getDecoder().decode(dbValue.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, in, 0, IV_LENGTH));
            byte[] plaintext = cipher.doFinal(in, IV_LENGTH, in.length - IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Bank account decryption failed", e);
        }
    }
}
