package vn.marketplace.notification.adapter.delivery.outbound.preference;

import org.springframework.stereotype.Component;

import vn.marketplace.notification.application.delivery.PreferencePort;

/**
 * Preferences-service client. <strong>Stand-in</strong>: returns "not opted out" for everyone on the
 * standalone profile. Production calls the Preferences service (mTLS), caches with a TTL, and applies
 * fail-open for TRANSACTIONAL / fail-closed for BULK.
 */
@Component
public class PreferencesClientOa implements PreferencePort {

    @Override
    public boolean isOptedOut(String userId) {
        return false;
    }
}
