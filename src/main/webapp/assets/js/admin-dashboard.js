/**
 * admin-dashboard.js — Logic for the ADMIN role dashboard.
 *
 * Features:
 *  - Load + display platform stats (users, restaurants, orders, deliveries, revenue)
 *  - Four data tabs with searchable, sortable tables:
 *      Users · Restaurants · Orders · Deliveries
 *  - Client-side search + status filter on Orders tab
 *  - All data is read-only (admin has no write endpoints)
 */

// ─────────────────────────────────────────────────────────
//  RAW DATA STORE
// ─────────────────────────────────────────────────────────

const store = {
    users:         [],
    restaurants:   [],
    orders:        [],
    deliveries:    [],
};

// ─────────────────────────────────────────────────────────
//  UTILITY HELPERS
// ─────────────────────────────────────────────────────────

function fmtDate(ts) {
    if (!ts) return '—';
    try {
        return new Date(ts).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit', hour12: true,
        });
    } catch { return String(ts); }
}

function esc(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function badgeHTML(status) {
    if (!status) return '—';
    return `<span class="badge badge-${status}">${esc(status.replace(/_/g, ' '))}</span>`;
}

function emptyRow(cols, msg) {
    return `<tr><td colspan="${cols}" style="text-align:center; padding:2rem; color:var(--color-text-muted);">${msg}</td></tr>`;
}

// ─────────────────────────────────────────────────────────
//  TAB NAVIGATION
// ─────────────────────────────────────────────────────────

const tabLoaded = { users: false, restaurants: false, orders: false, deliveries: false };

function switchTab(targetTab) {
    document.querySelectorAll('.dashboard-tab').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === targetTab);
    });
    document.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.toggle('active', panel.id === `panel-${targetTab}`);
    });

    if (!tabLoaded[targetTab]) {
        tabLoaded[targetTab] = true;
        const loaders = {
            users:       loadUsers,
            restaurants: loadRestaurants,
            orders:      loadOrders,
            deliveries:  loadDeliveries,
        };
        if (loaders[targetTab]) loaders[targetTab]();
    }
}

// ─────────────────────────────────────────────────────────
//  STATS
// ─────────────────────────────────────────────────────────

async function loadStats() {
    const result = await api.get('/admin/stats');
    if (!result.success) {
        showToast('Could not load stats: ' + result.message, 'error');
        return;
    }
    const s = result.data;
    const setVal = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v; };
    setVal('stat-users',       s.totalUsers       ?? '—');
    setVal('stat-restaurants', s.totalRestaurants ?? '—');
    setVal('stat-orders',      s.totalOrders      ?? '—');
    setVal('stat-deliveries',  s.totalDeliveries  ?? '—');
    setVal('stat-revenue',     s.totalRevenue != null ? formatCurrency(s.totalRevenue) : '—');
}

// ─────────────────────────────────────────────────────────
//  USERS TAB
// ─────────────────────────────────────────────────────────

async function loadUsers() {
    const tbody = document.getElementById('users-tbody');
    tbody.innerHTML = `<tr><td colspan="5" class="dash-loading">⏳ Loading…</td></tr>`;

    const result = await api.get('/admin/users');
    if (!result.success) {
        tbody.innerHTML = emptyRow(5, '⚠️ ' + esc(result.message));
        return;
    }

    store.users = result.data || [];
    renderUsers();
}

function renderUsers() {
    const tbody   = document.getElementById('users-tbody');
    const query   = (document.getElementById('search-users')?.value || '').toLowerCase();
    const rows    = query
        ? store.users.filter(u =>
            (u.name  || '').toLowerCase().includes(query) ||
            (u.email || '').toLowerCase().includes(query))
        : store.users;

    if (rows.length === 0) {
        tbody.innerHTML = emptyRow(5, query ? 'No users match your search.' : 'No users found.');
        return;
    }

    tbody.innerHTML = rows.map(u => `
        <tr>
            <td><span style="color:var(--color-text-muted);font-size:0.8rem;">#${u.userId}</span></td>
            <td><strong>${esc(u.name)}</strong></td>
            <td>${esc(u.email)}</td>
            <td>${esc(u.phone) || '—'}</td>
            <td>${badgeHTML(u.role)}</td>
        </tr>`).join('');
}

// ─────────────────────────────────────────────────────────
//  RESTAURANTS TAB
// ─────────────────────────────────────────────────────────

async function loadRestaurants() {
    const tbody = document.getElementById('restaurants-tbody');
    tbody.innerHTML = `<tr><td colspan="7" class="dash-loading">⏳ Loading…</td></tr>`;

    const result = await api.get('/admin/restaurants');
    if (!result.success) {
        tbody.innerHTML = emptyRow(7, '⚠️ ' + esc(result.message));
        return;
    }

    store.restaurants = result.data || [];
    renderRestaurants();
}

function renderRestaurants() {
    const tbody = document.getElementById('restaurants-tbody');
    const query = (document.getElementById('search-restaurants')?.value || '').toLowerCase();
    const rows  = query
        ? store.restaurants.filter(r =>
            (r.name        || '').toLowerCase().includes(query) ||
            (r.cuisineType || '').toLowerCase().includes(query) ||
            (r.ownerName   || '').toLowerCase().includes(query))
        : store.restaurants;

    if (rows.length === 0) {
        tbody.innerHTML = emptyRow(7, query ? 'No restaurants match your search.' : 'No restaurants found.');
        return;
    }

    tbody.innerHTML = rows.map(r => `
        <tr>
            <td><span style="color:var(--color-text-muted);font-size:0.8rem;">#${r.restaurantId}</span></td>
            <td><strong>${esc(r.name)}</strong></td>
            <td>${esc(r.ownerName) || '—'}</td>
            <td>${esc(r.cuisineType) || '—'}</td>
            <td>${esc(r.phone) || '—'}</td>
            <td>${badgeHTML(r.status)}</td>
            <td style="font-size:0.8rem;">${fmtDate(r.createdAt)}</td>
        </tr>`).join('');
}

