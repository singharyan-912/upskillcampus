/**
 * restaurant-dashboard.js — Logic for the RESTAURANT role dashboard.
 *
 * Features:
 *  - Load owned restaurants → populate selectors
 *  - Menu tab: view all items (inc. unavailable), add / edit / delete / toggle availability
 *  - Orders tab: view incoming orders for owned restaurants, advance status
 *  - Info tab: edit restaurant name, address, phone, cuisine, status
 */

// ─────────────────────────────────────────────────────────
//  STATE
// ─────────────────────────────────────────────────────────

let myRestaurants = [];       // [{restaurantId, name, ...}]
let currentMenuItems = [];    // items currently rendered
let editingItemId = null;     // null = add mode, number = edit mode

// ─────────────────────────────────────────────────────────
//  DATE / UTILITY HELPERS
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
//  STATS SECTION
// ─────────────────────────────────────────────────────────

function updateStats(restaurants, items, activeOrders) {
    const setVal = (id, v) => { const el = document.getElementById(id); if (el) el.textContent = v; };
    setVal('stat-restaurants', restaurants.length);
    setVal('stat-items', items);
    setVal('stat-orders', activeOrders);
}

// ─────────────────────────────────────────────────────────
//  LOAD OWNED RESTAURANTS
// ─────────────────────────────────────────────────────────

async function loadMyRestaurants() {
    const result = await api.get('/restaurant/mine');
    if (!result.success) {
        showToast('Could not load your restaurants: ' + result.message, 'error');
        return;
    }

    myRestaurants = result.data || [];

    // Populate both selectors (menu tab + info tab)
    const selects = ['rest-select', 'info-rest-select'];
    selects.forEach(id => {
        const sel = document.getElementById(id);
        if (!sel) return;
        // Remove all but placeholder
        while (sel.options.length > 1) sel.remove(1);
        myRestaurants.forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.restaurantId;
            opt.textContent = r.name;
            sel.appendChild(opt);
        });
    });

    updateStats(myRestaurants, 0, 0);

    // Auto-select if only one restaurant
    if (myRestaurants.length === 1) {
        const sel = document.getElementById('rest-select');
        if (sel) { sel.value = myRestaurants[0].restaurantId; sel.dispatchEvent(new Event('change')); }
    }
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

    if (targetTab === 'orders') loadOrdersTab();
}

// ─────────────────────────────────────────────────────────
//  MENU TAB
// ─────────────────────────────────────────────────────────

async function loadMenu(restaurantId) {
    const menuList = document.getElementById('menu-list');
    if (!menuList) return;
    menuList.innerHTML = '<div class="dash-loading">⏳ Loading menu…</div>';

    const result = await api.get(`/restaurant/menu?restaurantId=${restaurantId}`);
    if (!result.success) {
        menuList.innerHTML = `<div class="dash-empty"><span class="dash-empty-icon">⚠️</span><h3>Error</h3><p>${esc(result.message)}</p></div>`;
        return;
    }

    currentMenuItems = result.data || [];
    renderMenu();
    updateStats(myRestaurants, currentMenuItems.length, parseInt(document.getElementById('stat-orders')?.textContent) || 0);
}

function renderMenu() {
    const menuList = document.getElementById('menu-list');
    const showUnavailable = document.getElementById('show-unavailable')?.checked;

    const items = showUnavailable
        ? currentMenuItems
        : currentMenuItems.filter(i => i.availability !== false);

    if (items.length === 0) {
        menuList.innerHTML = `
            <div class="dash-empty">
                <span class="dash-empty-icon">🍱</span>
                <h3>No menu items${showUnavailable ? '' : ' available'}</h3>
                <p>Add your first item using the button above.</p>
            </div>`;
        return;
    }

    menuList.innerHTML = items.map(item => menuItemCardHTML(item)).join('');
}

