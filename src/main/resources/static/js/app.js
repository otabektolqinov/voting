// API Base URL
const API_URL = 'http://localhost:8080/api';

// Current state
let currentUser = null;
let selectedCandidateId = null;
let currentElectionId = null;

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    startNotificationPolling();
});

// ============================================
// AUTHENTICATION
// ============================================

function checkAuth() {
    const token = localStorage.getItem('accessToken');
    if (token) {
        // User is logged in
        fetchCurrentUser();
    } else {
        // User is not logged in
        showLogin();
    }
}

async function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_URL}/auth/login`, {
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

        // Save tokens
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);

        // Save user info
        currentUser = data.user;

        // Show appropriate dashboard based on role
        showDashboardByRole();
        updateNavbar();

    } catch (error) {
        showError('login-error', 'Network error. Please try again.');
    }
}

async function handleRegister(event) {
    event.preventDefault();

    const username = document.getElementById('register-username').value;
    const email = document.getElementById('register-email').value;
    const fullName = document.getElementById('register-fullname').value;
    const nationalId = document.getElementById('register-nationalid').value;
    const password = document.getElementById('register-password').value;

    try {
        const response = await fetch(`${API_URL}/auth/register`, {
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

        // Clear form
        document.getElementById('register-form').reset();

        // Switch to login after 2 seconds
        setTimeout(() => showLogin(), 2000);

    } catch (error) {
        showError('register-error', 'Network error. Please try again.');
    }
}

async function logout() {
    const token = localStorage.getItem('accessToken');

    try {
        await fetch(`${API_URL}/auth/logout`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
    } catch (error) {
        console.error('Logout error:', error);
    }

    // Clear local storage
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    currentUser = null;

    // Show login page
    showLogin();
    updateNavbar();
}

async function fetchCurrentUser() {
    const token = localStorage.getItem('accessToken');

    try {
        const response = await fetch(`${API_URL}/users/me`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            logout();
            return;
        }

        currentUser = await response.json();
        updateNavbar();

        // Show appropriate dashboard based on role
        showDashboardByRole();

    } catch (error) {
        console.error('Error fetching user:', error);
        logout();
    }
}

// ============================================
// ROLE-BASED ROUTING
// ============================================

function showDashboardByRole() {
    if (!currentUser) {
        showLogin();
        return;
    }

    // Check user role and show appropriate dashboard
    if (currentUser.role === 'ADMIN' || currentUser.role === 'ELECTION_OFFICER') {
        showAdminDashboard();
    } else {
        showVoterDashboard();
    }
}

// ============================================
// NAVIGATION
// ============================================

function updateNavbar() {
    const navLinks = document.getElementById('nav-links');
    const loginButton = document.getElementById('login-button');

    if (currentUser) {
        navLinks.classList.remove('hidden');
        navLinks.classList.add('flex');
        loginButton.classList.add('hidden');

        document.getElementById('user-name').textContent = currentUser.fullName;

        // Show/hide admin link based on role
        const adminLink = document.getElementById('admin-link');
        if (currentUser.role === 'ADMIN' || currentUser.role === 'ELECTION_OFFICER') {
            adminLink.classList.remove('hidden');
            adminLink.onclick = () => showAdminDashboard();
        } else {
            adminLink.classList.add('hidden');
        }

        // Update dashboard link to show appropriate dashboard
        const dashboardLink = document.querySelector('a[onclick="showDashboard()"]');
        if (dashboardLink) {
            dashboardLink.onclick = (e) => {
                e.preventDefault();
                showDashboardByRole();
            };
        }
    } else {
        navLinks.classList.add('hidden');
        loginButton.classList.remove('hidden');
    }
}

function showLogin() {
    hideAllPages();
    document.getElementById('login-page').classList.remove('hidden');
}

function showRegister() {
    hideAllPages();
    document.getElementById('register-page').classList.remove('hidden');
}

// Voter Dashboard - shows elections to vote in
function showVoterDashboard() {
    if (!currentUser) {
        showLogin();
        return;
    }

    hideAllPages();
    document.getElementById('dashboard-page').classList.remove('hidden');
    loadElections();
}

// Keep backward compatibility
function showDashboard() {
    showDashboardByRole();
}

// Admin Dashboard - shows management interface
function showAdminDashboard() {
    if (!currentUser) {
        showLogin();
        return;
    }

    // Check if user has admin privileges
    if (currentUser.role !== 'ADMIN' && currentUser.role !== 'ELECTION_OFFICER') {
        alert('Access denied. Admin privileges required.');
        showVoterDashboard();
        return;
    }

    hideAllPages();
    document.getElementById('admin-dashboard-page').classList.remove('hidden');
    loadAdminStats();
    loadAllElections();
}

function showProfile() {
    if (!currentUser) {
        showLogin();
        return;
    }

    hideAllPages();
    document.getElementById('profile-page').classList.remove('hidden');

    // Fill profile data
    document.getElementById('profile-username').textContent = currentUser.username;
    document.getElementById('profile-email').textContent = currentUser.email;
    document.getElementById('profile-fullname').textContent = currentUser.fullName;
    document.getElementById('profile-role').textContent = currentUser.role;

    // Account status badges
    const activatedBadge = document.getElementById('profile-activated');
    activatedBadge.textContent = currentUser.accountActivated ? 'Activated' : 'Not Activated';
    activatedBadge.className = currentUser.accountActivated
        ? 'px-3 py-1 rounded-full text-sm bg-green-100 text-green-800'
        : 'px-3 py-1 rounded-full text-sm bg-red-100 text-red-800';

    const verifiedBadge = document.getElementById('profile-verified');
    verifiedBadge.textContent = currentUser.emailVerified ? 'Email Verified' : 'Email Not Verified';
    verifiedBadge.className = currentUser.emailVerified
        ? 'px-3 py-1 rounded-full text-sm bg-green-100 text-green-800'
        : 'px-3 py-1 rounded-full text-sm bg-yellow-100 text-yellow-800';
}

function hideAllPages() {
    document.getElementById('login-page').classList.add('hidden');
    document.getElementById('register-page').classList.add('hidden');
    document.getElementById('dashboard-page').classList.add('hidden');
    document.getElementById('admin-dashboard-page').classList.add('hidden');
    document.getElementById('election-details-page').classList.add('hidden');
    document.getElementById('profile-page').classList.add('hidden');
}

// ============================================
// VOTER ELECTIONS (for voters)
// ============================================

async function loadElections() {
    const token = localStorage.getItem('accessToken');

    try {
        const response = await fetch(`${API_URL}/elections/my-elections`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const elections = await response.json();

        const grid = document.getElementById('elections-grid');
        const noElections = document.getElementById('no-elections');

        if (elections.length === 0) {
            grid.innerHTML = '';
            noElections.classList.remove('hidden');
            return;
        }

        noElections.classList.add('hidden');
        grid.innerHTML = elections.map(election => `
            <div class="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-all card-hover cursor-pointer"
                 onclick="viewElection('${election.id}')">
                <div class="flex justify-between items-start mb-4">
                    <h3 class="text-xl font-semibold text-gray-900">${election.title}</h3>
                    <span class="px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(election.status)}">
                        ${election.status}
                    </span>
                </div>
                <p class="text-gray-600 mb-4">${election.description || 'No description'}</p>
                <div class="flex items-center text-sm text-gray-500 space-x-4">
                    <span>📅 ${formatDate(election.startDate)} - ${formatDate(election.endDate)}</span>
                </div>
                ${election.hasVoted ? '<p class="mt-4 text-green-600 font-semibold">✓ You have voted</p>' : ''}
            </div>
        `).join('');

    } catch (error) {
        console.error('Error loading elections:', error);
    }
}

function getStatusColor(status) {
    switch (status) {
        case 'ACTIVE': return 'bg-green-100 text-green-800';
        case 'UPCOMING': return 'bg-blue-100 text-blue-800';
        case 'CLOSED': return 'bg-gray-100 text-gray-800';
        default: return 'bg-gray-100 text-gray-800';
    }
}

async function viewElection(electionId) {
    currentElectionId = electionId;
    const token = localStorage.getItem('accessToken');

    try {
        const response = await fetch(`${API_URL}/elections/${electionId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const election = await response.json();

        hideAllPages();
        document.getElementById('election-details-page').classList.remove('hidden');

        document.getElementById('election-title').textContent = election.title;
        document.getElementById('election-description').textContent = election.description || 'No description';
        document.getElementById('election-dates').textContent =
            `${formatDate(election.startDate)} - ${formatDate(election.endDate)}`;

        // Load candidates
        const candidatesResponse = await fetch(`${API_URL}/elections/${electionId}/candidates`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const candidates = await candidatesResponse.json();

        const candidatesList = document.getElementById('candidates-list');
        candidatesList.innerHTML = candidates.map(candidate => `
            <div class="border rounded-lg p-4 hover:border-indigo-500 cursor-pointer transition-colors"
                 onclick="selectCandidate('${candidate.id}')">
                <input type="radio" name="candidate" value="${candidate.id}" 
                       class="mr-3 h-4 w-4 text-indigo-600">
                <div class="inline-block">
                    <h4 class="font-semibold text-lg">${candidate.name}</h4>
                    ${candidate.partyAffiliation ? `<p class="text-sm text-gray-600">${candidate.partyAffiliation}</p>` : ''}
                    ${candidate.bio ? `<p class="text-sm text-gray-500 mt-1">${candidate.bio}</p>` : ''}
                </div>
            </div>
        `).join('');

        // Show vote button
        document.getElementById('vote-button').classList.remove('hidden');

    } catch (error) {
        console.error('Error loading election details:', error);
    }
}

