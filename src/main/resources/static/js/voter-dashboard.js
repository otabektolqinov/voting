import { ENDPOINTS } from './config.js';
import { getAuthToken, setSelectedCandidateId, setCurrentElectionId, getCurrentElectionId, getSelectedCandidateId } from './state.js';
import { showVoterDashboard } from './navigation.js';
import { formatDate, sanitize } from './utils.js';

let currentFilter = 'all';

export async function loadElections() {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.MY_ELECTIONS, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const elections = await response.json();

        // Filter elections based on current tab
        let filteredElections = elections;
        console.log("RAW elections:", elections);
        if (currentFilter === 'active') {
            filteredElections = elections.filter(e => e.status === 'ACTIVE');
        } else if (currentFilter === 'closed') {
            filteredElections = elections.filter(e => e.status === 'CLOSED');
        }

        const grid = document.getElementById('elections-grid');
        const noElections = document.getElementById('no-elections');

        // Render tabs
        renderTabs(elections);

        if (filteredElections.length === 0) {
            grid.innerHTML = '';
            noElections.classList.remove('hidden');
            noElections.querySelector('p').textContent =
                currentFilter === 'active' ? 'No active elections' :
                    currentFilter === 'closed' ? 'No closed elections' :
                        'No elections available';
            return;
        }

        noElections.classList.add('hidden');
        grid.innerHTML = filteredElections.map(election => renderElectionCard(election)).join('');

    } catch (error) {
        console.error('Error loading elections:', error);
    }
}


function renderTabs(elections) {
    const activeCount = elections.filter(e => e.status === 'ACTIVE').length;
    const closedCount = elections.filter(e => e.status === 'CLOSED').length;

    const tabsContainer = document.getElementById('election-tabs');
    if (!tabsContainer) return;

    tabsContainer.innerHTML = `
        <div class="flex space-x-1 bg-gray-100 p-1 rounded-lg mb-6">
            <button onclick="filterElections('all')" 
                    class="flex-1 px-4 py-2 rounded-md font-semibold transition-all ${currentFilter === 'all' ? 'bg-white text-indigo-600 shadow' : 'text-gray-600 hover:text-gray-900'}">
                All (${elections.length})
            </button>
            <button onclick="filterElections('active')" 
                    class="flex-1 px-4 py-2 rounded-md font-semibold transition-all ${currentFilter === 'active' ? 'bg-white text-indigo-600 shadow' : 'text-gray-600 hover:text-gray-900'}">
                Active (${activeCount})
            </button>
            <button onclick="filterElections('closed')" 
                    class="flex-1 px-4 py-2 rounded-md font-semibold transition-all ${currentFilter === 'closed' ? 'bg-white text-indigo-600 shadow' : 'text-gray-600 hover:text-gray-900'}">
                Closed (${closedCount})
            </button>
        </div>
    `;
}

/**
 * Filter elections by tab
 */
export function filterElections(filter) {
    currentFilter = filter;
    loadElections();
}

/**
 * Render individual election card
 */
