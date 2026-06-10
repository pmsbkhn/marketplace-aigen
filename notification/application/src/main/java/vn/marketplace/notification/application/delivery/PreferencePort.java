package vn.marketplace.notification.application.delivery;

/**
 * Outbound port to the Preferences service. {@code isOptedOut} drives BULK suppression (fail-closed):
 * a BULK notification to an opted-out user must be suppressed and never sent.
 */
public interface PreferencePort {
    boolean isOptedOut(String userId);
}
