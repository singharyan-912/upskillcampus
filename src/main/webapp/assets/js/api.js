/**
 * api.js — Base fetch wrapper and session utilities.
 *
 * All other JS files import helpers from here.
 * Centralises API base path, error handling, and session reads.
 */

// ────────────────────────────────────────────────────────
// CONFIG
// ────────────────────────────────────────────────────────

/**
 * Detect the application context path dynamically.
 * Works whether the WAR is deployed as root or as /food_delivery_application-1.0-SNAPSHOT/
 */
function getContextPath() {
    // pathname looks like /food_delivery_application-1.0-SNAPSHOT/pages/login.html
    const parts = window.location.pathname.split('/');
    // parts[0] = '' (before leading slash)
    // parts[1] = context path (e.g. 'food_delivery_application-1.0-SNAPSHOT') OR 'pages'
    // If the context root is just '/', parts[1] will be 'pages' or 'index.html'
    if (parts[1] === 'pages' || parts[1] === 'assets' || parts[1].endsWith('.html') || parts[1] === '') {
        return ''; // deployed at root
    }
    return '/' + parts[1];
}

const CONTEXT = getContextPath();
const API_BASE = CONTEXT + '/api';

// ────────────────────────────────────────────────────────
// CORE FETCH WRAPPER
// ────────────────────────────────────────────────────────

/**
 * Perform an API call.
 * Always sends/receives JSON.
 * Returns the parsed response object (with success, message, data fields).
 * On network error, returns { success: false, message: '...' }.
 *
 * @param {string} endpoint  — path after /api, e.g. '/auth/login'
 * @param {string} method    — 'GET' | 'POST' | 'PUT' | 'DELETE'
 * @param {object} [body]    — request body (will be JSON-stringified)
 * @returns {Promise<object>}
 */
async function apiCall(endpoint, method = 'GET', body = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        },
        credentials: 'same-origin', // send session cookie automatically
    };

    if (body !== null && method !== 'GET') {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(API_BASE + endpoint, options);
        const json = await response.json();
        // Attach HTTP status for callers that need it
        json._status = response.status;
        return json;
    } catch (error) {
        console.error('[API] Network error on', method, endpoint, error);
        return {
            success: false,
            message: 'Network error. Please check your connection.',
            _status: 0,
        };
    }
}

// ────────────────────────────────────────────────────────
// SHORTHAND HELPERS
// ────────────────────────────────────────────────────────

const api = {
    get:    (endpoint)         => apiCall(endpoint, 'GET'),
    post:   (endpoint, body)   => apiCall(endpoint, 'POST', body),
    put:    (endpoint, body)   => apiCall(endpoint, 'PUT', body),
    delete: (endpoint)         => apiCall(endpoint, 'DELETE'),
    /** DELETE with query params (e.g. /api/cart/remove?cartItemId=5) */
    deleteWithParam: (endpoint, params) => {
        const qs = new URLSearchParams(params).toString();
        return apiCall(endpoint + '?' + qs, 'DELETE');
    },
};

// ────────────────────────────────────────────────────────
// SESSION HELPERS  (localStorage mirror for fast UI reads)
// ────────────────────────────────────────────────────────

const Session = {
    KEY: 'fda_user',

    save(user) {
        localStorage.setItem(this.KEY, JSON.stringify(user));
    },

    get() {
        try {
            return JSON.parse(localStorage.getItem(this.KEY));
        } catch {
            return null;
        }
    },

    clear() {
        localStorage.removeItem(this.KEY);
    },

    isLoggedIn() {
        return this.get() !== null;
    },

    getRole() {
        const u = this.get();
        return u ? u.role : null;
    },

    getUserId() {
        const u = this.get();
        return u ? u.userId : null;
    },

    getName() {
        const u = this.get();
        return u ? u.name : null;
    },
};

// ────────────────────────────────────────────────────────
// TOAST NOTIFICATION SYSTEM
// ────────────────────────────────────────────────────────

(function initToastContainer() {
    if (!document.getElementById('toast-container')) {
        const container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
})();

/**
 * Show a toast notification.
 * @param {string} message
 * @param {'success'|'error'|'info'} type
 * @param {number} duration  ms before auto-dismiss (default 3500)
 */
function showToast(message, type = 'info', duration = 3500) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const icons = { success: '✅', error: '❌', info: 'ℹ️' };

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || 'ℹ️'}</span>
        <span>${message}</span>
    `;

    container.appendChild(toast);

    const remove = () => {
        toast.classList.add('hiding');
        setTimeout(() => toast.remove(), 350);
    };

    setTimeout(remove, duration);
    toast.addEventListener('click', remove);
}

// ────────────────────────────────────────────────────────
// AUTH GUARD  — call on any protected page's <script>
// ────────────────────────────────────────────────────────

/**
 * Redirect to login if user is not logged in.
 * Optionally restrict to specific roles.
 * @param {string[]} [allowedRoles]
 */
function requireAuth(allowedRoles = []) {
    const user = Session.get();
    if (!user) {
        window.location.href = CONTEXT + '/pages/login.html';
        return false;
    }
    if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
        showToast('Access denied. Redirecting...', 'error');
        setTimeout(() => redirectByRole(user.role), 1500);
        return false;
    }
    return true;
}

/**
 * Redirect to the correct dashboard based on role.
 */
function redirectByRole(role) {
    const base = CONTEXT + '/pages/';
    const map = {
        CUSTOMER:         base + 'restaurants.html',
        RESTAURANT:       base + 'restaurant-dashboard.html',
        DELIVERY_PARTNER: base + 'delivery-dashboard.html',
        ADMIN:            base + 'admin-dashboard.html',
    };
    window.location.href = map[role] || base + 'login.html';
}

// ────────────────────────────────────────────────────────
// NAV HELPERS
// ────────────────────────────────────────────────────────

/**
 * Populate the shared nav user info and wire up the logout button.
 * Call this on any page that includes the nav.
 */
function initNav() {
    const user = Session.get();

    // Scroll shadow effect
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        window.addEventListener('scroll', () => {
            navbar.classList.toggle('scrolled', window.scrollY > 10);
        });
    }

    // User avatar / name
    const avatarEl  = document.getElementById('nav-avatar');
    const nameEl    = document.getElementById('nav-user-name');
    const logoutBtn = document.getElementById('nav-logout-btn');

    if (user) {
        if (avatarEl)  avatarEl.textContent  = user.name ? user.name.charAt(0).toUpperCase() : '?';
        if (nameEl)    nameEl.textContent    = user.name || 'User';
        if (logoutBtn) logoutBtn.style.display = 'inline-flex';
    } else {
        if (logoutBtn) logoutBtn.style.display = 'none';
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            await api.post('/auth/logout');
            Session.clear();
            window.location.href = CONTEXT + '/pages/login.html';
        });
    }
}

// ────────────────────────────────────────────────────────
// CURRENCY FORMATTER
// ────────────────────────────────────────────────────────

function formatCurrency(amount) {
    const num = parseFloat(amount);
    if (isNaN(num)) return '₹0.00';
    return '₹' + num.toFixed(2);
}

// ────────────────────────────────────────────────────────
// EXPORTS (available globally — no module bundler needed)
// ────────────────────────────────────────────────────────
// All functions above are global-scoped for plain HTML usage.