function renderElectionCard(election) {
    // Determine what action to show
    let actionButton = '';

    if (election.status === 'ACTIVE') {
        if (election.hasVoted) {
            // Already voted in active election
            actionButton = `
                <div class="mt-4 flex items-center text-green-600 font-semibold">
                    <svg class="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
                    </svg>
                    You have voted
                </div>
            `;
        } else {
            // Can vote
            actionButton = `
                <div class="mt-4 flex items-center text-indigo-600 font-semibold">
                    <svg class="h-5 w-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                    Vote now
                </div>
            `;
        }
    } else if (election.status === 'CLOSED') {
        if (election.hasVoted) {
            // Voted and election closed
            if (election.resultsPublished) {
                // Can view results
                actionButton = `
                    <div class="mt-4">
                        <div class="flex items-center text-green-600 font-semibold mb-2">
                            <svg class="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
                            </svg>
                            You voted
                        </div>
                        <button onclick="viewElectionResults('${election.id}')" 
                                class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-semibold text-sm">
                            📊 View Results
                        </button>
                    </div>
                `;
            } else {
                // Results not published yet
                actionButton = `
                    <div class="mt-4">
                        <div class="flex items-center text-green-600 font-semibold mb-2">
                            <svg class="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
                            </svg>
                            You voted
                        </div>
                        <p class="text-sm text-gray-500">Results coming soon...</p>
                    </div>
                `;
            }
        } else {
            // Didn't vote
            actionButton = `
                <div class="mt-4 text-gray-500 font-semibold">
                    Election closed
                </div>
            `;
        }
    } else {
        // UPCOMING or DRAFT
        actionButton = `
            <div class="mt-4 text-blue-600 font-semibold">
                Coming soon
            </div>
        `;
    }

    return `
        <div class="bg-white rounded-xl shadow-md p-6 hover:shadow-xl transition-all cursor-pointer border-2 border-transparent hover:border-indigo-500"
             onclick="${election.status === 'ACTIVE' && !election.hasVoted ? `viewElection('${election.id}')` : ''}">
            <div class="flex justify-between items-start mb-4">
                <h3 class="text-xl font-bold text-gray-900">${sanitize(election.title)}</h3>
                <span class="px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(election.status)}">
                    ${election.status}
                </span>
            </div>
            <p class="text-gray-600 mb-4 line-clamp-2">${sanitize(election.description) || 'No description'}</p>
            <div class="flex items-center text-sm text-gray-500 space-x-2">
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                </svg>
                <span>${formatDate(election.startDate)} - ${formatDate(election.endDate)}</span>
            </div>
            ${actionButton}
        </div>
    `;
}