function menuItemCardHTML(item) {
    const available = item.availability !== false;
    return `
    <div class="menu-item-card ${available ? '' : 'unavailable'}" id="menu-card-${item.menuItemId}">
        <div class="menu-item-name">${esc(item.itemName)}</div>
        ${item.description ? `<div class="menu-item-desc">${esc(item.description)}</div>` : ''}
        <div class="menu-item-meta">
            <span class="menu-item-price">${formatCurrency(item.price)}</span>
            <span class="badge ${available ? 'badge-ACTIVE' : 'badge-INACTIVE'}">${available ? 'Available' : 'Unavailable'}</span>
        </div>
        <div style="font-size:0.75rem; color:var(--color-text-muted);">📂 ${esc(item.category)}</div>
        <div class="menu-item-actions">
            <button class="btn btn-outline btn-sm" onclick="openEditModal(${item.menuItemId})">✏️ Edit</button>
            <button class="btn btn-sm" style="background:${available ? 'var(--color-warning-bg)' : 'var(--color-success-bg)'}; color:${available ? '#a16207' : '#008069'}; border:1px solid currentColor;"
                    onclick="toggleAvailability(${item.menuItemId}, ${!available})" id="avail-btn-${item.menuItemId}">
                ${available ? '🚫 Disable' : '✅ Enable'}
            </button>
            <button class="btn btn-sm" style="background:var(--color-error-bg); color:var(--color-error); border:1px solid rgba(214,48,49,0.2);"
                    onclick="deleteItem(${item.menuItemId})">🗑️ Delete</button>
        </div>
    </div>`;
}

// ─────────────────────────────────────────────────────────
//  MENU CRUD
// ─────────────────────────────────────────────────────────

async function toggleAvailability(menuItemId, newAvail) {
    const btn = document.getElementById(`avail-btn-${menuItemId}`);
    if (btn) { btn.disabled = true; btn.textContent = '…'; }

    const result = await api.put(`/restaurant/menu/${menuItemId}/availability`, { available: newAvail });

    if (result.success) {
        showToast(newAvail ? 'Item enabled.' : 'Item disabled.', 'success');
        // Update in local state
        const item = currentMenuItems.find(i => i.menuItemId === menuItemId);
        if (item) item.availability = newAvail;
        renderMenu();
    } else {
        showToast(result.message || 'Failed to update availability.', 'error');
        if (btn) { btn.disabled = false; btn.textContent = newAvail ? '✅ Enable' : '🚫 Disable'; }
    }
}

async function deleteItem(menuItemId) {
    if (!confirm('Delete this menu item? This cannot be undone.')) return;

    const result = await api.delete(`/restaurant/menu/${menuItemId}`);
    if (result.success) {
        showToast('Menu item deleted.', 'success');
        currentMenuItems = currentMenuItems.filter(i => i.menuItemId !== menuItemId);
        renderMenu();
        updateStats(myRestaurants, currentMenuItems.length, parseInt(document.getElementById('stat-orders')?.textContent) || 0);
    } else {
        showToast(result.message || 'Failed to delete item.', 'error');
    }
}

// ─────────────────────────────────────────────────────────
//  MENU MODAL  (Add / Edit)
// ─────────────────────────────────────────────────────────

function openAddModal() {
    const sel = document.getElementById('rest-select');
    if (!sel || !sel.value) { showToast('Please select a restaurant first.', 'error'); return; }

    editingItemId = null;
    document.getElementById('item-modal-title').textContent = 'Add Menu Item';
    document.getElementById('item-id').value = '';
    document.getElementById('item-rest-id').value = sel.value;
    document.getElementById('item-name').value = '';
    document.getElementById('item-description').value = '';
    document.getElementById('item-category').value = '';
    document.getElementById('item-price').value = '';
    document.getElementById('item-available').checked = true;
    clearItemAlert();
    openModal('item-modal-overlay');
}

