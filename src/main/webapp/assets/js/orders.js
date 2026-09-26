/**
 * orders.js — Customer order history page logic.
 *
 * Features:
 *  - Load orders (GET /api/orders) with restaurant name + createdAt
 *  - Render order cards with all 7 status badges
 *  - Fetch + display payment status per order
 *  - Expandable "View Details" panel: items table + addresses + timestamps
 *  - Inline "Write Review" form for DELIVERED orders (1–5 star rating)
 *  - All review validation delegated to backend (ReviewService)
 */

// ─────────────────────────────────────────────
//  STATUS CONFIG
// ─────────────────────────────────────────────

const STATUS_CONFIG = {
    PLACED:           { icon: '🕐', label: 'Placed' },
    CONFIRMED:        { icon: '✅', label: 'Confirmed' },
    PREPARING:        { icon: '👨‍🍳', label: 'Preparing' },
    READY:            { icon: '✔️', label: 'Ready' },
    OUT_FOR_DELIVERY: { icon: '🏍️', label: 'Out for Delivery' },
    DELIVERED:        { icon: '🎉', label: 'Delivered' },
    CANCELLED:        { icon: '❌', label: 'Cancelled' },
};

function statusBadgeHTML(status) {
    const cfg = STATUS_CONFIG[status] || { icon: '❓', label: status };
    return `<span class="status-badge status-${status}">${cfg.icon} ${cfg.label}</span>`;
}

function paymentBadgeHTML(payments) {
    if (!payments || payments.length === 0) {
        return `<span class="pay-badge pay-NONE">⬜ Not Paid</span>`;
    }
    // Use the most recent payment
    const p = payments[payments.length - 1];
    const icons = { SUCCESS: '✅', PENDING: '🕐', FAILED: '❌' };
    const icon  = icons[p.paymentStatus] || '❓';
    const method = p.paymentMethod === 'COD' ? 'COD' : 'Online';
    return `<span class="pay-badge pay-${p.paymentStatus}">${icon} ${method} — ${p.paymentStatus}</span>`;
}

// ─────────────────────────────────────────────
//  LOAD ORDERS
// ─────────────────────────────────────────────

async function loadOrders() {
    const listEl = document.getElementById('orders-list');
    if (!listEl) return;

    const result = await api.get('/orders');

    // Remove loading skeleton
    const loadingEl = document.getElementById('orders-loading');
    if (loadingEl) loadingEl.remove();

    if (!result.success) {
        listEl.innerHTML = `
            <div class="empty-state" style="background:white; border-radius:1rem; padding:3rem; box-shadow:var(--shadow-sm);">
                <div class="empty-state-icon">⚠️</div>
                <h3>Could not load orders</h3>
                <p>${escapeHtml(result.message || 'Please try again.')}</p>
                <button class="btn btn-primary" style="margin-top:1rem;" onclick="location.reload()">Try Again</button>
            </div>`;
        return;
    }

    const orders = result.data || [];

    if (orders.length === 0) {
        listEl.innerHTML = `
            <div class="empty-state" style="background:white; border-radius:1rem; padding:3rem; box-shadow:var(--shadow-sm); border:1px solid var(--color-border-light);">
                <div class="empty-state-icon">🧾</div>
                <h3>No orders yet</h3>
                <p>You haven't placed any orders yet. Discover great restaurants!</p>
                <a href="restaurants.html" class="btn btn-primary" style="margin-top:1.25rem;">Browse Restaurants</a>
            </div>`;
        return;
    }

    // Sort newest first (by orderId descending — DB auto-increment)
    orders.sort((a, b) => b.orderId - a.orderId);

    // Render all cards first (fast)
    listEl.innerHTML = orders.map(o => orderCardHTML(o)).join('');

    // Then fetch payment status per order asynchronously
    orders.forEach(o => fetchAndSetPaymentStatus(o.orderId));
}

// ─────────────────────────────────────────────
//  ORDER CARD HTML
// ─────────────────────────────────────────────

