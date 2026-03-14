import { ENDPOINTS } from './config.js';
import { getCurrentUser, setCurrentUser, getAuthToken, setAuthTokens, clearAuthTokens, getRefreshToken } from './state.js';
import { showDashboardByRole } from './navigation.js';
import { updateNavbar } from './navigation.js';
import { showError, showSuccess } from './utils.js';
import { stopNotificationPolling, startNotificationPolling } from './notifications.js';

/**
 * Check if JWT token is expired
 */
function isTokenExpired(token) {
    if (!token) return true;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const expirationTime = payload.exp * 1000; // Convert to milliseconds
        const currentTime = Date.now();

        // Add 1 minute buffer to refresh before actual expiry
        return currentTime >= (expirationTime - 60000);
    } catch (error) {
        console.error('Error parsing token:', error);
        return true; // Treat invalid tokens as expired
    }
}

/**
 * Refresh access token using refresh token
 */
async function refreshAccessToken() {
    const refreshToken = getRefreshToken();

    if (!refreshToken) {
        console.log('No refresh token available');
        return false;
    }

    try {
        const response = await fetch(ENDPOINTS.AUTH.REFRESH, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (!response.ok) {
            console.error('Token refresh failed');
            return false;
        }

        const data = await response.json();

        // Save new tokens
        setAuthTokens(data.accessToken, data.refreshToken || refreshToken);

        console.log('Token refreshed successfully');
        return true;

    } catch (error) {
        console.error('Error refreshing token:', error);
        return false;
    }
}

/**
 * Check authentication on page load
 */
export function checkAuth() {
    const token = getAuthToken();
    const refreshToken = getRefreshToken();

    if (token) {
        // Check if token is expired
        if (isTokenExpired(token)) {
            console.log('Token expired, attempting refresh...');
            // Try to refresh token
            if (refreshToken) {
                refreshAccessToken().then(success => {
                    if (success) {
                        fetchCurrentUser();
                    } else {
                        logout();
                    }
                });
            } else {
                logout();
            }
        } else {
            // Token is still valid
            fetchCurrentUser();
        }
    } else if (refreshToken) {
        // If access token is missing but refresh token exists, restore session silently
        refreshAccessToken().then(success => {
            if (success) {
                fetchCurrentUser();
            } else {
                logout();
            }
        });
    } else {
        // No token, show login
        showLogin();
    }
}

/**
 * Fetch current user with improved error handling
 */
export async function fetchCurrentUser() {
    const token = getAuthToken();

    if (!token) {
        showLogin();
        return;
    }

    try {
        const response = await fetch(ENDPOINTS.AUTH.ME, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 401) {
            // Unauthorized - token might be expired, try to refresh
            console.log('Unauthorized, attempting token refresh...');
            const refreshed = await refreshAccessToken();
            if (refreshed) {
                // Retry fetching user with new token
                return fetchCurrentUser();
            } else {
                logout();
            }
            return;
        }

        if (!response.ok) {
            console.error('Failed to fetch user, status:', response.status);
            // Don't logout on server errors (5xx) or other non-auth errors
            // Only logout on auth issues (401)
            if (response.status === 401 || response.status === 403) {
                logout();
            }
            return;
        }

        const user = await response.json();
        setCurrentUser(user);
        updateNavbar();
        showDashboardByRole();
        startNotificationPolling();

    } catch (error) {
        console.error('Network error fetching user:', error);
        // Don't logout on network errors - user can refresh manually
        // Just show a warning in console
        console.warn('Could not fetch user data. You may need to refresh the page.');
    }
}

/**
 * Enhanced fetch wrapper that handles token refresh automatically
 */
export async function authenticatedFetch(url, options = {}) {
    let token = getAuthToken();

    // Check if token is expired and refresh if needed
    if (isTokenExpired(token)) {
        const refreshed = await refreshAccessToken();
        if (!refreshed) {
            logout();
            throw new Error('Authentication failed');
        }
        token = getAuthToken();
    }

    // Make request with auth header
    const authOptions = {
        ...options,
        headers: {
            ...options.headers,
            'Authorization': `Bearer ${token}`
        }
    };

    let response = await fetch(url, authOptions);

    // If 401 Unauthorized, try to refresh token once and retry
    if (response.status === 401) {
        console.log('Got 401, attempting token refresh...');
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            // Retry request with new token
            authOptions.headers['Authorization'] = `Bearer ${getAuthToken()}`;
            response = await fetch(url, authOptions);
        } else {
            logout();
            throw new Error('Authentication failed');
        }
    }

    return response;
}

export async function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(ENDPOINTS.AUTH.LOGIN, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                usernameOrEmail: username,
                password: password
            })
        });

        if (!response.ok) {
            const error = await response.json();
            showError('login-error', error.message || 'Login failed');
            return;
        }

        const data = await response.json();

        setAuthTokens(data.accessToken, data.refreshToken);
        setCurrentUser(data.user);

        showDashboardByRole();
        updateNavbar();
        startNotificationPolling();

    } catch (error) {
        showError('login-error', 'Network error. Please try again.');
    }
}

export async function handleRegister(event) {
    event.preventDefault();

    const username = document.getElementById('register-username').value;
    const email = document.getElementById('register-email').value;
    const fullName = document.getElementById('register-fullname').value;
    const nationalId = document.getElementById('register-nationalid').value;
    const password = document.getElementById('register-password').value;

    try {
        const response = await fetch(ENDPOINTS.AUTH.REGISTER, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username, email, fullName, nationalId, password
            })
        });

        const data = await response.json();

        if (!response.ok) {
            showError('register-error', data.message || 'Registration failed');
            return;
        }

        showSuccess('register-success', 'Registration successful! Please check your email to verify your account.');
        document.getElementById('register-form').reset();
        setTimeout(() => showLogin(), 2000);

    } catch (error) {
        showError('register-error', 'Network error. Please try again.');
    }
}

export async function logout() {
    const token = getAuthToken();

    if (token) {
        try {
            await fetch(ENDPOINTS.AUTH.LOGOUT, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            });
        } catch (error) {
            console.error('Logout error:', error);
        }
    }

    clearAuthTokens();
    setCurrentUser(null);
    stopNotificationPolling();

    showLogin();
    updateNavbar();
}

export function showLogin() {
    hideAllPages();
    document.getElementById('login-page').classList.remove('hidden');
}

export function showRegister() {
    hideAllPages();
    document.getElementById('register-page').classList.remove('hidden');
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

// Expose functions to window
window.handleLogin = handleLogin;
window.handleRegister = handleRegister;
window.logout = logout;
window.showLogin = showLogin;
window.showRegister = showRegister;