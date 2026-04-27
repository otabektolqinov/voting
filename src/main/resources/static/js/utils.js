export function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

export function formatDateForInput(dateString) {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

export function formatTimeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;

    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;

    return formatDate(dateString);
}

// Error and success message utilities
export function showError(elementId, message) {
    const errorDiv = document.getElementById(elementId);
    if (!errorDiv) return;

    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');

    setTimeout(() => {
        errorDiv.classList.add('hidden');
    }, 5000);
}

export function showSuccess(elementId, message) {
    const successDiv = document.getElementById(elementId);
    if (!successDiv) return;

    successDiv.textContent = message;
    successDiv.classList.remove('hidden');

    setTimeout(() => {
        successDiv.classList.add('hidden');
    }, 5000);
}

// Local storage utilities
export function setLocalStorage(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
        console.error('Error saving to localStorage:', error);
    }
}

export function getLocalStorage(key) {
    try {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    } catch (error) {
        console.error('Error reading from localStorage:', error);
        return null;
    }
}

export function removeLocalStorage(key) {
    try {
        localStorage.removeItem(key);
    } catch (error) {
        console.error('Error removing from localStorage:', error);
    }
}

// Validation utilities
export function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

export function validatePassword(password) {
    // At least 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special char
    const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    return re.test(password);
}

// DOM utilities
export function hideElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.classList.add('hidden');
    }
}

export function showElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.classList.remove('hidden');
    }
}

export function toggleElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.classList.toggle('hidden');
    }
}

// Debounce utility for search inputs
export function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// HTTP request utility with error handling
export async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('accessToken');

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers
        });

        if (response.status === 401) {
            // Token expired, redirect to login
            import('./auth.js').then(module => module.logout());
            throw new Error('Session expired. Please login again.');
        }

        return response;
    } catch (error) {
        console.error('Fetch error:', error);
        throw error;
    }
}

export function sanitize(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#x27;');
}