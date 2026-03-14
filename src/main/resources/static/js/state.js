export const state = {
    currentUser: null,
    selectedCandidateId: null,
    currentElectionId: null,
    editingElectionId: null,
    selectedVoters: [] // For election creation/editing
};

export function getCurrentUser() {
    if (state.currentUser) {
        return state.currentUser;
    }

    const userFromStorage = localStorage.getItem('currentUser');
    if (!userFromStorage) {
        return null;
    }

    try {
        state.currentUser = JSON.parse(userFromStorage);
    } catch (error) {
        console.error('Error parsing current user from localStorage:', error);
        localStorage.removeItem('currentUser');
        state.currentUser = null;
    }

    return state.currentUser;
}

export function setCurrentUser(user) {
    state.currentUser = user;

    if (user) {
        localStorage.setItem('currentUser', JSON.stringify(user));
    } else {
        localStorage.removeItem('currentUser');
    }
}

export function getSelectedCandidateId() {
    return state.selectedCandidateId;
}

export function setSelectedCandidateId(id) {
    state.selectedCandidateId = id;
}

export function getCurrentElectionId() {
    return state.currentElectionId;
}

export function setCurrentElectionId(id) {
    state.currentElectionId = id;
}

export function getEditingElectionId() {
    return state.editingElectionId;
}

export function setEditingElectionId(id) {
    state.editingElectionId = id;
}

export function getSelectedVoters() {
    return state.selectedVoters;
}

export function setSelectedVoters(voters) {
    state.selectedVoters = voters;
}

export function addSelectedVoter(voter) {
    if (!state.selectedVoters.find(v => v.id === voter.id)) {
        state.selectedVoters.push(voter);
    }
}

export function removeSelectedVoter(voterId) {
    state.selectedVoters = state.selectedVoters.filter(v => v.id !== voterId);
}

export function clearSelectedVoters() {
    state.selectedVoters = [];
}

export function getRefreshToken() {
    return localStorage.getItem('refreshToken');
}

export function getAuthToken() {
    return localStorage.getItem('accessToken');
}

export function setAuthTokens(accessToken, refreshToken) {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
}

export function clearAuthTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
}
