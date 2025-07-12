package org.example.pushnotifmvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PushNotifMvpApplication {
    public static void main(String[] args) {
        SpringApplication.run(PushNotifMvpApplication.class, args);
    }
}
