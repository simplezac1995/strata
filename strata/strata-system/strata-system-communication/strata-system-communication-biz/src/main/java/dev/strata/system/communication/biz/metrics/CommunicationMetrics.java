// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for the Communication subdomain.
 *
 * <p>Tracks email, SMS, announcement, and notification delivery events.
 */
@Component
public class CommunicationMetrics {

    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Counter smsSentCounter;
    private final Counter smsFailedCounter;
    private final Counter announcementCreatedCounter;
    private final Counter announcementPublishedCounter;
    private final Counter notificationSentCounter;
    private final Counter notificationReadCounter;

    public CommunicationMetrics(MeterRegistry registry) {
        this.emailSentCounter = Counter.builder("strata.system.communication.email.sent")
                .description("Emails sent successfully")
                .register(registry);
        this.emailFailedCounter = Counter.builder("strata.system.communication.email.failed")
                .description("Emails failed to send")
                .register(registry);
        this.smsSentCounter = Counter.builder("strata.system.communication.sms.sent")
                .description("SMS messages sent successfully")
                .register(registry);
        this.smsFailedCounter = Counter.builder("strata.system.communication.sms.failed")
                .description("SMS messages failed to send")
                .register(registry);
        this.announcementCreatedCounter = Counter.builder("strata.system.communication.announcement.created")
                .description("Announcements created")
                .register(registry);
        this.announcementPublishedCounter = Counter.builder("strata.system.communication.announcement.published")
                .description("Announcements published")
                .register(registry);
        this.notificationSentCounter = Counter.builder("strata.system.communication.notification.sent")
                .description("Notifications sent")
                .register(registry);
        this.notificationReadCounter = Counter.builder("strata.system.communication.notification.read")
                .description("Notifications read by users")
                .register(registry);
    }

    public void recordEmailSent() {
        emailSentCounter.increment();
    }

    public void recordEmailFailed() {
        emailFailedCounter.increment();
    }

    public void recordSmsSent() {
        smsSentCounter.increment();
    }

    public void recordSmsFailed() {
        smsFailedCounter.increment();
    }

    public void recordAnnouncementCreated() {
        announcementCreatedCounter.increment();
    }

    public void recordAnnouncementPublished() {
        announcementPublishedCounter.increment();
    }

    public void recordNotificationSent() {
        notificationSentCounter.increment();
    }

    public void recordNotificationRead() {
        notificationReadCounter.increment();
    }
}
