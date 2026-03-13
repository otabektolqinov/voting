import { ENDPOINTS } from './config.js';
import { getAuthToken, setEditingElectionId, getEditingElectionId, getSelectedVoters, setSelectedVoters, clearSelectedVoters, addSelectedVoter, removeSelectedVoter } from './state.js';
import { showAdminDashboard } from './navigation.js';
import { formatDate, formatDateForInput } from './utils.js';

export async function loadAdminStats() {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.BASE, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const elections = await response.json();

        const statsHtml = `
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <div class="bg-gradient-to-br from-indigo-500 to-indigo-700 rounded-2xl shadow-xl p-6 text-white">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-indigo-100 text-sm font-medium">Total Elections</p>
                            <p class="text-4xl font-bold mt-2">${elections.length}</p>
                        </div>
                        <div class="bg-white bg-opacity-20 p-4 rounded-xl">
                            <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-gradient-to-br from-green-500 to-green-700 rounded-2xl shadow-xl p-6 text-white">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-green-100 text-sm font-medium">Active Elections</p>
                            <p class="text-4xl font-bold mt-2">${elections.filter(e => e.status === 'ACTIVE').length}</p>
                        </div>
                        <div class="bg-white bg-opacity-20 p-4 rounded-xl">
                            <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-gradient-to-br from-purple-500 to-purple-700 rounded-2xl shadow-xl p-6 text-white">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-purple-100 text-sm font-medium">Upcoming Elections</p>
                            <p class="text-4xl font-bold mt-2">${elections.filter(e => e.status === 'UPCOMING').length}</p>
                        </div>
                        <div class="bg-white bg-opacity-20 p-4 rounded-xl">
                            <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('admin-stats').innerHTML = statsHtml;

    } catch (error) {
        console.error('Error loading admin stats:', error);
    }
}


export async function loadAllElections() {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.BASE, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const elections = await response.json();
        const container = document.getElementById('admin-elections-list');

        if (elections.length === 0) {
            container.innerHTML = `
                <div class="text-center py-12">
                    <svg class="mx-auto h-16 w-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                    </svg>
                    <p class="text-gray-500 mt-4 text-lg">No elections created yet</p>
                    <button onclick="showCreateElection()" class="mt-6 bg-indigo-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-indigo-700 shadow-lg hover:shadow-xl transition-all">
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
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Title</th>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Status</th>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Start Date</th>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">End Date</th>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider">Actions</th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    ${elections.map(election => {
            // CONDITION A: ACTIVE elections
            if (election.status === 'ACTIVE') {
                return `
                                <tr class="hover:bg-gray-50 transition-colors">
                                    <td class="px-6 py-4">
                                        <div class="text-sm font-semibold text-gray-900">${election.title}</div>
                                        <div class="text-xs text-gray-500">${election.description ? election.description.substring(0, 50) + '...' : ''}</div>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap">
                                        <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(election.status)}">
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
                                        <div class="flex gap-3">
                                            <button onclick="viewElectionResults('${election.id}')" 
                                                    class="text-indigo-600 hover:text-indigo-900 font-semibold hover:underline">
                                                Results
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            `;
            }

            // CONDITION B: UPCOMING (DRAFT) elections
            else if (election.status === 'DRAFT') {
                return `
                                <tr class="hover:bg-gray-50 transition-colors">
                                    <td class="px-6 py-4">
                                        <div class="text-sm font-semibold text-gray-900">${election.title}</div>
                                        <div class="text-xs text-gray-500">${election.description ? election.description.substring(0, 50) + '...' : ''}</div>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap">
                                        <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(election.status)}">
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
                                        <div class="flex gap-3">
                                            <button onclick="activateElection('${election.id}')" 
                                                    class="text-green-600 hover:text-green-900 font-semibold hover:underline">
                                                Activate
                                            </button>
                                            
                                            <button onclick="editElection('${election.id}')" 
                                                    class="text-blue-600 hover:text-blue-900 font-semibold hover:underline">
                                                Edit
                                            </button>
                                            
                                            <button onclick="deleteElection('${election.id}')" 
                                                    class="text-red-600 hover:text-red-900 font-semibold hover:underline">
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            `;
            }

            else if (election.status === 'CLOSED') {
                return `
                                <tr class="hover:bg-gray-50 transition-colors">
                                    <td class="px-6 py-4">
                                        <div class="text-sm font-semibold text-gray-900">${election.title}</div>
                                        <div class="text-xs text-gray-500">${election.description ? election.description.substring(0, 50) + '...' : ''}</div>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap">
                                        <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(election.status)}">
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
                                        <div class="flex gap-3">
                                            <button onclick="viewElectionResults('${election.id}')" 
                                                    class="text-indigo-600 hover:text-indigo-900 font-semibold hover:underline">
                                                Results
                                            </button>
                                            
                                            <button onclick="deleteElection('${election.id}')" 
                                                    class="text-red-600 hover:text-red-900 font-semibold hover:underline">
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            `;
            }

            else {
                return `
                                <tr class="hover:bg-gray-50 transition-colors">
                                    <td class="px-6 py-4">
                                        <div class="text-sm font-semibold text-gray-900">${election.title}</div>
                                        <div class="text-xs text-gray-500">${election.description ? election.description.substring(0, 50) + '...' : ''}</div>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap">
                                        <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(election.status)}">
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
                                        <div class="flex gap-3">
                                            <button onclick="deleteElection('${election.id}')" 
                                                    class="text-red-600 hover:text-red-900 font-semibold hover:underline">
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            `;
            }
        }).join('')}
                </tbody>
            </table>
        `;

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

export function showCreateElection() {
    hideAllPages();
    document.getElementById('create-election-page').classList.remove('hidden');
    document.getElementById('election-form-title').textContent = 'Create New Election';
    document.getElementById('election-form').reset();
    document.getElementById('candidates-form-list').innerHTML = '';
    setEditingElectionId(null);
    clearSelectedVoters();

    // Add 2 default candidate fields
    addCandidateField();
    addCandidateField();

    // Update voter selection display
    updateVoterSelectionDisplay();
}

export async function editElection(electionId) {
    console.log('Editing election:', electionId);

    // Fetch the election data from API
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.byId(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            alert('Failed to load election');
            return;
        }

        const election = await response.json();
        console.log('Loaded election:', election);
        console.log('Election candidates:', election.candidates);

        setEditingElectionId(electionId);

        // Show the form
        hideAllPages();
        document.getElementById('create-election-page').classList.remove('hidden');
        document.getElementById('election-form-title').textContent = 'Edit Election';

        // Populate basic fields
        document.getElementById('election-title-input').value = election.title || '';
        document.getElementById('election-description-input').value = election.description || '';
        document.getElementById('election-start-date').value = formatDateForInput(election.startDate);
        document.getElementById('election-end-date').value = formatDateForInput(election.endDate);

        const publicCheckbox = document.getElementById('election-public');
        if (publicCheckbox) {
            publicCheckbox.checked = election.isPublic || false;
        }

        // Handle voters
        if (election.isPublic) {
            clearSelectedVoters();
            const voterSection = document.getElementById('voter-selection-section');
            if (voterSection) voterSection.style.display = 'none';
        } else {
            const voterSection = document.getElementById('voter-selection-section');
            if (voterSection) voterSection.style.display = 'block';
            await loadVotersForForm(electionId);
        }

        // Load candidates
        const candidatesContainer = document.getElementById('candidates-form-list');
        candidatesContainer.innerHTML = '';

        if (election.candidates && Array.isArray(election.candidates) && election.candidates.length > 0) {
            console.log('Loading existing candidates:', election.candidates.length);
            election.candidates.forEach((candidate, index) => {
                const candidateDiv = document.createElement('div');
                candidateDiv.className = 'border-2 rounded-xl p-4 bg-gradient-to-r from-gray-50 to-white';
                candidateDiv.innerHTML = `
                    <div class="flex justify-between items-start mb-3">
                        <h4 class="font-bold text-gray-900">Candidate ${index + 1}</h4>
                        <button type="button" onclick="removeCandidateFromForm(this)" class="text-red-600 hover:text-red-800 hover:bg-red-50 p-1 rounded">
                            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path>
                            </svg>
                        </button>
                    </div>
                    <input type="hidden" class="candidate-id" value="${candidate.id || ''}">
                    <input type="text" class="candidate-name w-full px-4 py-2 border border-gray-300 rounded-lg mb-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Candidate Name" value="${candidate.name || ''}" required>
                    <input type="text" class="candidate-party w-full px-4 py-2 border border-gray-300 rounded-lg mb-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Party Affiliation (Optional)" value="${candidate.partyAffiliation || ''}">
                    <textarea class="candidate-bio w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Biography (Optional)" rows="2">${candidate.bio || ''}</textarea>
                `;
                candidatesContainer.appendChild(candidateDiv);
            });
        } else {
            console.warn('No candidates found, adding empty forms');
            addCandidateField();
            addCandidateField();
        }

    } catch (error) {
        console.error('Error loading election for edit:', error);
        alert('Error loading election. Please try again.');
    }
}

function addCandidateToForm(candidate = null) {
    const container = document.getElementById('candidates-form-list');
    const candidateDiv = document.createElement('div');
    candidateDiv.className = 'candidate-item border rounded-lg p-4 mb-4 bg-white';

    candidateDiv.innerHTML = `
        <input type="hidden" class="candidate-id" value="${candidate?.id || ''}">
        <div class="mb-3">
            <label class="block text-sm font-medium text-gray-700 mb-1">Candidate Name *</label>
            <input type="text" 
                   class="candidate-name w-full px-3 py-2 border border-gray-300 rounded-md" 
                   placeholder="Enter candidate name" 
                   value="${candidate?.name || ''}" 
                   required>
        </div>
        <div class="mb-3">
            <label class="block text-sm font-medium text-gray-700 mb-1">Party Affiliation</label>
            <input type="text" 
                   class="candidate-party w-full px-3 py-2 border border-gray-300 rounded-md" 
                   placeholder="e.g., Independent" 
                   value="${candidate?.partyAffiliation || ''}">
        </div>
        <div class="mb-3">
            <label class="block text-sm font-medium text-gray-700 mb-1">Bio</label>
            <textarea class="candidate-bio w-full px-3 py-2 border border-gray-300 rounded-md" 
                      rows="3"
                      placeholder="Brief description">${candidate?.bio || ''}</textarea>
        </div>
        <button type="button" 
                class="px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600"
                onclick="this.parentElement.remove()">
            Remove Candidate
        </button>
    `;

    container.appendChild(candidateDiv);
}

async function loadCandidatesForForm(electionId) {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.candidates(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const candidates = await response.json();
        const candidatesList = document.getElementById('candidates-form-list');

        candidatesList.innerHTML = candidates.map((candidate, index) => `
            <div class="border-2 rounded-xl p-4 bg-gradient-to-r from-gray-50 to-white">
                <div class="flex justify-between items-start mb-3">
                    <h4 class="font-bold text-gray-900">Candidate ${index + 1}</h4>
                    <button type="button" onclick="removeCandidateFromForm(this)" class="text-red-600 hover:text-red-800 hover:bg-red-50 p-1 rounded">
                        <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path>
                        </svg>
                    </button>
                </div>
                <input type="hidden" class="candidate-id" value="${candidate.id}">
                <input type="text" class="candidate-name w-full px-4 py-2 border border-gray-300 rounded-lg mb-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Candidate Name" value="${candidate.name}" required>
                <input type="text" class="candidate-party w-full px-4 py-2 border border-gray-300 rounded-lg mb-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Party Affiliation (Optional)" value="${candidate.partyAffiliation || ''}">
                <textarea class="candidate-bio w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Biography (Optional)" rows="2">${candidate.bio || ''}</textarea>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error loading candidates:', error);
    }
}

