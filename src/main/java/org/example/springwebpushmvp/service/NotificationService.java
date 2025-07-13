package org.example.springwebpushmvp.service;

import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.jose4j.lang.JoseException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalTime;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private volatile boolean schedulerEnabled = true;
    private int sendCounter = 1;

    private final SubscribersService subscribersService;
    private final PushService pushService;

    public void toggleScheduler(boolean enable) {
        this.schedulerEnabled = enable;
        System.out.println("Scheduler " + (enable ? "STARTED" : "PAUSED"));
    }

    public synchronized int getAndIncrementCounter() {
        return sendCounter++;
    }

    public void sendManualNotification(Subscription subscription, String title, String body) {
        int counter = getAndIncrementCounter();
        String json = String.format("""
                        {
                            "title": "%s #%d",
                            "body": "%s",
                            "icon": "/icon.png",
                            "counter": %d
                        }
                        """,
                title,
                counter,
                body,
                counter
        );
        sendNotification(subscription, json);
    }

    @Scheduled(fixedDelay = 1000)
    void sendNotifications() {
        if (!schedulerEnabled || subscribersService.getSubscriptions().isEmpty()) {
            System.out.println("!schedulerEnabled: " + !schedulerEnabled + ",    subscribersService.getSubscriptions().isEmpty(): " + subscribersService.getSubscriptions().size() + ",    now: " + LocalTime.now());
            return;
        }

        int counter = getAndIncrementCounter();
        String json = String.format("""
                        {
                            "title": "Server Update #%d",
                            "body": "It is now: %tT",
                            "icon": "/icon.png",
                            "counter": %d
                        }
                        """,
                counter,
                LocalTime.now(),
                counter
        );

        subscribersService.getSubscriptions().forEach(sub -> sendNotification(sub, json));
    }

    @Async("notificationTaskExecutor")
    public void sendNotification(Subscription subscription, String messageJson) {
        try {
            System.out.println("Sending notification - Endpoint: " + subscription.endpoint
                    + " | Message: " + messageJson);
            pushService.send(new Notification(subscription, messageJson));
        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException
                 | InterruptedException e) {
            System.out.println("Failed to send notification to " + subscription.endpoint
                    + " | Error: " + e.getMessage());
        }
    }
}
