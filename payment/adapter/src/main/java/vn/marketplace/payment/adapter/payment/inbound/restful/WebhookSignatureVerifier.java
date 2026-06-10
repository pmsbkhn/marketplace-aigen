package vn.marketplace.payment.adapter.payment.inbound.restful;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifies inbound payment-gateway webhooks (trust boundary B4 of the SAD):
 * <ul>
 *   <li><b>Authenticity</b> — {@code X-Signature} must be the hex HMAC-SHA256 of the raw request
 *       body with the shared gateway secret (constant-time compare);</li>
 *   <li><b>Anti-replay</b> — the {@code timestamp} signed inside the payload must be within
 *       {@code payment.webhook.max-age} of now (default 5 minutes), with a small clock-skew
 *       tolerance for timestamps slightly in the future.</li>
 * </ul>
 * The secret comes from configuration (Vault in production); the clock is injectable for tests.
 */
@Component
public class WebhookSignatureVerifier {

    private static final Duration FUTURE_SKEW_TOLERANCE = Duration.ofSeconds(30);

    private final byte[] secret;
    private final Duration maxAge;
    private final Clock clock;

    @Autowired
    public WebhookSignatureVerifier(
            @Value("${payment.webhook.secret:standalone-only-webhook-secret}") String secret,
            @Value("${payment.webhook.max-age:PT5M}") Duration maxAge) {
        this(secret, maxAge, Clock.systemUTC());
    }

    public WebhookSignatureVerifier(String secret, Duration maxAge, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maxAge = maxAge;
        this.clock = clock;
    }

    /** True iff {@code signatureHex} is the HMAC-SHA256 of {@code rawBody} under the shared secret. */
    public boolean validSignature(String rawBody, String signatureHex) {
        if (rawBody == null || signatureHex == null || signatureHex.isBlank()) {
            return false;
        }
        byte[] expected = hmacHex(rawBody).getBytes(StandardCharsets.UTF_8);
        byte[] provided = signatureHex.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided); // constant-time
    }

    /** True iff the signed payload timestamp (epoch seconds) is inside the anti-replay window. */
    public boolean fresh(long epochSeconds) {
        Instant timestamp = Instant.ofEpochSecond(epochSeconds);
        Instant now = clock.instant();
        return !timestamp.isBefore(now.minus(maxAge)) && !timestamp.isAfter(now.plus(FUTURE_SKEW_TOLERANCE));
    }

    /** Hex HMAC-SHA256 of the body — exposed so tests can sign their fixtures the same way. */
    public String hmacHex(String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}
