// Main application entry point
import { checkAuth } from './auth.js';
import { startNotificationPolling } from './notifications.js';

// Initialize application when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('Voting System initialized');
    checkAuth();
    startNotificationPolling();
});

// Handle page unload
window.addEventListener('beforeunload', () => {
    // Cleanup if needed
});

// Global error handler
window.addEventListener('error', (event) => {
    console.error('Global error:', event.error);
});

// Handle unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
});