function openEditModal(menuItemId) {
    const item = currentMenuItems.find(i => i.menuItemId === menuItemId);
    if (!item) return;

    editingItemId = menuItemId;
    document.getElementById('item-modal-title').textContent = 'Edit Menu Item';
    document.getElementById('item-id').value = menuItemId;
    document.getElementById('item-rest-id').value = item.restaurantId;
    document.getElementById('item-name').value = item.itemName || '';
    document.getElementById('item-description').value = item.description || '';
    document.getElementById('item-category').value = item.category || '';
    document.getElementById('item-price').value = item.price || '';
    document.getElementById('item-available').checked = item.availability !== false;
    clearItemAlert();
    openModal('item-modal-overlay');
}

function clearItemAlert() {
    const alertEl = document.getElementById('item-alert');
    if (alertEl) { alertEl.className = 'dash-alert'; alertEl.textContent = ''; }
}

function showItemAlert(msg, type) {
    const alertEl = document.getElementById('item-alert');
    if (!alertEl) return;
    alertEl.textContent = msg;
    alertEl.className = `dash-alert show ${type}`;
}

async function saveItem() {
    const nameVal     = document.getElementById('item-name').value.trim();
    const descVal     = document.getElementById('item-description').value.trim();
    const categoryVal = document.getElementById('item-category').value.trim();
    const priceVal    = parseFloat(document.getElementById('item-price').value);
    const available   = document.getElementById('item-available').checked;
    const restId      = parseInt(document.getElementById('item-rest-id').value);

    if (!nameVal || !categoryVal || isNaN(priceVal) || priceVal <= 0) {
        showItemAlert('Item name, category, and a valid price are required.', 'error');
        return;
    }

    const saveBtn = document.getElementById('item-save-btn');
    if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = 'Saving…'; }

    let result;
    if (editingItemId) {
        result = await api.put(`/restaurant/menu/${editingItemId}`, {
            itemName: nameVal, description: descVal, category: categoryVal,
            price: priceVal, available,
        });
    } else {
        result = await api.post('/restaurant/menu', {
            restaurantId: restId, itemName: nameVal, description: descVal,
            category: categoryVal, price: priceVal, available,
        });
    }

    if (saveBtn) { saveBtn.disabled = false; saveBtn.textContent = 'Save Item'; }

    if (result.success) {
        showToast(editingItemId ? 'Item updated.' : 'Item added.', 'success');
        closeModal('item-modal-overlay');
        // Refresh menu from server
        loadMenu(restId);
    } else {
        showItemAlert(result.message || 'Failed to save item.', 'error');
    }
}

// ─────────────────────────────────────────────────────────
//  ORDERS TAB
// ─────────────────────────────────────────────────────────

const ORDER_NEXT_STATUS = {
    PLACED:    'CONFIRMED',
    CONFIRMED: 'PREPARING',
    PREPARING: 'READY',
};

async function loadOrdersTab() {
    const listEl = document.getElementById('orders-list');
    if (!listEl) return;
    listEl.innerHTML = '<div class="dash-loading">⏳ Loading orders…</div>';

    // For RESTAURANT role, the backend requires ?restaurantId=X for each restaurant.
    // Fetch orders for all owned restaurants in parallel and merge them.
    if (!myRestaurants || myRestaurants.length === 0) {
        listEl.innerHTML = `<div class="dash-empty"><span class="dash-empty-icon">🏪</span><h3>No restaurants</h3><p>You have no restaurants to show orders for.</p></div>`;
        return;
    }

    const allOrders = [];
    const seenIds = new Set();
    for (const r of myRestaurants) {
        const result = await api.get(`/orders?restaurantId=${r.restaurantId}`);
        if (result.success && Array.isArray(result.data)) {
            for (const order of result.data) {
                if (!seenIds.has(order.orderId)) {
                    seenIds.add(order.orderId);
                    allOrders.push(order);
                }
            }
        }
    }

    // Create a fake result object to continue with existing render logic
    const result = { success: true, data: allOrders };
    if (!result.success) {
        listEl.innerHTML = `<div class="dash-empty"><span class="dash-empty-icon">⚠️</span><h3>Error loading orders</h3><p>${esc(result.message)}</p></div>`;
        return;
    }

    const orders = (result.data || []).sort((a, b) => b.orderId - a.orderId);
    const active = orders.filter(o => !['DELIVERED', 'CANCELLED'].includes(o.status));
    updateStats(myRestaurants, currentMenuItems.length, active.length);

    if (orders.length === 0) {
        listEl.innerHTML = `<div class="dash-empty"><span class="dash-empty-icon">📦</span><h3>No orders yet</h3><p>Orders from customers will appear here.</p></div>`;
        return;
    }

    listEl.innerHTML = orders.map(o => orderCardHTML(o)).join('');
}

