/**
 * restaurants.js — Restaurant browsing page logic.
 *
 * Handles:
 *  - Loading all active restaurants from /api/restaurants
 *  - Rendering restaurant cards
 *  - Search filtering (client-side, instant)
 *  - Cuisine type filter chips
 *  - "View Menu" navigation with restaurantId in URL
 */

// ─────────────────────────────────────────────
//  STATE
// ─────────────────────────────────────────────

let allRestaurants = [];     // full list from server
let activeFilter   = 'All'; // currently selected cuisine chip

// ─────────────────────────────────────────────
//  CUISINE EMOJI MAP
// ─────────────────────────────────────────────

const CUISINE_EMOJI = {
    'North Indian': '🍛',
    'South Indian': '🍜',
    'Chinese': '🥡',
    'Italian': '🍕',
    'Continental': '🥗',
    'Fast Food': '🍔',
    'Pizza': '🍕',
    'Biryani': '🍚',
    'Desserts': '🍰',
    'Beverages': '🥤',
    'Vegan': '🥦',
    'Seafood': '🦐',
    'default': '🍽️',
};

function getCuisineEmoji(cuisine) {
    return CUISINE_EMOJI[cuisine] || CUISINE_EMOJI['default'];
}

// ─────────────────────────────────────────────
//  LOAD RESTAURANTS
// ─────────────────────────────────────────────

async function loadRestaurants() {
    showSkeletons(6);

    const result = await api.get('/restaurants');

    if (!result.success) {
        showError(result.message || 'Failed to load restaurants.');
        return;
    }

    allRestaurants = result.data || [];
    buildCuisineFilters(allRestaurants);
    renderRestaurants(allRestaurants);
}

// ─────────────────────────────────────────────
//  RENDER
// ─────────────────────────────────────────────