async function loadVotersForForm(electionId) {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.voters(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const voters = await response.json();
        setSelectedVoters(voters.map(v => ({
            id: v.user.id,
            fullName: v.user.fullName,
            email: v.user.email
        })));

        updateVoterSelectionDisplay();

    } catch (error) {
        console.error('Error loading voters:', error);
    }
}

export function addCandidateField() {
    const candidatesList = document.getElementById('candidates-form-list');
    const candidateCount = candidatesList.children.length + 1;

    const candidateDiv = document.createElement('div');
    candidateDiv.className = 'border-2 rounded-xl p-4 bg-gradient-to-r from-gray-50 to-white';
    candidateDiv.innerHTML = `
        <div class="flex justify-between items-start mb-3">
            <h4 class="font-bold text-gray-900">Candidate ${candidateCount}</h4>
            <button type="button" onclick="removeCandidateFromForm(this)" class="text-red-600 hover:text-red-800 hover:bg-red-50 p-1 rounded">
                <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path>
                </svg>
            </button>
        </div>
        <input type="text" class="candidate-name w-full px-4 py-2 border border-gray-300 rounded-lg mb-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Candidate Name" required>
        <input type="text" class="candidate-party w-full px-4 py-2 border border-gray-300 rounded-lg mb-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Party Affiliation (Optional)">
        <textarea class="candidate-bio w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Biography (Optional)" rows="2"></textarea>
    `;

    candidatesList.appendChild(candidateDiv);
}

