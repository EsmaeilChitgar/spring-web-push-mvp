let notificationCount = 0;

self.addEventListener('install', () => {
    self.skipWaiting(); // Force immediate activation
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cache => caches.delete(cache))
            ).then(() => self.clients.claim());
        })
    );
});

self.addEventListener('push', function (event) {
    try {
        const payload = event.data?.json() || {
            title: 'Notification',
            body: event.data?.text() || 'You have a new update!',
            data: {url: 'https://your-website.com'}
        };

        if (!payload.isScheduled) {
            notificationCount++;
        }

        const notificationData = {
            ...payload,
            count: payload.counter || notificationCount,
            receivedAt: new Date().toISOString()
        };

        self.clients.matchAll().then(clients => {
            clients.forEach(client => {
                client.postMessage({
                    type: 'NEW_NOTIFICATION',
                    count: notificationCount,
                    notificationData: notificationData
                });
            });
        });

        const options = {
            body: payload.body,
            icon: payload.icon || '/icon.png',
            badge: payload.badge || '/badge.png',
            image: payload.image,
            vibrate: payload.vibrate || [200, 100, 200],
            tag: payload.tag || `notif-${notificationCount}`,
            renotify: payload.renotify || false,
            requireInteraction: payload.requireInteraction || false,
            timestamp: payload.timestamp || Date.now(),
            data: {
                ...notificationData,
                url: payload.data?.url || 'https://your-website.com'
            }
        };

        const titlePrefix = payload.isScheduled ? '🔔 Scheduled' : '📬 Manual';
        const title = payload.title
            ? `${titlePrefix}: ${payload.title.replace(/^[🔔📬]\s*/, '')}`
            : `${titlePrefix} Notification #${notificationCount}`;

        event.waitUntil(
            self.registration.showNotification(title, options)
                .then(() => {
                    if (!options.requireInteraction) {
                        setTimeout(() => {
                            self.registration.getNotifications({tag: options.tag})
                                .then(notifications => {
                                    notifications.forEach(notification => {
                                        notification.close();
                                    });
                                });
                        }, payload.duration || (payload.isScheduled ? 2000 : 5000));
                    }
                })
        );

    } catch (e) {
        console.error('Error handling push:', e);
        const fallbackOptions = {
            body: 'New update available!',
            icon: '/icon.png',
            badge: '/badge.png',
            tag: `fallback-${Date.now()}`,
            data: {url: 'https://your-website.com'}
        };
        event.waitUntil(
            self.registration.showNotification('System Notification', fallbackOptions)
        );
    }
});

self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    const url = event.notification.data?.url || 'https://your-website.com';

    event.waitUntil(
        clients.matchAll({
            type: 'window',
            includeUncontrolled: true
        }).then(windowClients => {
            // Check if there's already a window open
            const matchingClient = windowClients.find(client =>
                client.url === url);

            if (matchingClient) {
                return matchingClient.focus();
            } else {
                return clients.openWindow(url);
            }
        })
    );
});
