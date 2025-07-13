const publicKey = 'BA0SFbfhI1QJgd3rtq5-_NPq1EtMC162SZaHmCG49jNGcgHwWmgwBR18_f34KibtouwYlLglDIi1wJ6G4qxNg04';

const statusElement = document.getElementById('status');
const counterElement = document.getElementById('counterValue');
const counterContainer = document.getElementById('notificationCounter');

let notificationCount = 0;

setInterval(function(){
    fetch('/ping.txt');
}, 20000)

function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/\-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    return new Uint8Array([...rawData].map(char => char.charCodeAt(0)));
}

function updateCounter() {
    counterElement.textContent = notificationCount;

    // Flash green
    counterContainer.classList.remove('alert-dark', 'alert-success');
    void counterContainer.offsetWidth; // Trigger reflow
    counterContainer.classList.add('alert-success');

    // Revert color after 1 second
    setTimeout(() => {
        counterContainer.classList.replace('alert-success', 'alert-dark');
    }, 1000);
}

async function subscribe() {
    try {
        const registration = await navigator.serviceWorker.register('sw.js');
        const permission = await Notification.requestPermission();

        if (permission !== 'granted') {
            statusElement.textContent = "Permission denied";
            statusElement.className = "alert alert-danger";
            return;
        }

        const subscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: urlBase64ToUint8Array(publicKey)
        });

        await fetch('/api/notifications/subscribe', {
            method: 'POST',
            body: JSON.stringify(subscription),
            headers: {'Content-Type': 'application/json'}
        });

        statusElement.textContent = "Subscribed! ✅";
        statusElement.className = "alert alert-success";
        updateSubscriptionStatus(); // Check actual subscription state
    } catch (error) {
        statusElement.textContent = `Error: ${error.message}`;
        statusElement.className = "alert alert-danger";
    }
}

async function sendCustom() {
    const customMessage = document.getElementById('customMessage').value;

    try {
        const response = await fetch(`/api/notifications/send-custom?title=Custom%20Message&body=${encodeURIComponent(customMessage || 'Default message')}`, {
            method: 'POST'
        });

        if (response.ok) {
            alert('Notification sent with: ' + (customMessage || 'default message'));
        } else {
            alert('Failed: ' + await response.text());
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function unsubscribe() {
    try {
        const registration = await navigator.serviceWorker.getRegistration();
        if (!registration) {
            statusElement.textContent = "No registration found";
            statusElement.className = "alert alert-warning";
            return;
        }

        const subscription = await registration.pushManager.getSubscription();
        if (!subscription) {
            statusElement.textContent = "Not subscribed";
            statusElement.className = "alert alert-secondary";
            return;
        }

        await subscription.unsubscribe();
        await fetch('/api/notifications/unsubscribe', {
            method: 'POST',
            body: JSON.stringify(subscription.toJSON()),
            headers: {'Content-Type': 'application/json'}
        });

        statusElement.textContent = "Unsubscribed! ❌";
        statusElement.className = "alert alert-secondary";
    } catch (error) {
        statusElement.textContent = `Unsubscribe failed: ${error.message}`;
        statusElement.className = "alert alert-danger";
    }
}

async function updateSubscriptionStatus() {
    const registration = await navigator.serviceWorker.getRegistration();
    if (!registration) {
        statusElement.textContent = "Service Worker not registered";
        statusElement.className = "alert alert-warning";
        return;
    }

    const subscription = await registration.pushManager.getSubscription();
    if (subscription) {
        statusElement.textContent = "Subscribed! ✅";
        statusElement.className = "alert alert-success";
    } else {
        statusElement.textContent = "Not subscribed";
        statusElement.className = "alert alert-secondary";
    }
}

async function startScheduler() {
    fetch('/api/notifications/scheduler?enable=true', {method: 'POST'})
        .then(response => response.ok ? alert('Scheduler started') : alert('Error'))
        .catch(err => alert('Error: ' + err));
}

async function stopScheduler() {
    fetch('/api/notifications/scheduler?enable=false', {method: 'POST'})
        .then(response => response.ok ? alert('Scheduler stopped') : alert('Error'))
        .catch(err => alert('Error: ' + err));
}

navigator.serviceWorker.addEventListener('controllerchange', updateSubscriptionStatus);

navigator.serviceWorker.addEventListener('message', event => {
    if (event.data.type === 'NEW_NOTIFICATION') {
        notificationCount++;
        updateCounter();
    }
});

window.addEventListener('load', () => {
    counterElement.textContent = '0';
    updateSubscriptionStatus();
});

window.addEventListener('error', (event) => {
    console.error("Global error:", event.error);
    statusElement.textContent = "Unexpected error occurred";
    statusElement.className = "alert alert-danger";
});
