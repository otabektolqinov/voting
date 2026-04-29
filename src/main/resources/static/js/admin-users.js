import { ENDPOINTS } from './config.js';
import { getAuthToken } from './state.js';

export async function loadUserStats() {
    const token = getAuthToken();
    try {
        const response = await fetch(ENDPOINTS.USERS.BASE, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const users = await response.json();

        return {
            total: users.length,
            active: users.filter(u => u.accountActivated).length,
            voters: users.filter(u => u.role === 'VOTER').length,
        };
    } catch (error) {
        console.error('Error loading user stats:', error);
        return { total: 0, active: 0, voters: 0 };
    }
}

export async function loadAllUsers() {
    const token = getAuthToken();

    renderImportSections();

    try {
        const response = await fetch(ENDPOINTS.USERS.BASE, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const users = await response.json();
        renderUsersTable(users);
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

function renderUsersTable(users) {
    const container = document.getElementById('users-table-container');

    if (users.length === 0) {
        container.innerHTML = `
            <div class="text-center py-12">
                <p class="text-gray-500 text-lg">No users found</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
                <tr>
                    <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">User</th>
                    <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Role</th>
                    <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Status</th>
                    <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Email Verified</th>
                    <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Last Login</th>
                    <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Actions</th>
                </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
                ${users.map(user => `
                    <tr class="hover:bg-gray-50 transition-colors">
                        <td class="px-6 py-4">
                            <div class="text-sm font-semibold text-gray-900">${user.fullName}</div>
                            <div class="text-xs text-gray-500">${user.email}</div>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${user.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'}">
                                ${user.role}
                            </span>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${user.accountActivated ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                                ${user.accountActivated ? 'Active' : 'Inactive'}
                            </span>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${user.emailVerified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}">
                                ${user.emailVerified ? 'Verified' : 'Unverified'}
                            </span>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            ${user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'}
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                            <div class="flex gap-3">
                                <button onclick="toggleUserActivation('${user.id}', ${user.accountActivated})"
                                        class="${user.accountActivated ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'} font-semibold hover:underline">
                                    ${user.accountActivated ? 'Deactivate' : 'Activate'}
                                </button>
                                <button onclick="deleteUser('${user.id}', '${user.fullName}')"
                                        class="text-red-600 hover:text-red-900 font-semibold hover:underline">
                                    Delete
                                </button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

export async function toggleUserActivation(userId, currentStatus) {
    const action = currentStatus ? 'deactivate' : 'activate';
    if (!confirm(`Are you sure you want to ${action} this user?`)) return;

    const token = getAuthToken();
    try {
        const response = await fetch(ENDPOINTS.USERS.toggleActivation(userId), {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            alert('Failed to update user');
            return;
        }

        loadAllUsers();
    } catch (error) {
        console.error('Error toggling activation:', error);
    }
}

function renderImportSections() {
    if (document.getElementById('import-sections')) return;

    const container = document.getElementById('admin-users-list');

    const importDiv = document.createElement('div');
    importDiv.id = 'import-sections';
    importDiv.className = 'p-6';
    importDiv.innerHTML = `
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            
            <!-- Import Users -->
            <div class="bg-white rounded-xl shadow-md p-6 border-2 border-dashed border-indigo-200 hover:border-indigo-400 transition-colors">
                <div class="flex items-center mb-4">
                    <div class="bg-indigo-100 p-3 rounded-xl mr-4">
                        <svg class="h-6 w-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                        </svg>
                    </div>
                    <div>
                        <h3 class="font-bold text-gray-900">Import Users</h3>
                        <p class="text-xs text-gray-500">Create user accounts from CSV</p>
                    </div>
                </div>
                <p class="text-xs text-gray-400 mb-3">CSV format: username, email, fullName, nationalId</p>
                <div class="flex gap-3 items-center">
                    <input type="file" id="import-users-file" accept=".csv"
                           class="block w-full text-sm text-gray-500 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100 cursor-pointer"/>
                    <button onclick="handleImportUsers()"
                            class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-semibold text-sm whitespace-nowrap transition-colors">
                        Upload
                    </button>
                </div>
                <p id="import-users-status" class="text-sm mt-3 hidden"></p>
            </div>

            <!-- Import Approved Voters -->
            <div class="bg-white rounded-xl shadow-md p-6 border-2 border-dashed border-green-200 hover:border-green-400 transition-colors">
                <div class="flex items-center mb-4">
                    <div class="bg-green-100 p-3 rounded-xl mr-4">
                        <svg class="h-6 w-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                    </div>
                    <div>
                        <h3 class="font-bold text-gray-900">Import Approved Voters</h3>
                        <p class="text-xs text-gray-500">Add eligible voters to whitelist</p>
                    </div>
                </div>
                <p class="text-xs text-gray-400 mb-3">CSV format: nationalId, email, fullName</p>
                <div class="flex gap-3 items-center">
                    <input type="file" id="import-approved-file" accept=".csv"
                           class="block w-full text-sm text-gray-500 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100 cursor-pointer"/>
                    <button onclick="handleImportApprovedVoters()"
                            class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 font-semibold text-sm whitespace-nowrap transition-colors">
                        Upload
                    </button>
                </div>
                <p id="import-approved-status" class="text-sm mt-3 hidden"></p>
            </div>

        </div>
    `;

    // ✅ prepend before the table
    container.insertBefore(importDiv, container.firstChild);
}

export async function handleImportUsers() {
    const file = document.getElementById('import-users-file').files[0];
    const status = document.getElementById('import-users-status');

    if (!file) {
        showStatus(status, 'Please select a CSV file', 'error');
        return;
    }

    const token = getAuthToken();
    const formData = new FormData();
    formData.append('file', file);

    showStatus(status, 'Uploading...', 'loading');

    try {
        const response = await fetch(ENDPOINTS.USERS.IMPORT, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });

        const data = await response.json();

        if (!response.ok) {
            showStatus(status, data.message || 'Import failed', 'error');
            return;
        }

        showStatus(status, data.message || 'Users imported successfully!', 'success');
        document.getElementById('import-users-file').value = '';
        loadAllUsers();

    } catch (error) {
        console.error('Error importing users:', error);
        showStatus(status, 'Network error. Please try again.', 'error');
    }
}

export async function handleImportApprovedVoters() {
    const file = document.getElementById('import-approved-file').files[0];
    const status = document.getElementById('import-approved-status');

    if (!file) {
        showStatus(status, 'Please select a CSV file', 'error');
        return;
    }

    const token = getAuthToken();
    const formData = new FormData();
    formData.append('file', file);

    showStatus(status, 'Uploading...', 'loading');

    try {
        const response = await fetch(ENDPOINTS.USERS.IMPORT_APPROVED, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });

        const data = await response.json();

        if (!response.ok) {
            showStatus(status, data.message || 'Import failed', 'error');
            return;
        }

        showStatus(status, data.message || 'Approved voters imported successfully!', 'success');
        document.getElementById('import-approved-file').value = '';

    } catch (error) {
        console.error('Error importing approved voters:', error);
        showStatus(status, 'Network error. Please try again.', 'error');
    }
}

function showStatus(element, message, type) {
    element.classList.remove('hidden', 'text-green-600', 'text-red-600', 'text-gray-500');

    if (type === 'success') element.classList.add('text-green-600');
    else if (type === 'error') element.classList.add('text-red-600');
    else element.classList.add('text-gray-500');

    element.textContent = message;
}

export async function deleteUser(userId, fullName) {
    if (!confirm(`Are you sure you want to delete ${fullName}? This cannot be undone.`)) return;

    const token = getAuthToken();
    try {
        const response = await fetch(ENDPOINTS.USERS.delete(userId), {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            alert('Failed to delete user');
            return;
        }

        loadAllUsers();
    } catch (error) {
        console.error('Error deleting user:', error);
    }
}

window.handleImportUsers = handleImportUsers;
window.handleImportApprovedVoters = handleImportApprovedVoters;
window.toggleUserActivation = toggleUserActivation;
window.deleteUser = deleteUser;