function orderCardHTML(order) {
    const dateStr = order.createdAt
        ? formatDate(order.createdAt)
        : '—';

    const canReview = order.status === 'DELIVERED';

    return `
    <div class="order-card" id="order-card-${order.orderId}">

        <!-- Header -->
        <div class="order-card-header">
            <div class="order-card-left">
                <div class="order-card-id">Order #${order.orderId}</div>
                <div class="order-card-meta">
                    <span>🏪 ${escapeHtml(order.restaurantName || 'Restaurant')}</span>
                    <span>📅 ${dateStr}</span>
                    <span>📍 ${escapeHtml(truncate(order.deliveryAddress, 40))}</span>
                </div>
            </div>
            <div class="order-card-right">
                ${statusBadgeHTML(order.status)}
                <div class="order-total-badge">${formatCurrency(order.totalAmount)}</div>
            </div>
        </div>

        <!-- Payment status (loaded asynchronously) -->
        <div style="padding: 0 1.5rem 0.75rem; display:flex; align-items:center; gap:0.75rem; flex-wrap:wrap;">
            <span id="pay-status-${order.orderId}" class="pay-badge pay-NONE">⬜ Loading payment...</span>
        </div>

        <!-- Actions -->
        <div class="order-card-actions">
            <button class="btn btn-outline btn-sm" style="display:flex; align-items:center; gap:0.375rem;"
                    onclick="toggleDetails(${order.orderId})" id="details-btn-${order.orderId}"
                    aria-expanded="false">
                📋 View Details
            </button>

            ${canReview ? `
            <button class="btn btn-sm" style="background:var(--color-warning-bg); color:#a16207; border:1px solid rgba(161,98,7,0.2); display:flex; align-items:center; gap:0.375rem;"
                    onclick="toggleReview(${order.orderId})" id="review-btn-${order.orderId}">
                ⭐ Write Review
            </button>` : ''}

            <a href="restaurants.html" class="btn btn-primary btn-sm" style="margin-left:auto; display:flex; align-items:center; gap:0.375rem;">
                🔁 Reorder
            </a>
        </div>

        <!-- Expandable Details Panel -->
        <div class="order-details-panel" id="details-panel-${order.orderId}">
            <div style="color:var(--color-text-muted); font-size:0.875rem;">Loading details...</div>
        </div>

        <!-- Review Form (DELIVERED orders only) -->
        ${canReview ? `
        <div class="order-details-panel review-form-wrap" id="review-panel-${order.orderId}" style="display:none; border-top:1px solid var(--color-border-light); padding:1.5rem; background:var(--color-bg);">
            <h4>⭐ Rate Your Experience</h4>
            <p style="font-size:0.85rem; color:var(--color-text-muted); margin-bottom:1rem;">
                Share your feedback for Order #${order.orderId} from ${escapeHtml(order.restaurantName || 'this restaurant')}.
            </p>

            <!-- Star Rating -->
            <div style="margin-bottom:1rem;">
                <div class="detail-section-title">Rating *</div>
                <div class="star-rating" id="stars-${order.orderId}" role="radiogroup" aria-label="Rating">
                    ${[5,4,3,2,1].map(n => `
                        <input type="radio" id="star-${order.orderId}-${n}" name="rating-${order.orderId}" value="${n}">
                        <label for="star-${order.orderId}-${n}" title="${n} star${n>1?'s':''}" aria-label="${n} star${n>1?'s':''}">★</label>
                    `).join('')}
                </div>
            </div>

            <!-- Comment -->
            <div class="form-group" style="margin-bottom:1rem;">
                <div class="detail-section-title">Comment (optional)</div>
                <textarea class="form-input" id="review-comment-${order.orderId}"
                          rows="3" placeholder="What did you love? Any suggestions?"
                          style="resize:vertical;"></textarea>
            </div>

            <button class="review-submit-btn" onclick="submitReview(${order.orderId})" id="review-submit-btn-${order.orderId}">
                Submit Review
            </button>
            <button class="btn btn-outline btn-sm" onclick="toggleReview(${order.orderId})" style="margin-left:0.5rem;">Cancel</button>
            <div class="review-result" id="review-result-${order.orderId}"></div>
        </div>` : ''}

    </div>`;
}

// ─────────────────────────────────────────────
//  FETCH PAYMENT STATUS
// ─────────────────────────────────────────────

async function fetchAndSetPaymentStatus(orderId) {
    const badge = document.getElementById(`pay-status-${orderId}`);
    if (!badge) return;

    const result = await api.get(`/payments?orderId=${orderId}`);
    if (result.success) {
        badge.outerHTML = paymentBadgeHTML(result.data);
    } else {
        const el = document.getElementById(`pay-status-${orderId}`);
        if (el) el.outerHTML = `<span class="pay-badge pay-NONE">⬜ No Payment</span>`;
    }
}

// ─────────────────────────────────────────────
//  VIEW DETAILS (expandable panel)
// ─────────────────────────────────────────────

async function toggleDetails(orderId) {
    const panel = document.getElementById(`details-panel-${orderId}`);
    const btn   = document.getElementById(`details-btn-${orderId}`);
    if (!panel) return;

    const isOpen = panel.classList.contains('open');
    panel.classList.toggle('open', !isOpen);
    if (btn) {
        btn.setAttribute('aria-expanded', String(!isOpen));
        btn.innerHTML = !isOpen ? '📋 Hide Details' : '📋 View Details';
    }

    if (!isOpen && panel.dataset.loaded !== 'true') {
        panel.dataset.loaded = 'true';
        panel.innerHTML = `<p style="color:var(--color-text-muted); font-size:0.875rem;">Loading...</p>`;
        await loadOrderDetails(orderId, panel);
    }
}