function selectCandidate(candidateId) {
    selectedCandidateId = candidateId;

    // Update radio button
    const radio = document.querySelector(`input[value="${candidateId}"]`);
    if (radio) radio.checked = true;
}

function confirmVote() {
    if (!selectedCandidateId) {
        alert('Please select a candidate');
        return;
    }

    if (confirm('Are you sure you want to cast this vote? This action cannot be undone.')) {
        castVote();
    }
}

async function castVote() {
    const token = localStorage.getItem('accessToken');

    try {
        const response = await fetch(`${API_URL}/votes`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                electionId: currentElectionId,
                candidateId: selectedCandidateId
            })
        });

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Failed to cast vote');
            return;
        }

        // Show success message
        document.getElementById('vote-button').classList.add('hidden');
        document.getElementById('candidates-list').style.pointerEvents = 'none';
        document.getElementById('candidates-list').style.opacity = '0.6';
        document.getElementById('vote-success').classList.remove('hidden');

    } catch (error) {
        console.error('Error casting vote:', error);
        alert('Network error. Please try again.');
    }
}

// ============================================
// ADMIN DASHBOARD FUNCTIONS
// ============================================

async function loadAdminStats() {
    const token = localStorage.getItem('accessToken');

    try {
        // You can create these endpoints or use mock data for now
        const statsHtml = `
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <div class="bg-white rounded-lg shadow-md p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-gray-500 text-sm">Total Elections</p>
                            <p id="total-elections" class="text-3xl font-bold text-gray-900">-</p>
                        </div>
                        <div class="bg-indigo-100 p-3 rounded-full">
                            <svg class="h-8 w-8 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-lg shadow-md p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-gray-500 text-sm">Total Voters</p>
                            <p id="total-voters" class="text-3xl font-bold text-gray-900">-</p>
                        </div>
                        <div class="bg-green-100 p-3 rounded-full">
                            <svg class="h-8 w-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-lg shadow-md p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-gray-500 text-sm">Total Votes Cast</p>
                            <p id="total-votes" class="text-3xl font-bold text-gray-900">-</p>
                        </div>
                        <div class="bg-purple-100 p-3 rounded-full">
                            <svg class="h-8 w-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"></path>
                            </svg>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('admin-stats').innerHTML = statsHtml;

        // Fetch actual stats if endpoints exist
        // For now, using mock data
        document.getElementById('total-elections').textContent = '0';
        document.getElementById('total-voters').textContent = '0';
        document.getElementById('total-votes').textContent = '0';

    } catch (error) {
        console.error('Error loading admin stats:', error);
    }
}

async function loadAllElections() {
    const token = localStorage.getItem('accessToken');

    try {
        const response = await fetch(`${API_URL}/elections`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const elections = await response.json();

        const container = document.getElementById('admin-elections-list');

        if (elections.length === 0) {
            container.innerHTML = `
                <div class="text-center py-12">
                    <p class="text-gray-500">No elections created yet</p>
                    <button onclick="showCreateElection()" class="mt-4 bg-indigo-600 text-white px-6 py-2 rounded-md hover:bg-indigo-700">
                        Create First Election
                    </button>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Title</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Start Date</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">End Date</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    ${elections.map(election => `
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="text-sm font-medium text-gray-900">${election.title}</div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(election.status)}">
                                    ${election.status}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                ${formatDate(election.startDate)}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                ${formatDate(election.endDate)}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                <button onclick="viewElectionResults('${election.id}')" class="text-indigo-600 hover:text-indigo-900 mr-3">
                                    View Results
                                </button>
                                <button onclick="editElection('${election.id}')" class="text-blue-600 hover:text-blue-900">
                                    Edit
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

    } catch (error) {
        console.error('Error loading elections:', error);
    }
}