// ─────────────────────────────────────────────────────────
//  ORDERS TAB
// ─────────────────────────────────────────────────────────

async function loadOrders() {
    const tbody = document.getElementById('orders-tbody');
    tbody.innerHTML = `<tr><td colspan="7" class="dash-loading">⏳ Loading…</td></tr>`;

    const result = await api.get('/admin/orders');
    if (!result.success) {
        tbody.innerHTML = emptyRow(7, '⚠️ ' + esc(result.message));
        return;
    }

    store.orders = result.data || [];
    renderOrders();
}

function renderOrders() {
    const tbody       = document.getElementById('orders-tbody');
    const query       = (document.getElementById('search-orders')?.value || '').toLowerCase();
    const statusFilter= (document.getElementById('filter-order-status')?.value || '');

    let rows = store.orders;
    if (query) {
        rows = rows.filter(o =>
            (o.customerName   || '').toLowerCase().includes(query) ||
            (o.restaurantName || '').toLowerCase().includes(query));
    }
    if (statusFilter) {
        rows = rows.filter(o => o.status === statusFilter);
    }

    if (rows.length === 0) {
        tbody.innerHTML = emptyRow(7, 'No orders match your filters.');
        return;
    }

    tbody.innerHTML = rows.map(o => `
        <tr>
            <td><span style="color:var(--color-text-muted);font-size:0.8rem;">#${o.orderId}</span></td>
            <td>${esc(o.customerName) || '—'}</td>
            <td>${esc(o.restaurantName) || '—'}</td>
            <td style="font-weight:700;color:var(--color-primary);">${formatCurrency(o.totalAmount)}</td>
            <td>${badgeHTML(o.status)}</td>
            <td style="font-size:0.8rem; max-width:160px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${esc(o.deliveryAddress)}">${esc(o.deliveryAddress) || '—'}</td>
            <td style="font-size:0.8rem;">${fmtDate(o.createdAt)}</td>
        </tr>`).join('');
}

// ─────────────────────────────────────────────────────────
//  DELIVERIES TAB
// ─────────────────────────────────────────────────────────

async function loadDeliveries() {
    const tbody = document.getElementById('deliveries-tbody');
    tbody.innerHTML = `<tr><td colspan="8" class="dash-loading">⏳ Loading…</td></tr>`;

    const result = await api.get('/admin/deliveries');
    if (!result.success) {
        tbody.innerHTML = emptyRow(8, '⚠️ ' + esc(result.message));
        return;
    }

    store.deliveries = result.data || [];
    renderDeliveries();
}

function renderDeliveries() {
    const tbody = document.getElementById('deliveries-tbody');
    const query = (document.getElementById('search-deliveries')?.value || '').toLowerCase();
    const rows  = query
        ? store.deliveries.filter(d =>
            (d.partnerName    || '').toLowerCase().includes(query) ||
            (d.restaurantName || '').toLowerCase().includes(query))
        : store.deliveries;

    if (rows.length === 0) {
        tbody.innerHTML = emptyRow(8, query ? 'No deliveries match your search.' : 'No deliveries found.');
        return;
    }

    tbody.innerHTML = rows.map(d => `
        <tr>
            <td><span style="color:var(--color-text-muted);font-size:0.8rem;">#${d.deliveryId}</span></td>
            <td><span style="color:var(--color-text-muted);font-size:0.8rem;">#${d.orderId}</span></td>
            <td>${esc(d.partnerName) || '—'}</td>
            <td>${esc(d.restaurantName) || '—'}</td>
            <td style="font-weight:700;color:var(--color-primary);">${formatCurrency(d.totalAmount)}</td>
            <td>${badgeHTML(d.deliveryStatus)}</td>
            <td style="font-size:0.8rem;">${fmtDate(d.assignedTime)}</td>
            <td style="font-size:0.8rem;">${fmtDate(d.deliveredTime)}</td>
        </tr>`).join('');
}

// ─────────────────────────────────────────────────────────
//  INIT
// ─────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth(['ADMIN'])) return;
    initNav();

    // Set subtitle
    const name = Session.getName();
    const subtitleEl = document.getElementById('dash-subtitle');
    if (subtitleEl && name) subtitleEl.textContent = `Welcome, ${name}. Platform overview and management.`;

    // Load stats immediately; users tab is active by default
    loadStats();
    tabLoaded.users = true;
    loadUsers();

    // Tab switching
    document.querySelectorAll('.dashboard-tab').forEach(btn => {
        btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // Refresh stats button
    document.getElementById('btn-refresh-stats')?.addEventListener('click', loadStats);

    // Search inputs — debounced
    function debounce(fn, ms = 300) {
        let timer;
        return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), ms); };
    }

    document.getElementById('search-users')?.addEventListener('input', debounce(renderUsers));
    document.getElementById('search-restaurants')?.addEventListener('input', debounce(renderRestaurants));
    document.getElementById('search-orders')?.addEventListener('input', debounce(renderOrders));
    document.getElementById('search-deliveries')?.addEventListener('input', debounce(renderDeliveries));

    // Orders status filter
    document.getElementById('filter-order-status')?.addEventListener('change', renderOrders);
});
