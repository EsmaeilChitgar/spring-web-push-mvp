package org.example.pushnotifmvp;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.LocalTime;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {
    private final SubscribersService subscribersService;

    private volatile boolean schedulerEnabled = true;
    private int sendCounter = 1;

    @Value("${vapid.public.key}")
    private String publicKey;
    @Value("${vapid.private.key}")
    private String privateKey;

    private PushService pushService;


    @PostConstruct
    private void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        pushService = new PushService(publicKey, privateKey);
    }

    public NotificationService(SubscribersService subscribersService) {
        this.subscribersService = subscribersService;
    }

    public void toggleScheduler(boolean enable) {
        this.schedulerEnabled = enable;
        System.out.println("Scheduler " + (enable ? "STARTED" : "PAUSED"));
    }

    public synchronized int getAndIncrementCounter() {
        return sendCounter++;
    }

    public void sendManualNotification(Subscription subscription, String title, String body) {
        String json = String.format("""
                        {
                            "title": "%s #%d",
                            "body": "%s",
                            "icon": "/icon.png",
                            "counter": %d
                        }
                        """,
                title,
                getAndIncrementCounter(),
                body,
                sendCounter
        );
        sendNotification(subscription, json);
    }

    @Scheduled(fixedRate = 400)
    private void sendNotifications() {
        if (!schedulerEnabled || subscribersService.getSubscriptions().isEmpty()) {
            return;
        }

        String json = String.format("""
                        {
                            "title": "Server Update #%d",
                            "body": "It is now: %tT",
                            "icon": "/icon.png",
                            "counter": %d
                        }
                        """,
                getAndIncrementCounter(),
                LocalTime.now(),
                sendCounter
        );

        subscribersService.getSubscriptions().forEach(sub ->
                sendNotification(sub, json));
    }

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
