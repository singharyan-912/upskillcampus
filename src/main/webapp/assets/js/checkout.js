/**
 * checkout.js — Checkout page logic.
 *
 * Flow:
 *  1. Load cart from GET /api/cart
 *  2. Render items table + summary sidebar
 *  3. User enters address
 *  4. On submit:
 *     a. POST /api/orders/checkout  → creates order, clears cart
 *     b. Redirect to payment.html?orderId={id}
 */

// ─────────────────────────────────────────────
//  STATE
// ─────────────────────────────────────────────
let cartTotal    = 0;
let cartItems    = [];

// ─────────────────────────────────────────────
//  LOAD CART
// ─────────────────────────────────────────────

async function loadCheckoutCart() {
    const result = await api.get('/cart');

    const loadingEl = document.getElementById('checkout-loading');
    const mainEl    = document.getElementById('checkout-main');

    if (!result.success) {
        if (loadingEl) {
            loadingEl.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h3>Could not load cart</h3>
                    <p>${escapeHtml(result.message || 'Please try again.')}</p>
                    <a href="cart.html" class="btn btn-primary" style="margin-top:1rem;">← Back to Cart</a>
                </div>`;
        }
        return;
    }

    const { items, total, restaurantName } = result.data;

    if (!items || items.length === 0) {
        // Cart is empty — nothing to checkout
        window.location.href = CONTEXT + '/pages/cart.html';
        return;
    }

    cartItems = items;
    cartTotal = parseFloat(total) || 0;

    if (loadingEl) loadingEl.style.display = 'none';
    if (mainEl)    mainEl.style.display    = 'grid';

    // Restaurant name
    setEl('checkout-restaurant-name', restaurantName ? `from ${restaurantName}` : '');

    renderItemsTable(items);
    renderSummary(items, total, restaurantName);
}

// ─────────────────────────────────────────────
//  RENDER ITEMS TABLE
// ─────────────────────────────────────────────

function renderItemsTable(items) {
    const body = document.getElementById('checkout-items-body');
    if (!body) return;

    const rows = items.map(item => {
        const lineTotal = item.lineTotal != null
            ? item.lineTotal
            : (parseFloat(item.unitPrice) * item.quantity);

        return `
            <tr>
                <td class="item-name">${escapeHtml(item.itemName)}</td>
                <td style="color:var(--color-text-muted); text-align:center;">${item.quantity}</td>
                <td style="color:var(--color-text-muted);">${formatCurrency(item.unitPrice)}</td>
                <td class="item-subtotal">${formatCurrency(lineTotal)}</td>
            </tr>`;
    }).join('');

    body.innerHTML = `
        <table class="review-items-table">
            <thead>
                <tr>
                    <th>Item</th>
                    <th style="text-align:center;">Qty</th>
                    <th>Unit Price</th>
                    <th>Subtotal</th>
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>`;
}

// ─────────────────────────────────────────────
//  RENDER SUMMARY SIDEBAR
// ─────────────────────────────────────────────

function renderSummary(items, total, restaurantName) {
    const summaryItems = document.getElementById('checkout-summary-items');
    const subtotalEl   = document.getElementById('summary-subtotal');
    const totalEl      = document.getElementById('summary-total');

    if (summaryItems) {
        summaryItems.innerHTML = items.map(item => {
            const lineTotal = item.lineTotal != null
                ? item.lineTotal
                : (parseFloat(item.unitPrice) * item.quantity);
            return `
                <div style="display:flex; justify-content:space-between; padding:0.4rem 0; font-size:0.85rem;">
                    <span style="color:var(--color-text);">${item.quantity}× ${escapeHtml(item.itemName)}</span>
                    <span style="color:var(--color-text-muted);">${formatCurrency(lineTotal)}</span>
                </div>`;
        }).join('') + '<hr style="border:none;border-top:1px solid var(--color-border-light);margin:0.75rem 0;">';
    }

    if (subtotalEl) subtotalEl.textContent = formatCurrency(total);
    if (totalEl)    totalEl.textContent    = formatCurrency(total);
}


// ─────────────────────────────────────────────
//  ERROR DISPLAY
// ─────────────────────────────────────────────

function showError(message) {
    const box  = document.getElementById('checkout-error-box');
    const text = document.getElementById('checkout-error-text');
    if (box && text) {
        text.textContent = message;
        box.style.display = 'flex';
        box.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } else {
        showToast(message, 'error');
    }
}

function hideError() {
    const box = document.getElementById('checkout-error-box');
    if (box) box.style.display = 'none';
}

// ─────────────────────────────────────────────
//  PLACE ORDER (CREATE ORDER ONLY)
// ─────────────────────────────────────────────

async function placeOrder() {
    hideError();

    const addressInput    = document.getElementById('checkout-address');
    const btn             = document.getElementById('place-order-btn');

    const address         = addressInput ? addressInput.value.trim() : '';

    // Client-side validation
    if (!address) {
        showError('Please enter a delivery address before placing your order.');
        if (addressInput) {
            addressInput.focus();
            addressInput.classList.add('error');
        }
        return;
    }
    if (addressInput) addressInput.classList.remove('error');

    // Loading state
    const originalHtml = btn.innerHTML;
    btn.disabled   = true;
    btn.innerHTML  = '⏳ Placing order...';

    try {
        // ─── Step 1: Create Order ───
        const orderResult = await api.post('/orders/checkout', {
            deliveryAddress: address
        });

        if (!orderResult.success) {
            showError(orderResult.message || 'Failed to create order. Please try again.');
            btn.disabled  = false;
            btn.innerHTML = originalHtml;
            return;
        }

        const { orderId } = orderResult.data;

        // ─── Step 2: Redirect to Payment ───
        window.location.href = CONTEXT + '/pages/payment.html?orderId=' + orderId;

    } catch (e) {
        showError('An unexpected error occurred. Please refresh and try again.');
        btn.disabled  = false;
        btn.innerHTML = originalHtml;
        console.error(e);
    }
}


// ─────────────────────────────────────────────
//  UTILITIES
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

    initNav();
    loadCheckoutCart();

    const btn = document.getElementById('place-order-btn');
    if (btn) btn.addEventListener('click', placeOrder);
});
