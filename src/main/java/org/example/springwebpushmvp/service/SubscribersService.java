package org.example.springwebpushmvp.service;

import lombok.Getter;
import nl.martijndwars.webpush.Subscription;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class SubscribersService {
    private final List<Subscription> subscriptions = new ArrayList<>();

    public void subscribe(final Subscription subscription) {
        subscriptions.removeIf(sub -> sub.endpoint.equals(subscription.endpoint));
        subscriptions.add(subscription);
    }

    public void unsubscribe(final Subscription subscription) {
        subscriptions.removeIf(sub ->
                sub.endpoint.equals(subscription.endpoint)
        );
    }
}
