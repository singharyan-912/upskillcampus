/**
 * cart.js — Cart page logic.
 *
 * Handles:
 *  - Loading cart items from /api/cart
 *  - Rendering cart item rows with subtotals
 *  - Increase / Decrease quantity (PUT /api/cart/update)
 *  - Remove item (DELETE /api/cart/remove)
 *  - Live total update after each action
 *  - Empty cart state
 *  - Proceed to Checkout button (placeholder — redirects to orders.html)
 */

// ─────────────────────────────────────────────
//  LOAD & RENDER CART
// ─────────────────────────────────────────────

async function loadCart() {
    showCartLoading();

    const result = await api.get('/cart');

    if (!result.success) {
        // 401 = not logged in (handled by requireAuth), other errors:
        showCartError(result.message || 'Failed to load cart.');
        return;
    }

    const { items, total, restaurantName } = result.data;
    renderCart(items || [], total || 0, restaurantName);
}

function renderCart(items, total, restaurantName) {
    const mainSection = document.getElementById('cart-main');
    const emptySection = document.getElementById('cart-empty-state');

    if (items.length === 0) {
        if (mainSection)  mainSection.style.display  = 'none';
        if (emptySection) emptySection.style.display = 'flex';
        renderOrderSummary([], 0);
        return;
    }

    if (mainSection)  mainSection.style.display  = 'block';
    if (emptySection) emptySection.style.display = 'none';

    // Restaurant name in header
    const rnEl = document.getElementById('cart-restaurant-name');
    if (rnEl) rnEl.textContent = restaurantName ? `from ${restaurantName}` : '';

    // Item count
    const countEl = document.getElementById('cart-item-count');
    if (countEl) countEl.textContent = items.length + ' item' + (items.length !== 1 ? 's' : '');

    // Render rows
    const list = document.getElementById('cart-items-list');
    if (list) {
        list.innerHTML = items.map(item => cartItemRowHTML(item)).join('');
        wireCartItemButtons(list);
    }

    renderOrderSummary(items, total);
}

function cartItemRowHTML(item) {
    // lineTotal = unitPrice * quantity (computed by CartItem.getLineTotal())
    const lineTotal = item.lineTotal != null
        ? item.lineTotal
        : (item.unitPrice * item.quantity);

    return `
        <div class="cart-item-row" id="cart-item-row-${item.cartItemId}">

            <div class="cart-item-info">
                <div class="cart-item-name">${escapeHtml(item.itemName)}</div>
                <div class="cart-item-unit-price">${formatCurrency(item.unitPrice)} each</div>
            </div>

            <div class="qty-control" role="group" aria-label="Quantity for ${escapeHtml(item.itemName)}">
                <button class="qty-btn qty-decrease"
                        data-cart-item-id="${item.cartItemId}"
                        data-current-qty="${item.quantity}"
                        aria-label="Decrease quantity">−</button>
                <span class="qty-value" id="qty-val-${item.cartItemId}">${item.quantity}</span>
                <button class="qty-btn qty-increase"
                        data-cart-item-id="${item.cartItemId}"
                        data-current-qty="${item.quantity}"
                        aria-label="Increase quantity">+</button>
            </div>

            <div class="cart-item-subtotal" id="subtotal-${item.cartItemId}">
                ${formatCurrency(lineTotal)}
            </div>

            <button class="cart-item-remove-btn"
                    data-cart-item-id="${item.cartItemId}"
                    aria-label="Remove ${escapeHtml(item.itemName)} from cart"
                    title="Remove item">
                🗑️
            </button>
        </div>`;
}

function wireCartItemButtons(container) {
    // Increase quantity
    container.querySelectorAll('.qty-increase').forEach(btn => {
        btn.addEventListener('click', async () => {
            const cartItemId = parseInt(btn.dataset.cartItemId);
            const current    = parseInt(btn.dataset.currentQty);
            await updateQuantity(cartItemId, current + 1, btn);
        });
    });

    // Decrease quantity
    container.querySelectorAll('.qty-decrease').forEach(btn => {
        btn.addEventListener('click', async () => {
            const cartItemId = parseInt(btn.dataset.cartItemId);
            const current    = parseInt(btn.dataset.currentQty);
            if (current <= 1) {
                // Removing the last unit — confirm first
                if (confirm('Remove this item from your cart?')) {
                    await removeItem(cartItemId);
                }
                return;
            }
            await updateQuantity(cartItemId, current - 1, btn);
        });
    });

    // Remove button
    container.querySelectorAll('.cart-item-remove-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            const cartItemId = parseInt(btn.dataset.cartItemId);
            await removeItem(cartItemId);
        });
    });
}

// ─────────────────────────────────────────────
//  UPDATE QUANTITY
// ─────────────────────────────────────────────

