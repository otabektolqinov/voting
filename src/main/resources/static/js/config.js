export const API_URL = 'http://91.99.215.33:9090//api';

export const ENDPOINTS = {
    AUTH: {
        LOGIN: `${API_URL}/auth/login`,
        REGISTER: `${API_URL}/auth/register`,
        LOGOUT: `${API_URL}/auth/logout`,
        REFRESH: `${API_URL}/auth/refresh`,
        ME: `${API_URL}/users/me`
    },
    ELECTIONS: {
        BASE: `${API_URL}/elections`,
        MY_ELECTIONS: `${API_URL}/elections/my-elections`,
        ACTIVE: `${API_URL}/elections/active`,
        byId: (id) => `${API_URL}/elections/${id}`,
        candidates: (id) => `${API_URL}/elections/${id}/candidates`,
        results: (id) => `${API_URL}/elections/${id}/results`,
        voters: (id) => `${API_URL}/elections/${id}/voters`,
        activate: (id) => `${API_URL}/elections/${id}/activate`,
        publishResults: (id) => `${API_URL}/elections/${id}/publish-results`
    },
    VOTES: `${API_URL}/votes`,
    NOTIFICATIONS: {
        UNREAD: `${API_URL}/notifications/unread`,
        UNREAD_COUNT: `${API_URL}/notifications/unread/count`,
        READ_ALL: `${API_URL}/notifications/read-all`,
        markAsRead: (id) => `${API_URL}/notifications/${id}/read`
    },
    USERS: {
        BASE: `${API_URL}/users`,
        SEARCH: `${API_URL}/users/search`,
        byId: (id) => `/api/users/${id}`,
        toggleActivation: (id) => `/api/users/${id}/toggle-activation`,
        updateRole: (id) => `/api/users/${id}/role`,
        delete: (id) => `/api/users/${id}`,
    }
};

export const POLLING_INTERVALS = {
    NOTIFICATIONS: 30000 // 30 seconds
};