function renderRestaurants(list) {
    const grid = document.getElementById('restaurants-grid');
    const countEl = document.getElementById('restaurant-count');
    if (!grid) return;

    if (countEl) {
        countEl.textContent = list.length + ' restaurant' + (list.length !== 1 ? 's' : '') + ' found';
    }

    if (list.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1/-1;">
                <div class="empty-state-icon">🍽️</div>
                <h3>No restaurants found</h3>
                <p>Try adjusting your search or filter to find what you're looking for.</p>
            </div>`;
        return;
    }

    grid.innerHTML = list.map(r => restaurantCardHTML(r)).join('');

    // Wire up "View Menu" buttons
    grid.querySelectorAll('.view-menu-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const id = btn.dataset.restaurantId;
            navigateToMenu(id, btn.dataset.restaurantName);
        });
    });

    // Whole card is also clickable
    grid.querySelectorAll('.restaurant-card').forEach(card => {
        card.addEventListener('click', () => {
            const id   = card.dataset.restaurantId;
            const name = card.dataset.restaurantName;
            navigateToMenu(id, name);
        });
    });
}

function restaurantCardHTML(r) {
    const emoji   = getCuisineEmoji(r.cuisineType);
    const statusBadge = r.status === 'ACTIVE'
        ? '<span class="badge badge-success">Open</span>'
        : '<span class="badge badge-error">Closed</span>';

    return `
        <div class="restaurant-card"
             data-restaurant-id="${r.restaurantId}"
             data-restaurant-name="${escapeHtml(r.name)}"
             role="button"
             tabindex="0"
             aria-label="View menu for ${escapeHtml(r.name)}">

            <div class="restaurant-card-image">
                <span>${emoji}</span>
                <div class="restaurant-card-status">${statusBadge}</div>
            </div>

            <div class="restaurant-card-body">
                <div class="restaurant-card-name">${escapeHtml(r.name)}</div>
                <div class="restaurant-card-cuisine">
                    <span>${emoji}</span>
                    <span>${escapeHtml(r.cuisineType || 'Multi-Cuisine')}</span>
                </div>
                <div class="restaurant-card-meta">
                    <span>📍 ${escapeHtml(r.address || 'Location not specified')}</span>
                </div>
            </div>

            <div class="restaurant-card-footer">
                <div class="restaurant-card-address">
                    📞 ${escapeHtml(r.phone || 'N/A')}
                </div>
                <button class="view-menu-btn"
                        data-restaurant-id="${r.restaurantId}"
                        data-restaurant-name="${escapeHtml(r.name)}"
                        aria-label="View menu for ${escapeHtml(r.name)}">
                    🍽️ Menu
                </button>
            </div>
        </div>`;
}

// ─────────────────────────────────────────────
//  CUISINE FILTER CHIPS
// ─────────────────────────────────────────────

function buildCuisineFilters(restaurants) {
    const bar = document.getElementById('filter-chips');
    if (!bar) return;

    const cuisines = ['All', ...new Set(restaurants.map(r => r.cuisineType).filter(Boolean))];

    bar.innerHTML = cuisines.map(c => `
        <button class="filter-chip ${c === 'All' ? 'active' : ''}"
                data-cuisine="${escapeHtml(c)}"
                aria-pressed="${c === 'All'}">
            ${c === 'All' ? '🌟' : getCuisineEmoji(c)} ${c}
        </button>`).join('');

    bar.querySelectorAll('.filter-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            bar.querySelectorAll('.filter-chip').forEach(c => {
                c.classList.remove('active');
                c.setAttribute('aria-pressed', 'false');
            });
            chip.classList.add('active');
            chip.setAttribute('aria-pressed', 'true');
            activeFilter = chip.dataset.cuisine;
            applyFilters();
        });
    });
}

// ─────────────────────────────────────────────
//  SEARCH + FILTER LOGIC
// ─────────────────────────────────────────────

function applyFilters() {
    const query = (document.getElementById('restaurant-search')?.value || '').toLowerCase();

    const filtered = allRestaurants.filter(r => {
        const matchesCuisine = activeFilter === 'All' || r.cuisineType === activeFilter;
        const matchesSearch  = !query
            || r.name.toLowerCase().includes(query)
            || (r.cuisineType && r.cuisineType.toLowerCase().includes(query))
            || (r.address && r.address.toLowerCase().includes(query));
        return matchesCuisine && matchesSearch;
    });

    renderRestaurants(filtered);
}

// ─────────────────────────────────────────────
//  NAVIGATION
// ─────────────────────────────────────────────

function navigateToMenu(restaurantId, restaurantName) {
    const url = `${CONTEXT}/pages/menu.html?restaurantId=${restaurantId}&name=${encodeURIComponent(restaurantName)}`;
    window.location.href = url;
}

// ─────────────────────────────────────────────
//  LOADING SKELETONS
// ─────────────────────────────────────────────

function showSkeletons(count) {
    const grid = document.getElementById('restaurants-grid');
    if (!grid) return;
    grid.innerHTML = Array(count).fill(`
        <div class="restaurant-card-skeleton">
            <div class="skeleton-restaurant-img"></div>
            <div class="skeleton-restaurant-body">
                <div class="skeleton-line title"></div>
                <div class="skeleton-line medium"></div>
                <div class="skeleton-line short"></div>
            </div>
        </div>`).join('');
}

function showError(message) {
    const grid = document.getElementById('restaurants-grid');
    if (!grid) return;
    grid.innerHTML = `
        <div class="empty-state" style="grid-column: 1/-1;">
            <div class="empty-state-icon">⚠️</div>
            <h3>Something went wrong</h3>
            <p>${escapeHtml(message)}</p>
            <button class="btn btn-primary mt-4" onclick="loadRestaurants()">Try Again</button>
        </div>`;
}

// ─────────────────────────────────────────────
//  UTILITIES
// ─────────────────────────────────────────────

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
    // Auth guard — only CUSTOMER can browse restaurants
    if (!requireAuth(['CUSTOMER'])) return;

    initNav();
    loadRestaurants();

    // Search input — debounced
    const searchInput = document.getElementById('restaurant-search');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(applyFilters, 250);
        });
    }
});
