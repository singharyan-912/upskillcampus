/**
 * menu.js — Menu browsing and Add to Cart logic.
 *
 * Handles:
 *  - Reading restaurantId from URL params
 *  - Loading restaurant info and menu items from backend
 *  - Rendering menu items (available only)
 *  - Category filter chips
 *  - Add to Cart calls to CartServlet
 *  - Sticky cart summary panel updates
 */

// ─────────────────────────────────────────────
//  STATE
// ─────────────────────────────────────────────

let allMenuItems     = [];
let cartSummaryItems = [];  // light cart state for the sidebar
let activeCategoryFilter = 'All';

// Read from URL
const urlParams      = new URLSearchParams(window.location.search);
const RESTAURANT_ID  = urlParams.get('restaurantId');
const RESTAURANT_NAME = decodeURIComponent(urlParams.get('name') || 'Restaurant');

// ─────────────────────────────────────────────
//  FOOD CATEGORY EMOJI
// ─────────────────────────────────────────────

const CATEGORY_EMOJI = {
    'Starter':    '🥗',
    'Main Course':'🍛',
    'Biryani':    '🍚',
    'Pizza':      '🍕',
    'Burger':     '🍔',
    'Dessert':    '🍰',
    'Beverage':   '🥤',
    'Snacks':     '🍿',
    'Seafood':    '🦐',
    'Veg':        '🥦',
    'Non-Veg':    '🍗',
    'default':    '🍽️',
};

function getCategoryEmoji(cat) {
    return CATEGORY_EMOJI[cat] || CATEGORY_EMOJI['default'];
}

// ─────────────────────────────────────────────
//  LOAD & RENDER RESTAURANT INFO
// ─────────────────────────────────────────────

async function loadRestaurantInfo() {
    const result = await api.get('/restaurants/' + RESTAURANT_ID);
    if (result.success && result.data) {
        const r = result.data;
        setEl('restaurant-banner-name',    r.name);
        setEl('restaurant-banner-cuisine', r.cuisineType || 'Multi-Cuisine');
        setEl('restaurant-banner-phone',   r.phone || 'N/A');
        setEl('restaurant-banner-address', r.address || 'N/A');
        setEl('restaurant-banner-status',  r.status === 'ACTIVE' ? '🟢 Open' : '🔴 Closed');
        setEl('page-title-name',           r.name);
        document.title = r.name + ' — Menu | FoodieExpress';
    }
}

// ─────────────────────────────────────────────
//  LOAD & RENDER MENU
// ─────────────────────────────────────────────

async function loadMenu() {
    showMenuSkeletons(6);

    const result = await api.get('/menu?restaurantId=' + RESTAURANT_ID);

    if (!result.success) {
        showMenuError(result.message || 'Failed to load menu.');
        return;
    }

    allMenuItems = result.data || [];
    buildCategoryFilters(allMenuItems);
    renderMenu(allMenuItems);
}

