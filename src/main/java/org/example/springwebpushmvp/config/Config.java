package org.example.springwebpushmvp.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableAsync
public class Config {
    @Bean
    public PushService pushService(@Value("${vapid.public.key}") String publicKey, @Value("${vapid.private.key}") String privateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        PushService service = new PushService();
        service.setPublicKey(publicKey);
        service.setPrivateKey(privateKey);
        return service;
    }

    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("NotificationExecutor-");
        executor.initialize();
        return executor;
    }
}
