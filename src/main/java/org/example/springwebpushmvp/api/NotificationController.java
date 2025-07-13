package org.example.springwebpushmvp.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Subscription;
import org.example.springwebpushmvp.service.NotificationService;
import org.example.springwebpushmvp.service.SubscribersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;
    private final SubscribersService subscribersService;

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody Subscription subscription) {
        subscribersService.subscribe(subscription);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody Subscription subscription) {
        subscribersService.unsubscribe(subscription);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-custom")
    public ResponseEntity<String> sendCustomNotification(@RequestParam(required = false) String title, @RequestParam(required = false) String body) {
        title = Optional.ofNullable(title).orElse("📬 New Message");
        body = Optional.ofNullable(body).orElse("You have a new notification!");

        for (Subscription sub : subscribersService.getSubscriptions()) {
            try {
                notificationService.sendManualNotification(sub, title, body);
            } catch (Exception e) {
                log.warn("/send error: {}", e.getMessage());
                return ResponseEntity.status(500).body("Failed to send to some subscribers");
            }
        }
        return ResponseEntity.ok("Sent to all subscribers!");
    }

    @PostMapping("/scheduler")
    public ResponseEntity<String> controlScheduler(@RequestParam boolean enable) {
        notificationService.toggleScheduler(enable);
        return ResponseEntity.ok("Scheduler " + (enable ? "started" : "paused"));
    }
}
