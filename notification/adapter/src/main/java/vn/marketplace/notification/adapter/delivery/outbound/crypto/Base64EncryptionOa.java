package vn.marketplace.notification.adapter.delivery.outbound.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import vn.marketplace.notification.application.delivery.EncryptionPort;

/**
 * Field-level encryption adapter. <strong>Stand-in</strong>: a reversible Base64 transform with an
 * {@code ENC:} prefix to demonstrate ciphertext-at-rest on the standalone profile. Production replaces
 * this with AES-256 + envelope encryption via KMS-managed rotating keys (L4).
 */
@Component
public class Base64EncryptionOa implements EncryptionPort {

    private static final String PREFIX = "ENC:";

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return PREFIX + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            return ciphertext;
        }
        byte[] decoded = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