async function updateQuantity(cartItemId, newQuantity, triggerBtn) {
    // Optimistic UI
    const row      = document.getElementById(`cart-item-row-${cartItemId}`);
    const qtySpan  = document.getElementById(`qty-val-${cartItemId}`);

    if (qtySpan) qtySpan.textContent = newQuantity;
    setRowLoading(cartItemId, true);

    const result = await api.put('/cart/update', { cartItemId, quantity: newQuantity });

    setRowLoading(cartItemId, false);

    if (result.success) {
        // Update data attributes so next click has the right value
        if (row) {
            row.querySelectorAll('.qty-increase, .qty-decrease').forEach(b => {
                b.dataset.currentQty = newQuantity;
            });
        }

        // Update subtotal inline
        const subtotalEl = document.getElementById(`subtotal-${cartItemId}`);
        if (subtotalEl && result.data) {
            // We need unitPrice for the subtotal — re-fetch or compute from total change
            // Simplest: reload full cart to stay consistent
            await loadCart();
        }

        // Update summary total
        if (result.data && result.data.newTotal !== undefined) {
            updateTotalDisplay(result.data.newTotal);
        }

    } else {
        // Revert optimistic update
        await loadCart();
        showToast(result.message || 'Could not update quantity.', 'error');
    }
}

// ─────────────────────────────────────────────
//  REMOVE ITEM
// ─────────────────────────────────────────────

async function removeItem(cartItemId) {
    const row = document.getElementById(`cart-item-row-${cartItemId}`);

    // Animate removal
    if (row) {
        row.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        row.style.opacity = '0';
        row.style.transform = 'translateX(20px)';
    }

    const result = await api.deleteWithParam('/cart/remove', { cartItemId });

    if (result.success) {
        showToast('Item removed from cart.', 'info');
        await loadCart(); // Full re-render to update all counts
    } else {
        // Restore row
        if (row) {
            row.style.opacity = '1';
            row.style.transform = 'none';
        }
        showToast(result.message || 'Could not remove item.', 'error');
    }
}

// ─────────────────────────────────────────────
//  ORDER SUMMARY PANEL
// ─────────────────────────────────────────────

function renderOrderSummary(items, total) {
    const itemCountEl = document.getElementById('summary-item-count');
    const subtotalEl  = document.getElementById('summary-subtotal');
    const totalEl     = document.getElementById('summary-total');
    const checkoutBtn = document.getElementById('checkout-btn');

    const count = items.length;

    if (itemCountEl) itemCountEl.textContent = count + ' item' + (count !== 1 ? 's' : '');
    if (subtotalEl)  subtotalEl.textContent  = formatCurrency(total);
    if (totalEl)     totalEl.textContent     = formatCurrency(total);

    if (checkoutBtn) {
        checkoutBtn.disabled = count === 0;
    }

    // Nav cart badge
    const badge = document.getElementById('nav-cart-count');
    if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'none';
    }
}

function updateTotalDisplay(total) {
    const subtotalEl = document.getElementById('summary-subtotal');
    const totalEl    = document.getElementById('summary-total');
    if (subtotalEl) subtotalEl.textContent = formatCurrency(total);
    if (totalEl)    totalEl.textContent    = formatCurrency(total);
}

// ─────────────────────────────────────────────
//  LOADING / ERROR STATES
// ─────────────────────────────────────────────

function showCartLoading() {
    const list = document.getElementById('cart-items-list');
    if (list) {
        list.innerHTML = Array(3).fill(`
            <div class="cart-item-row" style="opacity:0.5; pointer-events:none;">
                <div style="flex:1;">
                    <div class="skeleton-line medium" style="height:16px;margin-bottom:8px;"></div>
                    <div class="skeleton-line short" style="height:12px;"></div>
                </div>
                <div class="skeleton-line short" style="width:80px;height:32px;border-radius:2rem;"></div>
                <div class="skeleton-line short" style="width:60px;height:16px;"></div>
                <div class="skeleton-line short" style="width:32px;height:32px;border-radius:50%;"></div>
            </div>`).join('');
    }
}

function showCartError(message) {
    const mainSection = document.getElementById('cart-main');
    if (mainSection) {
        mainSection.innerHTML = `
            <div class="cart-items-container">
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h3>Could not load cart</h3>
                    <p>${escapeHtml(message)}</p>
                    <button class="btn btn-primary mt-4" onclick="loadCart()">Try Again</button>
                </div>
            </div>`;
    }
}

function setRowLoading(cartItemId, loading) {
    const row = document.getElementById(`cart-item-row-${cartItemId}`);
    if (row) {
        row.style.opacity = loading ? '0.5' : '1';
        row.querySelectorAll('button').forEach(b => b.disabled = loading);
    }
}

// ─────────────────────────────────────────────
//  UTILS
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
    if (!requireAuth(['CUSTOMER'])) return;

    initNav();
    loadCart();

    // Checkout button
    const checkoutBtn = document.getElementById('checkout-btn');
    if (checkoutBtn) {
        checkoutBtn.addEventListener('click', () => {
            window.location.href = CONTEXT + '/pages/checkout.html';
        });
    }

    // Continue shopping
    const continueBtn = document.getElementById('continue-shopping-btn');
    if (continueBtn) {
        continueBtn.addEventListener('click', () => {
            window.location.href = CONTEXT + '/pages/restaurants.html';
        });
    }
});
