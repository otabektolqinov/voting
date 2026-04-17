import { ENDPOINTS } from './config.js';
import { getAuthToken } from './state.js';

let selectedVoters = [];
let searchTimeout = null;

/**
 * Initialize voter autocomplete
 */
export function initVoterAutocomplete(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = `
        <div class="voter-autocomplete">
            <!-- Search Input -->
            <div class="relative mb-4">
                <input type="text" 
                       id="voter-search-input"
                       placeholder="Type email to search voters..."
                       class="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
                       autocomplete="off">
                
                <!-- Dropdown Results -->
                <div id="voter-search-results" 
                     class="absolute z-10 w-full bg-white border-2 border-gray-300 rounded-lg mt-1 max-h-60 overflow-y-auto hidden shadow-lg">
                </div>
            </div>
            
            <!-- Selected Voters List -->
            <div id="selected-voters-list" class="space-y-2">
                <div class="text-gray-500 text-sm italic py-4 text-center border-2 border-dashed border-gray-300 rounded-lg">
                    No voters selected. Search and add voters above.
                </div>
            </div>
        </div>
    `;

    // Setup event listeners
    const searchInput = document.getElementById('voter-search-input');
    searchInput.addEventListener('input', (e) => handleSearch(e.target.value));

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.voter-autocomplete')) {
            hideSearchResults();
        }
    });
}

/**
 * Handle search with debounce
 */
async function handleSearch(query) {
    if (searchTimeout) {
        clearTimeout(searchTimeout);
    }

    if (!query.trim()) {
        hideSearchResults();
        return;
    }

    searchTimeout = setTimeout(async () => {
        await searchVoters(query);
    }, 300);
}

/**
 * Search voters via API
 */
async function searchVoters(email) {
    const token = getAuthToken();

    try {
        const response = await fetch(`${ENDPOINTS.USERS.SEARCH}?email=${encodeURIComponent(email)}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            console.error('Search failed');
            return;
        }

        const users = await response.json();
        displaySearchResults(users);

    } catch (error) {
        console.error('Error searching voters:', error);
    }
}

/**
 * Display search results
 */
function displaySearchResults(users) {
    const resultsDiv = document.getElementById('voter-search-results');

    if (users.length === 0) {
        resultsDiv.innerHTML = `
            <div class="px-4 py-3 text-gray-500 text-sm">
                No voters found
            </div>
        `;
        resultsDiv.classList.remove('hidden');
        return;
    }

    // Filter out already selected
    const selectedIds = selectedVoters.map(v => v.id);
    const availableUsers = users.filter(u => !selectedIds.includes(u.id));

    if (availableUsers.length === 0) {
        resultsDiv.innerHTML = `
            <div class="px-4 py-3 text-gray-500 text-sm">
                All matching voters already added
            </div>
        `;
        resultsDiv.classList.remove('hidden');
        return;
    }

    resultsDiv.innerHTML = availableUsers.map(user => `
        <div class="px-4 py-3 hover:bg-indigo-50 cursor-pointer border-b border-gray-200 last:border-b-0"
             onclick="window.addVoterFromSearch('${user.id}', '${user.email}', '${user.fullName}')">
            <div class="font-semibold text-gray-900">${user.fullName}</div>
            <div class="text-sm text-gray-600">${user.email}</div>
        </div>
    `).join('');

    resultsDiv.classList.remove('hidden');
}

/**
 * Hide search results
 */
function hideSearchResults() {
    const resultsDiv = document.getElementById('voter-search-results');
    if (resultsDiv) {
        resultsDiv.classList.add('hidden');
    }
}

/**
 * Add voter
 */
window.addVoterFromSearch = function(id, email, fullName) {
    if (selectedVoters.find(v => v.id === id)) {
        return;
    }

    selectedVoters.push({ id, email, fullName });

    document.getElementById('voter-search-input').value = '';
    hideSearchResults();
    renderSelectedVoters();
};

/**
 * Remove voter
 */
window.removeVoterFromList = function(id) {
    selectedVoters = selectedVoters.filter(v => v.id !== id);
    renderSelectedVoters();
};

/**
 * Render selected voters
 */
function renderSelectedVoters() {
    const listDiv = document.getElementById('selected-voters-list');

    if (selectedVoters.length === 0) {
        listDiv.innerHTML = `
            <div class="text-gray-500 text-sm italic py-4 text-center border-2 border-dashed border-gray-300 rounded-lg">
                No voters selected. Search and add voters above.
            </div>
        `;
        return;
    }

    listDiv.innerHTML = `
        <div class="mb-2 text-sm font-semibold text-gray-700">
            Selected Voters (${selectedVoters.length})
        </div>
        ${selectedVoters.map(voter => `
            <div class="flex items-center justify-between bg-indigo-50 border-2 border-indigo-200 rounded-lg px-4 py-3">
                <div>
                    <div class="font-semibold text-gray-900">${voter.fullName}</div>
                    <div class="text-sm text-gray-600">${voter.email}</div>
                </div>
                <button onclick="window.removeVoterFromList('${voter.id}')" 
                        class="text-red-600 hover:text-red-800 font-bold text-xl">
                    ×
                </button>
            </div>
        `).join('')}
    `;
}

/**
 * Get selected voter IDs
 */
export function getSelectedVoterIds() {
    return selectedVoters.map(v => v.id);
}

/**
 * Clear selected voters
 */

export function setSelectedVoters(voters) {
    selectedVoters = voters;
    renderSelectedVoters();
}

export function clearSelectedVoters() {
    selectedVoters = [];
    const listDiv = document.getElementById('selected-voters-list');
    if (listDiv) {
        listDiv.innerHTML = `
            <div class="text-gray-500 text-sm italic py-4 text-center border-2 border-dashed border-gray-300 rounded-lg">
                No voters selected. Search and add voters above.
            </div>
        `;
    }
}