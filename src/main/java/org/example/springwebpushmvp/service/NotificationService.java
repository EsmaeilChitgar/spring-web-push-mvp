package org.example.springwebpushmvp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NotificationService {
    private volatile boolean schedulerEnabled = true;
    private int sendCounter = 1;

    private final SubscribersService subscribersService;
    private final PushService pushService;

    public void toggleScheduler(boolean enable) {
        this.schedulerEnabled = enable;
        log.info("Scheduler {}", enable ? "STARTED" : "PAUSED");
    }

    public synchronized int getAndIncrementCounter() {
        return sendCounter++;
    }

    public void sendManualNotification(Subscription subscription, String title, String body) {
        int counter = getAndIncrementCounter();
        sendNotification(subscription, json(title, body, counter));
    }

    @Scheduled(fixedDelay = 1000)
    void sendNotifications() {
        if (!schedulerEnabled || subscribersService.getSubscriptions().isEmpty()) {
            log.warn("!schedulerEnabled: {},    subscribersService.getSubscriptions().isEmpty(): {},    now: {}", !schedulerEnabled, subscribersService.getSubscriptions().size(), LocalTime.now());
            return;
        }

        int counter = getAndIncrementCounter();
        String title = "Server Update";
        String body = String.format("It is now: %tT",LocalTime.now());
        subscribersService.getSubscriptions().forEach(sub -> sendNotification(sub, json(title, body, counter)));
    }

    @Async("notificationTaskExecutor")
    public void sendNotification(Subscription subscription, String messageJson) {
        try {
            log.info("Sending notification - Message: {} | Endpoint: {}", messageJson, subscription.endpoint);
            pushService.send(new Notification(subscription, messageJson));
        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException
                 | InterruptedException e) {
            log.warn("Failed to send notification to {} | Error: {}", subscription.endpoint, e.getMessage());
        }
    }

    private String json(String title, String body, int counter) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("title", title + " #" + counter);
        jsonNode.put("body", body);
        jsonNode.put("icon", "/icon.png");
        jsonNode.put("counter", counter);
        return jsonNode.toString();
    }
}
