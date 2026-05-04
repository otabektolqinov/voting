export const API_URL = 'https://onlinevotingsystem.dev/api';

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
        IMPORT: `${API_URL}/admin/users/import`,
        IMPORT_APPROVED: `${API_URL}/admin/approved-voters/import`,
        byId: (id) => `${API_URL}/users/${id}`,
        toggleActivation: (id) => `${API_URL}/users/${id}/toggle-activation`,
        updateRole: (id) => `${API_URL}/users/${id}/role`,
        delete: (id) => `${API_URL}/users/${id}`,
    }
};

export const POLLING_INTERVALS = {
    NOTIFICATIONS: 30000 // 30 seconds
};