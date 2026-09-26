/**
 * delivery-dashboard.js — Logic for the DELIVERY_PARTNER role dashboard.
 *
 * Features:
 *  - Load own deliveries (GET /api/delivery/mine)
 *  - Load available READY orders (GET /api/delivery/available)
 *  - Self-assign an order (POST /api/delivery/assign)
 *  - Advance delivery status: ASSIGNED → PICKED_UP → DELIVERED
 *    (PUT /api/delivery/status)
 *  - Stat cards: total, active, delivered, available
 *  - Status filter on My Deliveries tab
 */

// ─────────────────────────────────────────────────────────
//  STATE
// ─────────────────────────────────────────────────────────

let myDeliveries    = [];   // enriched delivery rows from backend
let availableOrders = [];   // READY orders not yet assigned

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
    return `<span class="badge badge-${status}">${esc(status.replace(/_/g, ' '))}</span>`;
}

// ─────────────────────────────────────────────────────────
//  STATS
// ─────────────────────────────────────────────────────────

function updateStats() {
    const active    = myDeliveries.filter(d => d.deliveryStatus !== 'DELIVERED').length;
    const delivered = myDeliveries.filter(d => d.deliveryStatus === 'DELIVERED').length;

    const setVal = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v; };
    setVal('stat-total',     myDeliveries.length);
    setVal('stat-active',    active);
    setVal('stat-delivered', delivered);
    setVal('stat-available', availableOrders.length);
}

// ─────────────────────────────────────────────────────────
//  TAB NAVIGATION
// ─────────────────────────────────────────────────────────

function switchTab(targetTab) {
    document.querySelectorAll('.dashboard-tab').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === targetTab);
    });
    document.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.toggle('active', panel.id === `panel-${targetTab}`);
    });

    if (targetTab === 'available') loadAvailableOrders();
}

// ─────────────────────────────────────────────────────────
//  MY DELIVERIES
// ─────────────────────────────────────────────────────────

async function loadMyDeliveries() {
    const listEl = document.getElementById('mine-list');
    if (!listEl) return;
    listEl.innerHTML = '<div class="dash-loading">⏳ Loading your deliveries…</div>';

    const result = await api.get('/delivery/mine');
    if (!result.success) {
        listEl.innerHTML = `<div class="dash-empty"><span class="dash-empty-icon">⚠️</span><h3>Error</h3><p>${esc(result.message)}</p></div>`;
        return;
    }

    myDeliveries = result.data || [];
    updateStats();
    renderMyDeliveries();
}

function renderMyDeliveries() {
    const listEl      = document.getElementById('mine-list');
    const filterSel   = document.getElementById('filter-status');
    const filterVal   = filterSel ? filterSel.value : '';

    const filtered = filterVal
        ? myDeliveries.filter(d => d.deliveryStatus === filterVal)
        : myDeliveries;

    if (filtered.length === 0) {
        listEl.innerHTML = `
            <div class="dash-empty">
                <span class="dash-empty-icon">🏍️</span>
                <h3>${filterVal ? 'No deliveries with this status' : 'No deliveries yet'}</h3>
                <p>${filterVal ? 'Try a different filter.' : 'Accept an order from the "Available Orders" tab!'}</p>
            </div>`;
        return;
    }

    listEl.innerHTML = filtered.map(d => deliveryCardHTML(d)).join('');
}

function deliveryCardHTML(d) {
    const nextStatus = { ASSIGNED: 'PICKED_UP', PICKED_UP: 'DELIVERED' }[d.deliveryStatus];
    const nextLabel  = { PICKED_UP: '📦 Mark Picked Up', DELIVERED: '✅ Mark Delivered' }[nextStatus] || '';

    return `
    <div class="delivery-card" id="dcard-${d.deliveryId}">
        <div class="delivery-card-header">
            <span class="delivery-card-id">🚚 Delivery #${d.deliveryId} &mdash; Order #${d.orderId}</span>
            ${badgeHTML(d.deliveryStatus)}
        </div>
        <div class="delivery-card-body">
            <div class="delivery-info-row">
                <span class="delivery-info-label">🏪 Restaurant</span>
                <span>${esc(d.restaurantName)}</span>
            </div>
            <div class="delivery-info-row">
                <span class="delivery-info-label">📍 Pick-up</span>
                <span>${esc(d.restaurantAddress)}</span>
            </div>
            <div class="delivery-info-row">
                <span class="delivery-info-label">🏠 Drop-off</span>
                <span>${esc(d.deliveryAddress)}</span>
            </div>
            <div class="delivery-info-row">
                <span class="delivery-info-label">💰 Order Total</span>
                <span style="font-weight:700;color:var(--color-primary);">${formatCurrency(d.totalAmount)}</span>
            </div>
            <div class="delivery-info-row">
                <span class="delivery-info-label">⏱ Assigned</span>
                <span>${fmtDate(d.assignedTime)}</span>
            </div>
            ${d.pickupTime ? `
            <div class="delivery-info-row">
                <span class="delivery-info-label">📦 Picked Up</span>
                <span>${fmtDate(d.pickupTime)}</span>
            </div>` : ''}
            ${d.deliveredTime ? `
            <div class="delivery-info-row">
                <span class="delivery-info-label">✅ Delivered</span>
                <span>${fmtDate(d.deliveredTime)}</span>
            </div>` : ''}
        </div>
        ${nextStatus ? `
        <div class="delivery-card-actions">
            <button class="btn btn-primary btn-sm"
                    onclick="advanceDelivery(${d.orderId}, '${nextStatus}', this)"
                    id="advance-btn-${d.deliveryId}">
                ${nextLabel}
            </button>
        </div>` : ''}
    </div>`;
}

