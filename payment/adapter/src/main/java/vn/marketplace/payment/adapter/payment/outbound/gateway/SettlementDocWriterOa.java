package vn.marketplace.payment.adapter.payment.outbound.gateway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import vn.marketplace.payment.application.payment.SettlementDocWriter;

/**
 * Outbound adapter to the WORM settlement-document store. Production: S3 result bucket with Object
 * Lock — write-once, prevent overwrite/override even for the owner. Standalone: a local directory
 * with the same semantics, enforced here:
 * <ul>
 *   <li>same key + identical content → returns the existing URI (idempotent retry);</li>
 *   <li>same key + different content → {@link WormAccessDeniedException} (TC-PAY-INT-03);</li>
 *   <li>{@code CREATE_NEW} write — never truncates an existing object.</li>
 * </ul>
 */
@Component
public class SettlementDocWriterOa implements SettlementDocWriter {

    /** Mirrors the S3 result-bucket URI shape so downstream consumers see the production format. */
    private static final String URI_PREFIX = "s3://settlement-docs/";

    private final Path root;

    public SettlementDocWriterOa(
            @Value("${payment.settlement-doc.dir:${java.io.tmpdir}/payment-settlement-docs}") String dir) {
        this.root = Path.of(dir);
    }

    @Override
    public String writeOnce(String documentKey, String content) {
        Path target = resolveWithinRoot(documentKey);
        try {
            if (Files.exists(target)) {
                String existing = Files.readString(target, StandardCharsets.UTF_8);
                if (existing.equals(content)) {
                    return URI_PREFIX + documentKey; // identical retry — same immutable object
                }
                throw new WormAccessDeniedException(documentKey);
            }
            Files.createDirectories(target.getParent());
            Files.write(target, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            return URI_PREFIX + documentKey;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot persist settlement document " + documentKey, e);
        }
    }

    private Path resolveWithinRoot(String documentKey) {
        Path target = root.resolve(documentKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Document key escapes the WORM root: " + documentKey);
        }
        return target;
    }
}