async function loadOrderDetails(orderId, panel) {
    const result = await api.get(`/orders/${orderId}`);

    if (!result.success) {
        panel.innerHTML = `<p style="color:var(--color-error); font-size:0.875rem;">
            ⚠️ ${escapeHtml(result.message || 'Failed to load details.')}</p>`;
        return;
    }

    const { order, items, restaurantName } = result.data;

    const itemsRows = (items || []).map(item => {
        const subtotal = item.subtotal != null
            ? item.subtotal
            : (parseFloat(item.unitPrice) * item.quantity);
        return `
            <tr>
                <td>${escapeHtml(item.itemName)}</td>
                <td style="text-align:center;">${item.quantity}</td>
                <td>${formatCurrency(item.unitPrice)}</td>
                <td class="item-subtotal">${formatCurrency(subtotal)}</td>
            </tr>`;
    }).join('');

    panel.innerHTML = `
        <div class="order-details-grid">
            <div>
                <div class="detail-section-title">Delivery Address</div>
                <div class="detail-value">${escapeHtml(order.deliveryAddress)}</div>
            </div>
            <div>
                <div class="detail-section-title">Restaurant</div>
                <div class="detail-value">${escapeHtml(restaurantName || 'N/A')}</div>
            </div>
            <div>
                <div class="detail-section-title">Order Status</div>
                <div class="detail-value">${statusBadgeHTML(order.status)}</div>
            </div>
            <div>
                <div class="detail-section-title">Order Date</div>
                <div class="detail-value">${order.createdAt ? formatDate(order.createdAt) : '—'}</div>
            </div>
        </div>

        <div>
            <div class="detail-section-title" style="margin-bottom:0.5rem;">Items Ordered</div>
            <table class="items-mini-table">
                <thead>
                    <tr>
                        <th>Item</th>
                        <th style="text-align:center;">Qty</th>
                        <th>Unit Price</th>
                        <th>Subtotal</th>
                    </tr>
                </thead>
                <tbody>${itemsRows || '<tr><td colspan="4" style="color:var(--color-text-muted); padding:1rem;">No items found.</td></tr>'}</tbody>
            </table>
            <div style="text-align:right; font-size:1rem; font-weight:700; color:var(--color-primary); margin-top:0.75rem; padding-top:0.75rem; border-top:2px dashed var(--color-border);">
                Total: ${formatCurrency(order.totalAmount)}
            </div>
        </div>`;
}

// ─────────────────────────────────────────────
//  REVIEW FORM
// ─────────────────────────────────────────────

function toggleReview(orderId) {
    const reviewPanel = document.getElementById(`review-panel-${orderId}`);
    if (!reviewPanel) return;
    const isOpen = reviewPanel.style.display !== 'none';
    reviewPanel.style.display = isOpen ? 'none' : 'block';

    const btn = document.getElementById(`review-btn-${orderId}`);
    if (btn) btn.innerHTML = isOpen ? '⭐ Write Review' : '✕ Cancel Review';
}

async function submitReview(orderId) {
    const ratingInput  = document.querySelector(`input[name="rating-${orderId}"]:checked`);
    const commentInput = document.getElementById(`review-comment-${orderId}`);
    const submitBtn    = document.getElementById(`review-submit-btn-${orderId}`);
    const resultDiv    = document.getElementById(`review-result-${orderId}`);

    // Clear previous result
    if (resultDiv) { resultDiv.className = 'review-result'; resultDiv.textContent = ''; }

    const rating  = ratingInput  ? parseInt(ratingInput.value)   : 0;
    const comment = commentInput ? commentInput.value.trim()      : '';

    if (rating < 1 || rating > 5) {
        showReviewResult(orderId, 'Please select a star rating (1–5).', false);
        return;
    }

    if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Submitting...'; }

    const result = await api.post('/reviews/submit', {
        orderId: orderId,
        rating:  rating,
        comment: comment
    });

    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Submit Review'; }

    if (result.success) {
        showReviewResult(orderId, '✅ ' + (result.message || 'Review submitted! Thank you.'), true);

        // Disable the review button so they can't re-open the form
        const reviewBtn = document.getElementById(`review-btn-${orderId}`);
        if (reviewBtn) {
            reviewBtn.disabled   = true;
            reviewBtn.innerHTML  = '⭐ Review Submitted';
            reviewBtn.style.opacity = '0.6';
            reviewBtn.style.cursor  = 'not-allowed';
        }
    } else {
        showReviewResult(orderId, '❌ ' + (result.message || 'Could not submit review.'), false);
    }
}

function showReviewResult(orderId, message, isSuccess) {
    const el = document.getElementById(`review-result-${orderId}`);
    if (!el) return;
    el.textContent = message;
    el.className   = `review-result ${isSuccess ? 'success' : 'error'}`;
}

// ─────────────────────────────────────────────
//  UTILITIES
// ─────────────────────────────────────────────

function formatDate(ts) {
    if (!ts) return '—';
    try {
        // ts may be a number (ms since epoch) or an ISO string
        return new Date(ts).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit', hour12: true
        });
    } catch {
        return String(ts);
    }
}

function truncate(str, maxLen) {
    if (!str) return '';
    return str.length > maxLen ? str.slice(0, maxLen) + '…' : str;
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
    loadOrders();

    // Update cart badge in nav
    api.get('/cart').then(result => {
        if (result.success && result.data && result.data.items) {
            const count = result.data.items.length;
            const badge = document.getElementById('nav-cart-count');
            if (badge) {
                badge.textContent  = count;
                badge.style.display = count > 0 ? 'flex' : 'none';
            }
        }
    });
});