function showCreateElection() {
    alert('Create Election feature coming soon!');
    // You can implement a modal or new page for creating elections
}

function viewElectionResults(electionId) {
    alert(`Viewing results for election: ${electionId}`);
    // Implement viewing results
}

function editElection(electionId) {
    alert(`Editing election: ${electionId}`);
    // Implement editing elections
}

// ============================================
// NOTIFICATIONS
// ============================================

let notificationInterval;

function startNotificationPolling() {
    // Poll every 30 seconds
    notificationInterval = setInterval(loadNotificationCount, 30000);
    loadNotificationCount();
}

async function loadNotificationCount() {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    try {
        const response = await fetch(`${API_URL}/notifications/unread/count`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const data = await response.json();
        updateNotificationBadge(data.count);

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

async function toggleNotifications() {
    const dropdown = document.getElementById('notification-dropdown');

    if (dropdown.classList.contains('hidden')) {
        dropdown.classList.remove('hidden');
        await loadNotifications();
    } else {
        dropdown.classList.add('hidden');
    }
}

async function loadNotifications() {
    const token = localStorage.getItem('accessToken');

    try {
        const response = await fetch(`${API_URL}/notifications/unread`, {
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
                 onclick="handleNotificationClick('${notif.id}', '${notif.actionUrl}')">
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
    const token = localStorage.getItem('accessToken');

    try {
        // Mark as read
        await fetch(`${API_URL}/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        // Reload notifications
        await loadNotifications();
        await loadNotificationCount();

        // Navigate if action URL exists
        if (actionUrl && actionUrl !== 'null') {
            // Handle navigation based on URL
            if (actionUrl.includes('/elections/')) {
                const electionId = actionUrl.split('/elections/')[1].split('/')[0];
                viewElection(electionId);
            }
        }

        // Close dropdown
        document.getElementById('notification-dropdown').classList.add('hidden');

    } catch (error) {
        console.error('Error handling notification click:', error);
    }
}

async function markAllAsRead() {
    const token = localStorage.getItem('accessToken');

    try {
        await fetch(`${API_URL}/notifications/read-all`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        await loadNotifications();
        await loadNotificationCount();

    } catch (error) {
        console.error('Error marking all as read:', error);
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function formatTimeAgo(dateString) {
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

function showError(elementId, message) {
    const errorDiv = document.getElementById(elementId);
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');

    setTimeout(() => {
        errorDiv.classList.add('hidden');
    }, 5000);
}

function showSuccess(elementId, message) {
    const successDiv = document.getElementById(elementId);
    successDiv.textContent = message;
    successDiv.classList.remove('hidden');
}

// Close notification dropdown when clicking outside
document.addEventListener('click', (event) => {
    const dropdown = document.getElementById('notification-dropdown');
    const bellButton = event.target.closest('button[onclick="toggleNotifications()"]');

    if (!bellButton && !dropdown.contains(event.target)) {
        dropdown.classList.add('hidden');
    }
});