import { ENDPOINTS, POLLING_INTERVALS } from './config.js';
import { getAuthToken } from './state.js';
import { formatTimeAgo } from './utils.js';

let notificationInterval;

export function startNotificationPolling() {
    stopNotificationPolling();
    notificationInterval = setInterval(loadNotificationCount, POLLING_INTERVALS.NOTIFICATIONS);
    loadNotificationCount();
}

export function stopNotificationPolling() {
    if (notificationInterval) {
        clearInterval(notificationInterval);
    }
}

export async function loadNotificationCount() {
    const token = getAuthToken();
    if (!token) return;

    try {
        const response = await fetch(ENDPOINTS.NOTIFICATIONS.UNREAD_COUNT, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            updateNotificationBadge(0);
            return;
        }

        const data = await response.json();
        updateNotificationBadge(data.count || 0);

    } catch (error) {
        console.error('Error loading notification count:', error);
    }
}

function updateNotificationBadge(count) {
    const badge = document.getElementById('notification-badge');
    if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.style.display = 'block';
    } else {
        badge.style.display = 'none';
    }
}

export async function toggleNotifications() {
    const dropdown = document.getElementById('notification-dropdown');

    if (dropdown.classList.contains('hidden')) {
        dropdown.classList.remove('hidden');
        await loadNotifications();
    } else {
        dropdown.classList.add('hidden');
    }
}

async function loadNotifications() {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.NOTIFICATIONS.UNREAD, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const notifications = await response.json();
        const list = document.getElementById('notification-list');

        if (notifications.length === 0) {
            list.innerHTML = '<div class="p-4 text-center text-gray-500">No new notifications</div>';
            return;
        }

        list.innerHTML = notifications.map(notif => `
            <div class="p-4 border-b hover:bg-gray-50 cursor-pointer notification-item"
                 onclick="handleNotificationClick('${notif.id}', '${notif.actionUrl || ''}')">
                <h4 class="font-semibold text-sm">${notif.title}</h4>
                <p class="text-sm text-gray-600 mt-1">${notif.message}</p>
                <p class="text-xs text-gray-400 mt-1">${formatTimeAgo(notif.sentAt)}</p>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error loading notifications:', error);
    }
}

async function handleNotificationClick(notificationId, actionUrl) {
    const token = getAuthToken();

    try {
        await fetch(ENDPOINTS.NOTIFICATIONS.markAsRead(notificationId), {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        await loadNotifications();
        await loadNotificationCount();

        if (actionUrl && actionUrl !== 'null' && actionUrl !== '') {
            if (actionUrl.includes('/elections/')) {
                const electionId = actionUrl.split('/elections/')[1].split('/')[0];
                // Dynamic import to avoid circular dependency
                import('./voter-dashboard.js').then(module => {
                    module.viewElection(electionId);
                });
            }
        }

        document.getElementById('notification-dropdown').classList.add('hidden');

    } catch (error) {
        console.error('Error handling notification click:', error);
    }
}

export async function markAllAsRead() {
    const token = getAuthToken();

    try {
        await fetch(ENDPOINTS.NOTIFICATIONS.READ_ALL, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        await loadNotifications();
        await loadNotificationCount();

    } catch (error) {
        console.error('Error marking all as read:', error);
    }
}

// Close notification dropdown when clicking outside
document.addEventListener('click', (event) => {
    const dropdown = document.getElementById('notification-dropdown');
    if (!dropdown) return;

    const bellButton = event.target.closest('button[onclick="toggleNotifications()"]');

    if (!bellButton && !dropdown.contains(event.target)) {
        dropdown.classList.add('hidden');
    }
});

// Make functions globally available
window.toggleNotifications = toggleNotifications;
window.markAllAsRead = markAllAsRead;
window.handleNotificationClick = handleNotificationClick;