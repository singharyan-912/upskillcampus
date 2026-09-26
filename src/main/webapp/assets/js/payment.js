/**
 * payment.js
 */

let currentOrderId = null;

async function loadPaymentDetails() {
    const urlParams = new URLSearchParams(window.location.search);
    currentOrderId = urlParams.get('orderId');

    const loadingEl = document.getElementById('payment-loading');
    const mainEl    = document.getElementById('payment-main');

    if (!currentOrderId) {
        if (loadingEl) {
            loadingEl.innerHTML = `
                <div class="empty-state">
                    <h3>Invalid Order</h3>
                    <p>No order ID provided.</p>
                    <a href="orders.html" class="btn btn-primary">Go to My Orders</a>
                </div>`;
        }
        return;
    }

    const result = await api.get(`/orders/${currentOrderId}`);

    if (!result.success) {
        if (loadingEl) {
            loadingEl.innerHTML = `
                <div class="empty-state">
                    <h3>Could not load order</h3>
                    <p>${escapeHtml(result.message || 'Order not found or access denied.')}</p>
                    <a href="orders.html" class="btn btn-primary">Go to My Orders</a>
                </div>`;
        }
        return;
    }

    const { order } = result.data;

    document.getElementById('display-order-id').textContent = order.orderId;
    document.getElementById('display-order-total').textContent = formatCurrency(order.totalAmount);

    if (loadingEl) loadingEl.style.display = 'none';
    if (mainEl)    mainEl.style.display    = 'block';
}

function initPaymentToggle() {
    const radios = document.querySelectorAll('input[name="payment-method"]');
    radios.forEach(radio => {
        radio.addEventListener('change', () => {
            const isOnline = document.getElementById('pay-online')?.checked;
            const demoPanel = document.getElementById('demo-panel');
            const codInfo   = document.getElementById('cod-info');
            const btn       = document.getElementById('process-payment-btn');
            
            if (demoPanel) demoPanel.classList.toggle('visible', isOnline);
            if (codInfo)   codInfo.style.display = isOnline ? 'none' : 'block';
            if (btn)       btn.textContent = isOnline ? 'Pay Now' : 'Confirm Order';
        });
    });
}

function showError(message) {
    const box  = document.getElementById('payment-error-box');
    const text = document.getElementById('payment-error-text');
    if (box && text) {
        text.textContent = message;
        box.style.display = 'flex';
    } else {
        showToast(message, 'error');
    }
}

function hideError() {
    const box = document.getElementById('payment-error-box');
    if (box) box.style.display = 'none';
}

async function processPayment() {
    hideError();

    const methodInput     = document.querySelector('input[name="payment-method"]:checked');
    const simulateFail    = document.getElementById('simulate-failure');
    const btn             = document.getElementById('process-payment-btn');

    const method          = methodInput  ? methodInput.value : 'ONLINE';
    const simulateFailure = simulateFail ? simulateFail.checked : false;

    const originalHtml = btn.innerHTML;
    btn.disabled   = true;
    btn.innerHTML  = '⏳ Processing...';

    try {
        const payResult = await api.post('/payments/process', {
            orderId:                parseInt(currentOrderId),
            method:                 method,
            simulateSuccess:        !simulateFailure,
            simulateSuccessProvided: true
        });

        if (!payResult.success) {
            showError(payResult.message || 'Payment processing failed.');
            btn.innerHTML = originalHtml;
            btn.disabled = false;
            return;
        }

        const payment = payResult.data;
        const paymentStatus = payment ? payment.paymentStatus : 'UNKNOWN';

        if (method === 'ONLINE' && paymentStatus === 'FAILED') {
            showSuccessModal(currentOrderId, 
                `❌ Demo payment FAILED — TXN: ${payment.transactionRef}. Your order was placed but is unpaid.`,
                'Payment Failed (Demo)'
            );
        } else {
            let payMsg = '';
            if (method === 'COD') {
                payMsg = '💵 Cash on Delivery — pay when your order arrives.';
            } else {
                payMsg = `✅ Online payment SUCCESS — TXN: ${payment.transactionRef}`;
            }
            showSuccessModal(currentOrderId, payMsg, 'Order Confirmed!');
        }

        btn.innerHTML = '✅ Done';

    } catch (e) {
        showError('An unexpected error occurred. Please refresh and try again.');
        btn.disabled  = false;
        btn.innerHTML = originalHtml;
        console.error(e);
    }
}

function showSuccessModal(orderId, message, title) {
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-order-id').textContent = `Order #${orderId}`;
    document.getElementById('modal-payment-msg').textContent = message;

    const modal = document.getElementById('success-modal');
    if (modal) modal.classList.add('visible');

    const trackBtn = document.getElementById('modal-track-btn');
    if (trackBtn) {
        trackBtn.onclick = () => {
            window.location.href = CONTEXT + '/pages/orders.html';
        };
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', () => {
    if (!requireAuth(['CUSTOMER'])) return;
    
    initPaymentToggle();
    loadPaymentDetails();

    const btn = document.getElementById('process-payment-btn');
    if (btn) btn.addEventListener('click', processPayment);
});