async function advanceDelivery(orderId, newStatus, btn) {
    const label = newStatus === 'PICKED_UP' ? 'Picked Up' : 'Delivered';
    if (!confirm(`Mark Order #${orderId} as ${label}?`)) return;
    if (btn) { btn.disabled = true; btn.textContent = '…'; }

    const result = await api.put('/delivery/status', { orderId, newStatus });
    if (result.success) {
        showToast(`Status updated to ${label}!`, 'success');
        await loadMyDeliveries();
    } else {
        showToast(result.message || 'Could not update status.', 'error');
        if (btn) { btn.disabled = false; btn.textContent = `Mark ${label}`; }
    }
}

// ─────────────────────────────────────────────────────────
//  AVAILABLE ORDERS
// ─────────────────────────────────────────────────────────

async function loadAvailableOrders() {
    const listEl = document.getElementById('available-list');
    if (!listEl) return;
    listEl.innerHTML = '<div class="dash-loading">⏳ Looking for READY orders…</div>';

    const result = await api.get('/delivery/available');
    if (!result.success) {
        listEl.innerHTML = `<div class="dash-empty"><span class="dash-empty-icon">⚠️</span><h3>Error</h3><p>${esc(result.message)}</p></div>`;
        return;
    }

    availableOrders = result.data || [];
    updateStats();

    if (availableOrders.length === 0) {
        listEl.innerHTML = `
            <div class="dash-empty">
                <span class="dash-empty-icon">🎉</span>
                <h3>No available orders right now</h3>
                <p>All READY orders are assigned. Check back soon!</p>
            </div>`;
        return;
    }

    listEl.innerHTML = availableOrders.map(o => availableOrderCardHTML(o)).join('');
}

function availableOrderCardHTML(o) {
    return `
    <div class="dash-order-card" id="avail-order-${o.orderId}">
        <div class="dash-order-header">
            <span class="dash-order-id">Order #${o.orderId}</span>
            <span class="badge badge-READY">READY</span>
        </div>
        <div class="dash-order-meta">
            <span>🏪 ${esc(o.restaurantName)}</span>
            <span>📍 Pick-up: ${esc(o.restaurantAddress)}</span>
            <span>🏠 Drop-off: ${esc(o.deliveryAddress)}</span>
            <span>💰 ${formatCurrency(o.totalAmount)}</span>
            <span>📅 Placed: ${fmtDate(o.createdAt)}</span>
        </div>
        <div class="dash-order-actions">
            <button class="btn btn-primary btn-sm"
                    onclick="assignOrder(${o.orderId}, this)"
                    id="assign-btn-${o.orderId}">
                🚴 Accept Delivery
            </button>
        </div>
    </div>`;
}

async function assignOrder(orderId, btn) {
    if (!confirm(`Accept delivery for Order #${orderId}?`)) return;
    if (btn) { btn.disabled = true; btn.textContent = 'Accepting…'; }

    const result = await api.post('/delivery/assign', { orderId });
    if (result.success) {
        showToast(`Delivery accepted for Order #${orderId}!`, 'success');
        // Remove from available list instantly
        availableOrders = availableOrders.filter(o => o.orderId !== orderId);
        updateStats();
        const card = document.getElementById(`avail-order-${orderId}`);
        if (card) card.remove();
        if (availableOrders.length === 0) loadAvailableOrders();
        // Also refresh my deliveries in background
        loadMyDeliveries();
    } else {
        showToast(result.message || 'Could not accept delivery.', 'error');
        if (btn) { btn.disabled = false; btn.textContent = '🚴 Accept Delivery'; }
    }
}

// ─────────────────────────────────────────────────────────
//  INIT
// ─────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth(['DELIVERY_PARTNER'])) return;
    initNav();

    // Set subtitle
    const name = Session.getName();
    const subtitleEl = document.getElementById('dash-subtitle');
    if (subtitleEl && name) subtitleEl.textContent = `Welcome, ${name}! Manage your deliveries below.`;

    // Load data
    loadMyDeliveries();
    // Pre-fetch available orders for stats counter
    api.get('/delivery/available').then(r => {
        if (r.success) { availableOrders = r.data || []; updateStats(); }
    });

    // Tab switching
    document.querySelectorAll('.dashboard-tab').forEach(btn => {
        btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // Refresh buttons
    document.getElementById('btn-refresh-all')?.addEventListener('click', () => {
        loadMyDeliveries();
        api.get('/delivery/available').then(r => {
            if (r.success) { availableOrders = r.data || []; updateStats(); }
        });
    });
    document.getElementById('btn-refresh-available')?.addEventListener('click', loadAvailableOrders);

    // Status filter
    document.getElementById('filter-status')?.addEventListener('change', renderMyDeliveries);
});