function renderMenu(items) {
    const grid = document.getElementById('menu-grid');
    if (!grid) return;

    if (items.length === 0) {
        grid.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🍽️</div>
                <h3>No items found</h3>
                <p>Try a different category or check back later.</p>
            </div>`;
        return;
    }

    grid.innerHTML = items.map(item => menuItemCardHTML(item)).join('');

    // Wire up Add to Cart buttons
    grid.querySelectorAll('.add-to-cart-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const menuItemId = parseInt(btn.dataset.menuItemId);
            const itemName   = btn.dataset.itemName;
            addToCart(menuItemId, itemName, btn);
        });
    });
}

function menuItemCardHTML(item) {
    const emoji = getCategoryEmoji(item.category);

    return `
        <div class="menu-item-card" id="menu-item-${item.menuItemId}">
            <div class="menu-item-image">
                <span>${emoji}</span>
            </div>

            <div class="menu-item-details">
                <div class="menu-item-name">${escapeHtml(item.itemName)}</div>
                <div class="menu-item-description">
                    ${escapeHtml(item.description || 'A delicious dish prepared fresh for you.')}
                </div>
                <div class="menu-item-footer">
                    <div class="menu-item-price">${formatCurrency(item.price)}</div>
                    <span class="menu-item-category-tag">${escapeHtml(item.category)}</span>
                    <span class="badge badge-success text-xs" style="font-size:0.65rem;">✓ Available</span>
                </div>
            </div>

            <div class="menu-item-actions">
                <button class="add-to-cart-btn"
                        id="add-btn-${item.menuItemId}"
                        data-menu-item-id="${item.menuItemId}"
                        data-item-name="${escapeHtml(item.itemName)}"
                        aria-label="Add ${escapeHtml(item.itemName)} to cart">
                    🛒 Add
                </button>
            </div>
        </div>`;
}

// ─────────────────────────────────────────────
//  ADD TO CART
// ─────────────────────────────────────────────

async function addToCart(menuItemId, itemName, btn) {
    // Optimistic UI update
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '⏳';

    const result = await api.post('/cart/add', { menuItemId, quantity: 1 });

    if (result.success) {
        btn.innerHTML = '✅ Added';
        btn.classList.add('added');
        showToast(`${itemName} added to cart!`, 'success');

        // Refresh the sidebar cart summary
        refreshCartSummary();

        // Reset button after a moment
        setTimeout(() => {
            btn.disabled  = false;
            btn.innerHTML = originalHtml;
            btn.classList.remove('added');
        }, 2000);

    } else {
        btn.disabled  = false;
        btn.innerHTML = originalHtml;

        // Specific error for cross-restaurant cart
        if (result.message && result.message.includes('different restaurant')) {
            showClearCartModal(menuItemId, itemName, btn);
        } else {
            showToast(result.message || 'Could not add item.', 'error');
        }
    }
}

// ─────────────────────────────────────────────
//  CROSS-RESTAURANT CART WARNING MODAL
// ─────────────────────────────────────────────

function showClearCartModal(menuItemId, itemName, btn) {
    const modal = document.getElementById('clear-cart-modal');
    if (!modal) {
        showToast('Your cart has items from another restaurant. Go to cart to clear it first.', 'error', 5000);
        return;
    }
    modal.classList.remove('hidden');

    const cancelBtn  = document.getElementById('modal-cancel-btn');
    const goCartBtn  = document.getElementById('modal-go-cart-btn');

    cancelBtn.onclick  = () => modal.classList.add('hidden');
    goCartBtn.onclick  = () => {
        modal.classList.add('hidden');
        window.location.href = CONTEXT + '/pages/cart.html';
    };
}

// ─────────────────────────────────────────────
//  CART SUMMARY SIDEBAR
// ─────────────────────────────────────────────

async function refreshCartSummary() {
    const result = await api.get('/cart');
    if (!result.success) return;

    const { items, total } = result.data;
    renderCartSummary(items || [], total || 0);

    // Update nav badge
    const badge = document.getElementById('nav-cart-count');
    if (badge) {
        const count = (items || []).length;
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'none';
    }
}

function renderCartSummary(items, total) {
    const container = document.getElementById('cart-summary-items');
    const totalEl   = document.getElementById('cart-summary-total');
    const emptyEl   = document.getElementById('cart-summary-empty');
    const goCartBtn = document.getElementById('go-to-cart-btn');

    if (!container) return;

    if (items.length === 0) {
        container.innerHTML = '';
        if (emptyEl)   emptyEl.style.display  = 'block';
        if (totalEl)   totalEl.style.display   = 'none';
        if (goCartBtn) goCartBtn.disabled       = true;
        return;
    }

    if (emptyEl)   emptyEl.style.display  = 'none';
    if (totalEl)   totalEl.style.display   = 'flex';
    if (goCartBtn) goCartBtn.disabled       = false;

    container.innerHTML = items.map(item => `
        <div class="cart-summary-item">
            <span class="cart-summary-item-name" title="${escapeHtml(item.itemName)}">
                ${escapeHtml(item.itemName)}
            </span>
            <span class="cart-summary-item-qty">×${item.quantity}</span>
            <span class="cart-summary-item-price">${formatCurrency(item.lineTotal || (item.unitPrice * item.quantity))}</span>
        </div>`).join('');

    const totalAmountEl = document.getElementById('cart-total-amount');
    if (totalAmountEl) totalAmountEl.textContent = formatCurrency(total);
}

// ─────────────────────────────────────────────
//  CATEGORY FILTER CHIPS
// ─────────────────────────────────────────────

function buildCategoryFilters(items) {
    const bar = document.getElementById('category-filter-chips');
    if (!bar) return;

    const categories = ['All', ...new Set(items.map(i => i.category).filter(Boolean))];

    bar.innerHTML = categories.map(c => `
        <button class="filter-chip ${c === 'All' ? 'active' : ''}"
                data-category="${escapeHtml(c)}">
            ${c === 'All' ? '🌟' : getCategoryEmoji(c)} ${c}
        </button>`).join('');

    bar.querySelectorAll('.filter-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            bar.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            activeCategoryFilter = chip.dataset.category;
            applyMenuFilter();
        });
    });
}

function applyMenuFilter() {
    const filtered = activeCategoryFilter === 'All'
        ? allMenuItems
        : allMenuItems.filter(i => i.category === activeCategoryFilter);
    renderMenu(filtered);
}

// ─────────────────────────────────────────────
//  SKELETON / ERROR STATES
// ─────────────────────────────────────────────

function showMenuSkeletons(count) {
    const grid = document.getElementById('menu-grid');
    if (!grid) return;
    grid.innerHTML = Array(count).fill(`
        <div class="menu-item-card" style="opacity:0.5; pointer-events:none;">
            <div class="menu-item-image"><span>🍽️</span></div>
            <div class="menu-item-details">
                <div class="skeleton-line title"></div>
                <div class="skeleton-line medium"></div>
                <div class="skeleton-line short"></div>
            </div>
        </div>`).join('');
}

function showMenuError(message) {
    const grid = document.getElementById('menu-grid');
    if (!grid) return;
    grid.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">⚠️</div>
            <h3>Failed to load menu</h3>
            <p>${escapeHtml(message)}</p>
            <button class="btn btn-primary mt-4" onclick="loadMenu()">Try Again</button>
        </div>`;
}

// ─────────────────────────────────────────────
//  UTILS
// ─────────────────────────────────────────────

function setEl(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ─────────────────────────────────────────────
//  INIT
// ─────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth(['CUSTOMER'])) return;

    if (!RESTAURANT_ID) {
        window.location.href = CONTEXT + '/pages/restaurants.html';
        return;
    }

    initNav();

    // Set restaurant name while full info loads
    setEl('restaurant-banner-name', RESTAURANT_NAME);
    setEl('page-title-name', RESTAURANT_NAME);
    document.title = RESTAURANT_NAME + ' — Menu | FoodieExpress';

    // Go to cart button in sidebar
    const goCartBtn = document.getElementById('go-to-cart-btn');
    if (goCartBtn) {
        goCartBtn.addEventListener('click', () => {
            window.location.href = CONTEXT + '/pages/cart.html';
        });
    }

    // Load in parallel
    loadRestaurantInfo();
    loadMenu();
    refreshCartSummary();
});
