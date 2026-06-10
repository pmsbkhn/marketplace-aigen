package vn.marketplace.payment.adapter.payment.outbound.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TC-PAY-INT-03 — WORM immutability of settlement documents: after a successful upload, a second
 * upload to the SAME key with different content must be denied (AccessDenied — prevent
 * overwrite/override) and the original object must remain byte-identical. An identical re-upload
 * (idempotent retry) returns the same URI without touching the object.
 */
class SettlementDocWriterOaTest {

    private static final String KEY = "settlements/O-1.json";
    private static final String ORIGINAL = "{\"orderId\":\"O-1\",\"net\":980000}";

    @TempDir
    Path wormRoot;

    private SettlementDocWriterOa writer;

    @BeforeEach
    void setUp() {
        writer = new SettlementDocWriterOa(wormRoot.toString());
    }

    @Test
    void overwriteAttemptIsAccessDeniedAndOriginalIsIntact() throws Exception {
        String uri = writer.writeOnce(KEY, ORIGINAL);
        assertEquals("s3://settlement-docs/" + KEY, uri);

        WormAccessDeniedException ex = assertThrows(WormAccessDeniedException.class,
                () -> writer.writeOnce(KEY, "{\"orderId\":\"O-1\",\"net\":1}")); // tampered content

        assertTrue(ex.getMessage().contains("AccessDenied"));
        assertEquals(ORIGINAL, Files.readString(wormRoot.resolve(KEY)), "original object untouched");
    }

    @Test
    void identicalRetryReturnsSameUriWithoutRewriting() {
        String first = writer.writeOnce(KEY, ORIGINAL);
        String second = writer.writeOnce(KEY, ORIGINAL); // idempotent retry after a partial failure

        assertEquals(first, second);
    }

    @Test
    void pathTraversalKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> writer.writeOnce("../outside.json", "x"));
    }
}