export async function viewElectionResults(electionId) {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.results(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            alert(error.message || 'Results not available yet');
            return;
        }

        const resultsData = await response.json();
        const results = resultsData.data || resultsData;

        const electionResponse = await fetch(ENDPOINTS.ELECTIONS.byId(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const election = await electionResponse.json();

        showResultsModal(election, results);

    } catch (error) {
        console.error('Error loading results:', error);
        alert('Failed to load results');
    }
}


function showResultsModal(election, results) {
    const totalVotes = results.reduce((sum, r) => sum + (r.voteCount || 0), 0);

    const modal = document.createElement('div');
    modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4';
    modal.onclick = (e) => {
        if (e.target === modal) modal.remove();
    };

    modal.innerHTML = `
        <div class="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto p-8" onclick="event.stopPropagation()">
            <div class="flex justify-between items-start mb-6">
                <div>
                    <h2 class="text-3xl font-bold text-gray-900">${sanitize(election.title)}</h2>
                    <p class="text-gray-600 mt-2">Election Results</p>
                </div>
                <button onclick="this.closest('.fixed').remove()" 
                        class="text-gray-400 hover:text-gray-600 text-2xl">
                    ×
                </button>
            </div>

            <!-- Total Votes -->
            <div class="mb-6">
                <div class="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-xl p-6 text-white">
                    <div class="flex justify-between items-center">
                        <h3 class="text-xl font-bold">Total Votes</h3>
                        <span class="text-4xl font-bold">${totalVotes}</span>
                    </div>
                </div>
            </div>

            <!-- Results -->
            <div class="space-y-4">
                ${results.map(result => {
        const isWinner = result.rank === 1 && result.voteCount > 0;
        return `
                        <div class="border-2 ${isWinner ? 'border-yellow-400 bg-yellow-50' : 'border-gray-200'} rounded-xl p-4">
                            ${isWinner ? `
                                <div class="flex items-center mb-2 text-yellow-600">
                                    <svg class="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"></path>
                                    </svg>
                                    <span class="font-bold">Winner</span>
                                </div>
                            ` : ''}
                            <div class="flex justify-between items-center">
                                <div>
                                    <h4 class="font-bold text-lg">${sanitize(result.candidateName)}</h4>
                                    ${result.partyAffiliation ? `<p class="text-sm text-gray-600">${sanitize(result.partyAffiliation)}</p>` : ''}
                                </div>
                                <div class="text-right">
                                    <div class="text-2xl font-bold text-indigo-600">${result.voteCount}</div>
                                    <div class="text-sm text-gray-500">${result.percentage.toFixed(1)}%</div>
                                </div>
                            </div>
                            <div class="w-full bg-gray-200 rounded-full h-3 mt-3">
                                <div class="bg-gradient-to-r from-indigo-500 to-indigo-700 h-3 rounded-full" 
                                     style="width: ${result.percentage}%"></div>
                            </div>
                        </div>
                    `;
    }).join('')}
            </div>

            <button onclick="this.closest('.fixed').remove(); window.currentUserRole === 'ADMIN' ? window.showAdminDashboard() : window.showVoterDashboard()" 
                class="mt-6 w-full px-6 py-3 bg-gray-500 text-white rounded-lg hover:bg-gray-600 font-semibold">
                Close
            </button>
        </div>
    `;

    document.body.appendChild(modal);
}

function getStatusColor(status) {
    switch (status) {
        case 'ACTIVE': return 'bg-green-100 text-green-800';
        case 'UPCOMING': return 'bg-blue-100 text-blue-800';
        case 'CLOSED': return 'bg-gray-100 text-gray-800';
        default: return 'bg-gray-100 text-gray-800';
    }
}

export async function viewElection(electionId) {
    setCurrentElectionId(electionId);
    setSelectedCandidateId(null);
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.ELECTIONS.byId(electionId), {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            throw new Error('Failed to load election');
        }

        const election = await response.json();

        hideAllPages();
        document.getElementById('election-details-page').classList.remove('hidden');

        document.getElementById('election-title').textContent = sanitize(election.title);
        document.getElementById('election-description').textContent = sanitize(election.description) || 'No description';
        document.getElementById('election-dates').innerHTML = `
            <svg class="h-5 w-5 inline mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
            </svg>
            ${formatDate(election.startDate)} - ${formatDate(election.endDate)}
        `;

        // Use candidates from election object (not a separate API call)
        const candidates = election.candidates || [];

        const candidatesList = document.getElementById('candidates-list');

        // Check if candidates exist
        if (candidates.length === 0) {
            candidatesList.innerHTML = `
                <div class="text-center py-8 text-gray-500">
                    <p class="text-lg">No candidates available for this election.</p>
                </div>
            `;
        } else {
            candidatesList.innerHTML = candidates.map(candidate => `
                <label class="block">
                    <div class="border-2 rounded-xl p-6 hover:border-indigo-500 cursor-pointer transition-all bg-white hover:shadow-lg"
                         onclick="selectCandidate('${candidate.id}')">
                        <div class="flex items-start">
                            <input type="radio" name="candidate" value="${candidate.id}"
                                   class="mt-1 mr-4 h-5 w-5 text-indigo-600 focus:ring-2 focus:ring-indigo-500">
                            <div class="flex-1">
                                <h4 class="font-bold text-xl text-gray-900">${sanitize(candidate.name)}</h4>
                                ${candidate.partyAffiliation ? `<p class="text-sm text-indigo-600 font-semibold mt-1">${sanitize(candidate.partyAffiliation)}</p>` : ''}
                                ${candidate.bio ? `<p class="text-gray-600 mt-2">${sanitize(candidate.bio)}</p>` : ''}
                            </div>
                        </div>
                    </div>
                </label>
            `).join('');
        }

        const voteButton = document.getElementById('vote-button');
        const voteSuccess = document.getElementById('vote-success');

        if (election.hasVoted || election.status !== 'ACTIVE') {
            voteButton.classList.add('hidden');
            if (election.hasVoted) {
                voteSuccess.classList.remove('hidden');
                candidatesList.style.opacity = '0.6';
                candidatesList.style.pointerEvents = 'none';
            }
        } else {
            voteButton.classList.remove('hidden');
            voteSuccess.classList.add('hidden');
            candidatesList.style.opacity = '1';
            candidatesList.style.pointerEvents = 'auto';
        }

    } catch (error) {
        console.error('Error loading election details:', error);
        alert('Failed to load election details. Please try again.');
    }
}

export function selectCandidate(candidateId) {
    setSelectedCandidateId(candidateId);
    const radio = document.querySelector(`input[value="${candidateId}"]`);
    if (radio) radio.checked = true;
}

export function confirmVote() {
    if (!getSelectedCandidateId()) {
        alert('Please select a candidate');
        return;
    }

    requestOtp();
}

