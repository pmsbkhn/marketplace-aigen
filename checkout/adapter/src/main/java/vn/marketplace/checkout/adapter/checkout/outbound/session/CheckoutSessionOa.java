package vn.marketplace.checkout.adapter.checkout.outbound.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import vn.marketplace.checkout.application.checkout.CheckoutSessionPort;

/**
 * Session + idempotency-lock store. Production is Redis ({@code checkout:{key}} TTL 30 min,
 * {@code checkout:lock:{key}} TTL 10 s); this in-memory implementation is the standalone stand-in
 * with identical semantics — TTL-bound entries, lock via atomic put-if-absent, expired entries
 * treated as missing. Same trade-off as Redis: losing the store only loses idempotency
 * (reservations self-release via their Inventory TTL).
 */
@Component
public class CheckoutSessionOa implements CheckoutSessionPort {

    private final Map<String, Expiring<CheckoutSession>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Expiring<Boolean>> locks = new ConcurrentHashMap<>();
    private final Duration sessionTtl;
    private final Duration lockTtl;
    private final Clock clock;

    @Autowired
    public CheckoutSessionOa(@Value("${checkout.session-ttl:PT30M}") Duration sessionTtl,
                             @Value("${checkout.lock-ttl:PT10S}") Duration lockTtl) {
        this(sessionTtl, lockTtl, Clock.systemUTC());
    }

    public CheckoutSessionOa(Duration sessionTtl, Duration lockTtl, Clock clock) {
        this.sessionTtl = sessionTtl;
        this.lockTtl = lockTtl;
        this.clock = clock;
    }

    @Override
    public Optional<CheckoutSession> find(String idempotencyKey) {
        Expiring<CheckoutSession> entry = sessions.get(idempotencyKey);
        if (entry == null || entry.expired(clock.instant())) {
            sessions.remove(idempotencyKey, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public boolean tryLock(String idempotencyKey) {
        Instant now = clock.instant();
        Expiring<Boolean> fresh = new Expiring<>(Boolean.TRUE, now.plus(lockTtl));
        while (true) {
            Expiring<Boolean> existing = locks.putIfAbsent(idempotencyKey, fresh);
            if (existing == null) {
                return true; // acquired
            }
            if (!existing.expired(now)) {
                return false; // genuinely held by a concurrent request
            }
            if (locks.replace(idempotencyKey, existing, fresh)) {
                return true; // stale lock reclaimed atomically
            }
            // lost the race to reclaim — loop and re-evaluate
        }
    }

    @Override
    public void unlock(String idempotencyKey) {
        locks.remove(idempotencyKey);
    }

    @Override
    public void saveCompleted(String idempotencyKey, String buyerId, List<String> orderIds,
                              String paymentUrl, long grandTotal) {
        put(idempotencyKey, new CheckoutSession(idempotencyKey, buyerId,
                CheckoutSession.STATE_REDIRECTED, orderIds, paymentUrl, grandTotal, null));
    }

    @Override
    public void saveFailed(String idempotencyKey, String buyerId, String reason) {
        put(idempotencyKey, new CheckoutSession(idempotencyKey, buyerId,
                CheckoutSession.STATE_FAILED, List.of(), null, 0, reason));
    }

    private void put(String key, CheckoutSession session) {
        sessions.put(key, new Expiring<>(session, clock.instant().plus(sessionTtl)));
    }

    private record Expiring<T>(T value, Instant expiresAt) {
        boolean expired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }
}
