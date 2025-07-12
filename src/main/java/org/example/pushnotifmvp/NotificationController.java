package org.example.pushnotifmvp;

import nl.martijndwars.webpush.Subscription;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SubscribersService subscribersService;

    public NotificationController(NotificationService notificationService, SubscribersService subscribersService) {
        this.notificationService = notificationService;
        this.subscribersService = subscribersService;
    }

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
    public ResponseEntity<String> sendNotification(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String body) {

        String notificationTitle = Optional.ofNullable(title)
                .orElse("📬 New Message");

        String notificationBody = Optional.ofNullable(body)
                .orElse("You have a new notification!");

        String json = String.format("""
                        {
                            "title": "%s",
                            "body": "%s",
                            "icon": "/icon.png",
                            "image": "/notification-banner.jpg",
                            "vibrate": [200, 100, 200],
                            "timestamp": %d
                        }
                        """,
                notificationTitle,
                notificationBody,
                System.currentTimeMillis()
        );

        for (Subscription sub : subscribersService.getSubscriptions()) {
            try {
                notificationService.sendManualNotification(sub, notificationTitle, notificationBody);
            } catch (Exception e) {
                System.out.println("/send error: " + e.getMessage());
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
