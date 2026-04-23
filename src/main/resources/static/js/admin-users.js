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
    const container = document.getElementById('admin-users-list');

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

window.toggleUserActivation = toggleUserActivation;
window.deleteUser = deleteUser;