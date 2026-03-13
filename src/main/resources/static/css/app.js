export const API_URL = "http://localhost:8080/api";

export function getAuthHeaders() {
    const token = localStorage.getItem("token");
    return {
        "Content-Type": "application/json",
        "Authorization": token ? `Bearer ${token}` : ""
    };
}

export async function apiRequest(url, options = {}) {
    const response = await fetch(`${API_URL}${url}`, options);

    if (response.status === 401) {
        localStorage.clear();
        window.location.reload();
        throw new Error("Session expired");
    }

    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Request failed");
    }

    return response.json();
}
