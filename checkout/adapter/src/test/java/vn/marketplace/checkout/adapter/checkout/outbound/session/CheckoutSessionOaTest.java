package vn.marketplace.checkout.adapter.checkout.outbound.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import vn.marketplace.checkout.application.checkout.CheckoutSessionPort.CheckoutSession;

/**
 * Redis-semantics contract of the in-memory session store: TC-CHK-04's lock behaviour (second
 * acquire on a live key fails; a stale lock is reclaimable) and the TTL-bound idempotency cache.
 */
class CheckoutSessionOaTest {

    /** Mutable clock so the test can travel through TTL windows deterministically. */
    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-06-10T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private final SteppingClock clock = new SteppingClock();
    private final CheckoutSessionOa store =
            new CheckoutSessionOa(Duration.ofMinutes(30), Duration.ofSeconds(10), clock);

    @Test
    void secondLockOnLiveKeyIsDenied() { // TC-CHK-04 lock semantics
        assertTrue(store.tryLock("IK-1"), "first acquire wins");
        assertFalse(store.tryLock("IK-1"), "concurrent duplicate is denied");

        store.unlock("IK-1");
        assertTrue(store.tryLock("IK-1"), "free key is acquirable again");
    }

    @Test
    void staleLockIsReclaimedAfterTtl() {
        assertTrue(store.tryLock("IK-1"));
        clock.advance(Duration.ofSeconds(11)); // crashed holder — lock older than its 10s TTL

        assertTrue(store.tryLock("IK-1"), "expired lock must not block forever");
    }

    @Test
    void completedSessionIsCachedUntilTtl() {
        store.saveCompleted("IK-1", "B-1", List.of("O-1"), "url", 250);

        CheckoutSession session = store.find("IK-1").orElseThrow();
        assertTrue(session.isRedirected());
        assertEquals(List.of("O-1"), session.orderIds());

        clock.advance(Duration.ofMinutes(31));
        assertTrue(store.find("IK-1").isEmpty(), "session expires after its 30-min TTL");
    }

    @Test
    void failedSessionCachesTheReason() {
        store.saveFailed("IK-1", "B-1", "escrow 500");

        CheckoutSession session = store.find("IK-1").orElseThrow();
        assertEquals(CheckoutSession.STATE_FAILED, session.state());
        assertEquals("escrow 500", session.failReason());
    }
}
