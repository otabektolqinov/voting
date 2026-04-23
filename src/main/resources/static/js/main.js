// Main application entry point
import { checkAuth } from './auth.js';
import {showAdminDashboard, showDashboard, showProfile, showVoterDashboard} from "./navigation.js";
import {loadAllUsers} from "./admin-users.js";

// Initialize application when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('Voting System initialized');
    checkAuth();
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

window.switchAdminTab = function(tab) {
    // Toggle content
    document.getElementById('admin-tab-elections').classList.toggle('hidden', tab !== 'elections');
    document.getElementById('admin-tab-users').classList.toggle('hidden', tab !== 'users');

    // Toggle Create Election button
    document.getElementById('create-election-btn').classList.toggle('hidden', tab !== 'elections');

    // Toggle tab styles
    const electionsTab = document.getElementById('tab-elections');
    const usersTab = document.getElementById('tab-users');

    if (tab === 'elections') {
        electionsTab.className = 'py-4 px-1 border-b-2 border-indigo-600 font-semibold text-indigo-600 text-sm';
        usersTab.className = 'py-4 px-1 border-b-2 border-transparent font-semibold text-gray-500 hover:text-gray-700 text-sm';
    } else {
        usersTab.className = 'py-4 px-1 border-b-2 border-indigo-600 font-semibold text-indigo-600 text-sm';
        electionsTab.className = 'py-4 px-1 border-b-2 border-transparent font-semibold text-gray-500 hover:text-gray-700 text-sm';
        loadAllUsers(); // load users when tab is clicked
    }
}