function orderCardHTML(order) {
    const nextStatus = ORDER_NEXT_STATUS[order.status];
    return `
    <div class="dash-order-card" id="dash-order-${order.orderId}">
        <div class="dash-order-header">
            <span class="dash-order-id">Order #${order.orderId}</span>
            ${badgeHTML(order.status)}
        </div>
        <div class="dash-order-meta">
            <span>👤 ${esc(order.customerName || 'Customer')}</span>
            <span>📍 ${esc(order.deliveryAddress || '—')}</span>
            <span>💰 ${formatCurrency(order.totalAmount)}</span>
            <span>📅 ${fmtDate(order.createdAt)}</span>
        </div>
        <div class="dash-order-actions">
            ${nextStatus ? `
            <button class="btn btn-primary btn-sm"
                    onclick="advanceOrderStatus(${order.orderId}, '${nextStatus}', this)"
                    id="status-btn-${order.orderId}">
                ▶ Mark ${nextStatus.replace(/_/g, ' ')}
            </button>` : ''}
            ${order.status === 'PLACED' || order.status === 'CONFIRMED' ? `
            <button class="btn btn-sm" style="background:var(--color-error-bg);color:var(--color-error);border:1px solid rgba(214,48,49,0.2);"
                    onclick="advanceOrderStatus(${order.orderId}, 'CANCELLED', this)">
                ✕ Cancel
            </button>` : ''}
        </div>
    </div>`;
}

async function advanceOrderStatus(orderId, newStatus, btn) {
    if (!confirm(`Change order #${orderId} status to ${newStatus.replace(/_/g, ' ')}?`)) return;
    if (btn) { btn.disabled = true; btn.textContent = '…'; }

    const result = await api.put('/orders/status', { orderId, newStatus });
    if (result.success) {
        showToast(`Order #${orderId} → ${newStatus.replace(/_/g, ' ')}`, 'success');
        loadOrdersTab();
    } else {
        showToast(result.message || 'Could not update order status.', 'error');
        if (btn) { btn.disabled = false; btn.textContent = `▶ Mark ${newStatus.replace(/_/g, ' ')}`; }
    }
}

// ─────────────────────────────────────────────────────────
//  INFO TAB
// ─────────────────────────────────────────────────────────

function loadInfoForm(restaurantId) {
    const restaurant = myRestaurants.find(r => r.restaurantId === parseInt(restaurantId));
    const form       = document.getElementById('restaurant-info-form');
    const placeholder= document.getElementById('info-placeholder');

    if (!restaurant || !form) return;

    document.getElementById('info-name').value    = restaurant.name || '';
    document.getElementById('info-phone').value   = restaurant.phone || '';
    document.getElementById('info-cuisine').value = restaurant.cuisineType || '';
    document.getElementById('info-address').value = restaurant.address || '';
    document.getElementById('info-status').value  = restaurant.status || 'ACTIVE';

    form.style.display = 'block';
    if (placeholder) placeholder.style.display = 'none';

    const alertEl = document.getElementById('info-alert');
    if (alertEl) { alertEl.className = 'dash-alert'; alertEl.textContent = ''; }
}

