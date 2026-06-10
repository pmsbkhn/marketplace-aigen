package vn.marketplace.notification.application.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.marketplace.notification.domain.delivery.Channel;
import vn.marketplace.notification.domain.delivery.NotificationId;
import vn.marketplace.notification.domain.delivery.Priority;
import vn.marketplace.notification.domain.delivery.management.Notification;

class DispatchNotificationUcTest {

    private InMemoryNotificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryNotificationRepository();
    }

    private String givenAccepted(Priority priority) {
        Notification n = Notification.accept(new NotificationId("N-1"), "idem-1", "U-1", Channel.EMAIL,
                "promo", Map.of("code", "SALE"), priority, "Marketing", "evt-1");
        repository.save(n);
        return n.id().value();
    }

    @Test
    void bulkOptOutSuppressesAndNeverCallsProvider() { // TC-NOT-02
        String id = givenAccepted(Priority.BULK);
        CountingProvider provider = new CountingProvider();
        DispatchNotificationUc dispatch = new DispatchNotificationUc(repository, userId -> true, provider);

        dispatch.execute(new DispatchNotificationCmd(id));

        assertEquals("SUPPRESSED", repository.store.get(id).status().name());
        assertEquals(0, provider.calls, "provider must NOT be called when BULK + opted out");
    }

    @Test
    void bulkNotOptedOutSendsViaProvider() {
        String id = givenAccepted(Priority.BULK);
        CountingProvider provider = new CountingProvider();
        DispatchNotificationUc dispatch = new DispatchNotificationUc(repository, userId -> false, provider);

        dispatch.execute(new DispatchNotificationCmd(id));

        assertEquals("SENT", repository.store.get(id).status().name());
        assertEquals(1, provider.calls);
    }

    @Test
    void transactionalIgnoresOptOutAndSends() {
        String id = givenAccepted(Priority.TRANSACTIONAL);
        CountingProvider provider = new CountingProvider();
        DispatchNotificationUc dispatch = new DispatchNotificationUc(repository, userId -> true, provider);

        dispatch.execute(new DispatchNotificationCmd(id));

        assertEquals("SENT", repository.store.get(id).status().name());
        assertEquals(1, provider.calls);
    }

    private static final class CountingProvider implements NotificationProviderPort {
        int calls;

        @Override
        public String send(Channel channel, String userId, String templateId, Map<String, String> variables) {
            calls++;
            return "SES";
        }
    }
}
