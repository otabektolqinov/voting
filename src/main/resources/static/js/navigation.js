import { getCurrentUser } from './state.js';
import { loadElections } from './voter-dashboard.js';
import { loadAdminStats, loadAllElections } from './admin-dashboard.js';
import {loadAllUsers} from "./admin-users.js";

export function showDashboardByRole() {
    const user = getCurrentUser();
    if (!user) {
        showLogin();
        return;
    }

    if (user.role === 'ADMIN' || user.role === 'ELECTION_OFFICER') {
        showAdminDashboard();
    } else {
        showVoterDashboard();
    }
}

export function updateNavbar() {
    const navLinks = document.getElementById('nav-links');
    const loginButton = document.getElementById('login-button');
    const user = getCurrentUser();

    if (!navLinks || !loginButton) return;

    if (user) {
        navLinks.classList.remove('hidden');
        navLinks.classList.add('flex');
        loginButton.classList.add('hidden');

        document.getElementById('user-name').textContent = user.fullName;

        const adminLink = document.getElementById('admin-link');
        if (adminLink) {
            if (user.role === 'ADMIN' || user.role === 'ELECTION_OFFICER') {
                adminLink.classList.remove('hidden');
                adminLink.onclick = (event) => {
                    event.preventDefault();
                    showAdminDashboard();
                };
            } else {
                adminLink.classList.add('hidden');
                adminLink.onclick = null;
            }
        }
    } else {
        navLinks.classList.add('hidden');
        loginButton.classList.remove('hidden');
    }
}

export function showVoterDashboard() {
    const user = getCurrentUser();
    if (!user) {
        showLogin();
        return;
    }

    hideAllPages();
    document.getElementById('dashboard-page').classList.remove('hidden');
    loadElections();
}

export function showAdminDashboard() {
    const user = getCurrentUser();
    if (!user) {
        showLogin();
        return;
    }

    if (user.role !== 'ADMIN' && user.role !== 'ELECTION_OFFICER') {
        alert('Access denied. Admin privileges required.');
        showVoterDashboard();
        return;
    }

    hideAllPages();
    document.getElementById('admin-dashboard-page').classList.remove('hidden');
    loadAdminStats();
    loadAllElections();
}

export function showProfile() {
    const user = getCurrentUser();
    if (!user) {
        showLogin();
        return;
    }

    hideAllPages();
    document.getElementById('profile-page').classList.remove('hidden');

    document.getElementById('profile-username').textContent = user.username;
    document.getElementById('profile-email').textContent = user.email;
    document.getElementById('profile-fullname').textContent = user.fullName;
    document.getElementById('profile-role').textContent = user.role;

    const activatedBadge = document.getElementById('profile-activated');
    activatedBadge.textContent = user.accountActivated ? 'Activated' : 'Not Activated';
    activatedBadge.className = user.accountActivated
        ? 'px-3 py-1 rounded-full text-sm bg-green-100 text-green-800'
        : 'px-3 py-1 rounded-full text-sm bg-red-100 text-red-800';

    const verifiedBadge = document.getElementById('profile-verified');
    verifiedBadge.textContent = user.emailVerified ? 'Email Verified' : 'Email Not Verified';
    verifiedBadge.className = user.emailVerified
        ? 'px-3 py-1 rounded-full text-sm bg-green-100 text-green-800'
        : 'px-3 py-1 rounded-full text-sm bg-yellow-100 text-yellow-800';
}

function hideAllPages() {
    const pages = [
        'login-page', 'register-page', 'dashboard-page', 'admin-dashboard-page',
        'election-details-page', 'profile-page', 'create-election-page',
        'election-results-page'
    ];
    pages.forEach(page => {
        const element = document.getElementById(page);
        if (element) element.classList.add('hidden');
    });
}

function showLogin() {
    // Import dynamically to avoid circular dependency
    import('./auth.js').then(module => module.showLogin());
}

export function showDashboard() {
    showDashboardByRole();
}

// Make functions globally available
window.showDashboard = showDashboard;
window.showAdminDashboard = showAdminDashboard;
window.showVoterDashboard = showVoterDashboard;
window.showProfile = showProfile;