async function saveRestaurantInfo(e) {
    e.preventDefault();
    const sel = document.getElementById('info-rest-select');
    if (!sel || !sel.value) { showToast('No restaurant selected.', 'error'); return; }

    const restaurantId = parseInt(sel.value);
    const payload = {
        name:        document.getElementById('info-name').value.trim(),
        phone:       document.getElementById('info-phone').value.trim(),
        cuisineType: document.getElementById('info-cuisine').value.trim(),
        address:     document.getElementById('info-address').value.trim(),
        status:      document.getElementById('info-status').value,
    };

    if (!payload.name || !payload.phone || !payload.cuisineType || !payload.address) {
        showAlert('info-alert', 'All fields are required.', 'error'); return;
    }

    const result = await api.put(`/restaurant/info/${restaurantId}`, payload);
    if (result.success) {
        showAlert('info-alert', '✅ Restaurant info updated.', 'success');
        showToast('Restaurant info saved.', 'success');
        // Update local state
        const r = myRestaurants.find(r => r.restaurantId === restaurantId);
        if (r) Object.assign(r, payload);
    } else {
        showAlert('info-alert', result.message || 'Failed to save.', 'error');
    }
}

function showAlert(id, msg, type) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = msg;
    el.className = `dash-alert show ${type}`;
}

// ─────────────────────────────────────────────────────────
//  MODAL HELPERS
// ─────────────────────────────────────────────────────────

function openModal(overlayId) {
    const overlay = document.getElementById(overlayId);
    if (overlay) overlay.classList.add('open');
}

function closeModal(overlayId) {
    const overlay = document.getElementById(overlayId);
    if (overlay) overlay.classList.remove('open');
}

// ─────────────────────────────────────────────────────────
//  INIT
// ─────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth(['RESTAURANT'])) return;
    initNav();
    loadMyRestaurants();

    // Tab switching
    document.querySelectorAll('.dashboard-tab').forEach(btn => {
        btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // Restaurant selector (menu tab)
    document.getElementById('rest-select')?.addEventListener('change', e => {
        const val = e.target.value;
        if (val) {
            document.getElementById('item-rest-id') && (document.getElementById('item-rest-id').value = val);
            loadMenu(val);
        } else {
            document.getElementById('menu-list').innerHTML = `
                <div class="dash-empty"><span class="dash-empty-icon">🏪</span>
                <h3>Select a restaurant</h3><p>Choose a restaurant above to manage its menu.</p></div>`;
        }
    });

    // Show-unavailable toggle
    document.getElementById('show-unavailable')?.addEventListener('change', renderMenu);

    // Add menu item button
    document.getElementById('btn-add-item')?.addEventListener('click', openAddModal);

    // Modal save/cancel/close
    document.getElementById('item-save-btn')?.addEventListener('click', saveItem);
    document.getElementById('item-cancel-btn')?.addEventListener('click', () => closeModal('item-modal-overlay'));
    document.getElementById('item-modal-close')?.addEventListener('click', () => closeModal('item-modal-overlay'));
    document.getElementById('item-modal-overlay')?.addEventListener('click', e => {
        if (e.target === e.currentTarget) closeModal('item-modal-overlay');
    });

    // Orders tab refresh
    document.getElementById('btn-refresh-orders')?.addEventListener('click', loadOrdersTab);

    // Info tab restaurant selector
    document.getElementById('info-rest-select')?.addEventListener('change', e => {
        if (e.target.value) loadInfoForm(e.target.value);
    });

    // Info form submit
    document.getElementById('restaurant-info-form')?.addEventListener('submit', saveRestaurantInfo);

    // Set subtitle
    const name = Session.getName();
    const subtitleEl = document.getElementById('dash-subtitle');
    if (subtitleEl && name) subtitleEl.textContent = `Welcome back, ${name}! Manage your menus and track incoming orders.`;
});