async function requestOtp() {
    const token = getAuthToken();

    // Show modal immediately with loading state
    showOtpModal(async (otpValue) => {
        await castVote(otpValue);
    });

    // Send OTP in background
    try {
        const response = await fetch('/api/otp/send', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) {
            closeOtpModal();
            alert('Failed to send OTP. Please try again.');
        }
    } catch (error) {
        closeOtpModal();
        alert('Failed to send OTP. Please try again.');
    }
}

async function castVote(otp) {
    const token = getAuthToken();

    try {
        const response = await fetch(ENDPOINTS.VOTES, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                electionId: getCurrentElectionId(),
                candidateId: getSelectedCandidateId(),
                otp: otp
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to cast vote'); // ← throw, not alert
        }

        closeOtpModal();
        document.getElementById('vote-button').classList.add('hidden');
        document.getElementById('candidates-list').style.pointerEvents = 'none';
        document.getElementById('candidates-list').style.opacity = '0.6';
        document.getElementById('vote-success').classList.remove('hidden');

        setTimeout(() => {
            showVoterDashboard();
        }, 2000);


    } catch (error) {
        console.error('Error casting vote:', error);
        alert('Network error. Please try again.');
    }
}

function showOtpModal(onSubmit) {
    const modal = document.createElement('div');
    modal.id = 'otp-modal';
    modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
    modal.innerHTML = `
        <div class="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-sm mx-4">
            <div class="text-center mb-6">
                <div class="text-5xl mb-3">🔐</div>
                <h2 class="text-2xl font-bold text-gray-900">Verify Your Vote</h2>
                <p class="text-gray-500 mt-2 text-sm">We're sending a 6-digit OTP to your email...</p>
            </div>

            <div class="mb-4">
                <input 
                    id="otp-input"
                    type="text" 
                    maxlength="6"
                    placeholder="Enter 6-digit OTP"
                    class="w-full text-center text-2xl font-bold tracking-widest border-2 border-gray-300 rounded-xl px-4 py-3 focus:outline-none focus:border-indigo-500 transition-colors"
                />
            </div>

            <p id="otp-error" class="text-red-500 text-sm text-center mb-4 hidden"></p>

            <button 
                id="otp-submit-btn"
                class="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 rounded-xl transition-colors"
                onclick="handleOtpSubmit()">
                Confirm Vote
            </button>

            <button 
                class="w-full mt-3 text-gray-500 hover:text-gray-700 font-semibold py-2 transition-colors"
                onclick="closeOtpModal()">
                Cancel
            </button>

            <p class="text-center text-xs text-gray-400 mt-4">OTP expires in 1 minute</p>
        </div>
    `;
    document.body.appendChild(modal);

    // Auto focus input
    setTimeout(() => document.getElementById('otp-input')?.focus(), 100);

    // Store callback
    window._otpCallback = onSubmit;

    // Allow Enter key to submit
    document.getElementById('otp-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') handleOtpSubmit();
    });
}

window.handleOtpSubmit = async function() {
    const otpValue = document.getElementById('otp-input')?.value?.trim();
    const errorEl = document.getElementById('otp-error');
    const btn = document.getElementById('otp-submit-btn');

    if (!otpValue || otpValue.length !== 6) {
        errorEl.textContent = 'Please enter the 6-digit OTP';
        errorEl.classList.remove('hidden');
        return;
    }

    // Show loading state
    btn.textContent = 'Verifying...';
    btn.disabled = true;
    errorEl.classList.add('hidden');

    try {
        await window._otpCallback(otpValue);
        closeOtpModal();
    } catch (error) {
        btn.textContent = 'Confirm Vote';
        btn.disabled = false;
        errorEl.textContent = error.message || 'Invalid OTP. Please try again.';
        errorEl.classList.remove('hidden');
    }
}

window.closeOtpModal = function() {
    document.getElementById('otp-modal')?.remove();
    window._otpCallback = null;
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

window.viewElection = viewElection;
window.selectCandidate = selectCandidate;
window.confirmVote = confirmVote;
window.filterElections = filterElections;
window.viewElectionResults = viewElectionResults;