export function removeCandidateFromForm(button) {
    button.closest('.border-2').remove();

    // Update candidate numbers
    const candidates = document.querySelectorAll('#candidates-form-list > div');
    candidates.forEach((div, index) => {
        div.querySelector('h4').textContent = `Candidate ${index + 1}`;
    });
}

export async function searchVoters() {
    const searchTerm = document.getElementById('voter-search-input').value.trim();

    if (searchTerm.length < 2) {
        document.getElementById('voter-search-results').innerHTML = '';
        return;
    }

    const token = getAuthToken();

    try {
        const response = await fetch(`${ENDPOINTS.USERS.SEARCH}?q=${encodeURIComponent(searchTerm)}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const users = await response.json();
        const resultsContainer = document.getElementById('voter-search-results');

        resultsContainer.innerHTML = users
            .filter(user => !getSelectedVoters().find(v => v.id === user.id))
            .map(user => `
                <div class="p-3 hover:bg-gray-50 cursor-pointer border-b flex justify-between items-center"
                     onclick="addVoterToSelection('${user.id}', '${user.fullName}', '${user.email}')">
                    <div>
                        <div class="font-semibold text-gray-900">${user.fullName}</div>
                        <div class="text-sm text-gray-500">${user.email}</div>
                    </div>
                    <button class="text-indigo-600 hover:text-indigo-800 font-semibold text-sm">
                        + Add
                    </button>
                </div>
            `).join('');

    } catch (error) {
        console.error('Error searching voters:', error);
    }
}

export function addVoterToSelection(userId, fullName, email) {
    addSelectedVoter({ id: userId, fullName, email });
    updateVoterSelectionDisplay();
    document.getElementById('voter-search-input').value = '';
    document.getElementById('voter-search-results').innerHTML = '';
}

export function removeVoterFromSelection(voterId) {
    removeSelectedVoter(voterId);
    updateVoterSelectionDisplay();
}

function updateVoterSelectionDisplay() {
    const container = document.getElementById('selected-voters-list');
    const voters = getSelectedVoters();
    const makePublicCheckbox = document.getElementById('election-public');

    if (makePublicCheckbox && makePublicCheckbox.checked) {
        container.innerHTML = '<p class="text-sm text-gray-600">Election is open to all users</p>';
        return;
    }

    if (voters.length === 0) {
        container.innerHTML = '<p class="text-sm text-gray-500">No specific voters selected. Election will be open to all users.</p>';
        return;
    }

    container.innerHTML = voters.map(voter => `
        <div class="flex items-center justify-between p-3 bg-indigo-50 rounded-lg">
            <div>
                <div class="font-semibold text-gray-900">${voter.fullName}</div>
                <div class="text-sm text-gray-600">${voter.email}</div>
            </div>
            <button type="button" onclick="removeVoterFromSelection('${voter.id}')" class="text-red-600 hover:text-red-800">
                <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"></path>
                </svg>
            </button>
        </div>
    `).join('');
}

export async function handleElectionSubmit(event) {
    event.preventDefault();

    const token = getAuthToken();
    const title = document.getElementById('election-title-input').value;
    const description = document.getElementById('election-description-input').value;
    const startDate = document.getElementById('election-start-date').value;
    const endDate = document.getElementById('election-end-date').value;
    const isPublic = document.getElementById('election-public')?.checked || false;

    const candidateElements = document.querySelectorAll('#candidates-form-list > div');
    const candidates = Array.from(candidateElements).map(el => ({
        id: el.querySelector('.candidate-id')?.value || null,
        name: el.querySelector('.candidate-name').value,
        partyAffiliation: el.querySelector('.candidate-party').value,
        bio: el.querySelector('.candidate-bio').value
    }));

    if (candidates.length < 2) {
        alert('Please add at least 2 candidates');
        return;
    }

    const electionData = {
        title,
        description,
        startDate: new Date(startDate).toISOString(),
        endDate: new Date(endDate).toISOString(),
        candidates,
        isPublic: isPublic,
        voterIds: isPublic ? [] : getSelectedVoters().map(v => v.id)
    };

    try {
        let response;
        if (getEditingElectionId()) {
            response = await fetch(ENDPOINTS.ELECTIONS.byId(getEditingElectionId()), {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(electionData)
            });
        } else {
            response = await fetch(ENDPOINTS.ELECTIONS.BASE, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(electionData)
            });
        }

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Failed to save election');
            return;
        }

        alert(getEditingElectionId() ? 'Election updated successfully!' : 'Election created successfully!');
        showAdminDashboard();

    } catch (error) {
        console.error('Error saving election:', error);
        alert('Network error. Please try again.');
    }
}

export async function deleteElection(electionId) {
    if (!confirm('Are you sure you want to delete this election? This action cannot be undone.')) {
        return;
    }

    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.byId(electionId), {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Failed to delete election');
            return;
        }

        alert('Election deleted successfully!');
        loadAdminStats();
        loadAllElections();

    } catch (error) {
        console.error('Error deleting election:', error);
        alert('Network error. Please try again.');
    }
}

export async function viewElectionResults(electionId) {

    const token = getAuthToken();

    try {
        const electionResponse = await fetch(ENDPOINTS.ELECTIONS.byId(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!electionResponse.ok) {
            alert('Failed to load election details');
            return;
        }

        const election = await electionResponse.json();

        const resultsResponse = await fetch(ENDPOINTS.ELECTIONS.results(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!resultsResponse.ok) {
            const error = await resultsResponse.json();
            alert(error.message || 'Failed to load results');
            return;
        }

        const resultsData = await resultsResponse.json();
        const results = resultsData.data || resultsData;

        console.log('Results:', results);
        console.log('Election:', election); // ← ADD THIS LINE

        hideAllPages();
        document.getElementById('election-results-page').classList.remove('hidden');

        document.getElementById('results-title').textContent = election.title;
        document.getElementById('results-description').textContent = election.description || 'No description';

        const totalVotes = results.reduce((sum, result) => sum + (result.voteCount || 0), 0);

        // ============================================
        // ADD THESE LINES - Check if can publish
        // ============================================
        const canPublish = election.status === 'CLOSED' && !election.resultsPublished;
        const isPublished = election.resultsPublished;

        console.log('Can publish?', canPublish);
        console.log('Is published?', isPublished);
        // ============================================

        const resultsContainer = document.getElementById('results-container');

        resultsContainer.innerHTML = `
            <!-- ============================================ -->
            <!-- ADD THIS STATUS BANNER SECTION -->
            <!-- ============================================ -->
            ${isPublished ? `
                <div class="mb-6 bg-green-50 border-l-4 border-green-500 p-4 rounded">
                    <div class="flex items-center">
                        <svg class="h-6 w-6 text-green-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                        </svg>
                        <div>
                            <p class="font-semibold text-green-800">Results Published</p>
                            <p class="text-sm text-green-700">These results are now visible to all voters.</p>
                        </div>
                    </div>
                </div>
            ` : `
                <div class="mb-6 bg-yellow-50 border-l-4 border-yellow-500 p-4 rounded">
                    <div class="flex items-center">
                        <svg class="h-6 w-6 text-yellow-500 mr-3" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
                        </svg>
                        <div>
                            <p class="font-semibold text-yellow-800">Results Not Published</p>
                            <p class="text-sm text-yellow-700">Only admins can see these results. Voters cannot see them yet.</p>
                        </div>
                    </div>
                </div>
            `}
            <!-- ============================================ -->
            <!-- END STATUS BANNER -->
            <!-- ============================================ -->
            
            <!-- Total Votes Card (YOUR EXISTING CODE - NO CHANGE) -->
            <div class="mb-8">
                <div class="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-2xl p-6 text-white shadow-xl">
                    <div class="flex justify-between items-center">
                        <h3 class="text-2xl font-bold">Total Votes Cast</h3>
                        <span class="text-5xl font-bold">${totalVotes}</span>
                    </div>
                </div>
            </div>
            
            <!-- Candidates Results (YOUR EXISTING CODE - NO CHANGE) -->
            <div class="space-y-4">
                ${results.map((result) => {
            const isWinner = result.rank === 1 && result.voteCount > 0;
            return `
                        <div class="border-2 ${isWinner ? 'border-yellow-400 bg-yellow-50' : 'border-gray-200'} rounded-xl p-6 bg-white shadow-md hover:shadow-lg transition-all">
                            ${isWinner ? `
                                <div class="flex items-center mb-2">
                                    <svg class="h-6 w-6 text-yellow-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"></path>
                                    </svg>
                                    <span class="text-yellow-600 font-bold">Winner</span>
                                </div>
                            ` : ''}
                            <div class="flex justify-between items-center mb-3">
                                <div>
                                    <h4 class="font-bold text-xl text-gray-900">${result.candidateName}</h4>
                                    ${result.partyAffiliation ? `<p class="text-sm text-gray-600 mt-1">${result.partyAffiliation}</p>` : ''}
                                </div>
                                <div class="text-right">
                                    <div class="text-3xl font-bold text-indigo-600">${result.voteCount}</div>
                                    <div class="text-sm text-gray-500 font-semibold">${result.percentage.toFixed(1)}%</div>
                                </div>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-4 mt-4">
                                <div class="bg-gradient-to-r from-indigo-500 to-indigo-700 h-4 rounded-full transition-all duration-500" 
                                     style="width: ${result.percentage}%">
                                </div>
                            </div>
                        </div>
                    `;
        }).join('')}
            </div>
            
            <!-- ============================================ -->
            <!-- REPLACE YOUR "Back Button" SECTION WITH THIS -->
            <!-- ============================================ -->
            <div class="mt-8 flex gap-4">
                ${canPublish ? `
                    <button onclick="publishElectionResults('${electionId}')" 
                            class="px-6 py-3 bg-green-600 text-white rounded-lg font-semibold hover:bg-green-700 shadow-lg hover:shadow-xl transition-all">
                        📢 Publish Results to Public
                    </button>
                ` : ''}
                
                <button onclick="showAdminDashboard()" 
                        class="px-6 py-3 bg-gray-500 text-white rounded-lg hover:bg-gray-600">
                    Back to Dashboard
                </button>
            </div>
        `;

    } catch (error) {
        console.error('Error loading results:', error);
        alert('Network error. Failed to load election results.');
    }
}

export async function publishElectionResults(electionId) {
    if (!confirm('Are you sure you want to publish these results? All voters will be able to see them.')) {
        return;
    }

    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.publishResults(electionId), {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Failed to publish results');
            return;
        }

        alert('✅ Results published successfully! All voters can now see the results.');

        // Reload results page to show updated status
        viewElectionResults(electionId);

    } catch (error) {
        console.error('Error publishing results:', error);
        alert('Network error. Please try again.');
    }
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

export async function activateElection(electionId) {
    if (!confirm('Are you sure you want to activate this election? It will immediately open for voting.')) {
        return;
    }

    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.activate(electionId), {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Failed to activate election');
            return;
        }

        alert('Election activated successfully! Voters can now cast their votes.');

        // Reload the elections list
        loadAdminStats();
        loadAllElections();

    } catch (error) {
        console.error('Error activating election:', error);
        alert('Network error. Please try again.');
    }
}

window.activateElection = activateElection;
window.showCreateElection = showCreateElection;
window.editElection = editElection;
window.deleteElection = deleteElection;
window.viewElectionResults = viewElectionResults;
window.addCandidateField = addCandidateField;
window.removeCandidateFromForm = removeCandidateFromForm;
window.handleElectionSubmit = handleElectionSubmit;
window.searchVoters = searchVoters;
window.addVoterToSelection = addVoterToSelection;
window.removeVoterFromSelection = removeVoterFromSelection;
window.updateVoterSelectionDisplay = updateVoterSelectionDisplay;
window.publishElectionResults = publishElectionResults;