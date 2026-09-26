/**
 * auth.js — Registration and Login page logic.
 *
 * Handles:
 *  - Client-side validation
 *  - Fetch API calls to AuthServlet
 *  - Session storage on success
 *  - Role-based redirect
 *  - Error / success message display
 */

// ─────────────────────────────────────────────
//  SHARED UTILITIES
// ─────────────────────────────────────────────

function showMessage(containerId, text, type) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.className = 'auth-message ' + type;
    el.innerHTML = `<span>${type === 'success' ? '✅' : '❌'}</span> ${text}`;
    el.style.display = 'flex';
}

function hideMessage(containerId) {
    const el = document.getElementById(containerId);
    if (el) el.style.display = 'none';
}

function setLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.disabled = loading;
    if (loading) {
        btn.classList.add('loading');
        btn.dataset.originalText = btn.innerHTML;
        btn.innerHTML = '<span>Please wait...</span>';
    } else {
        btn.classList.remove('loading');
        btn.innerHTML = btn.dataset.originalText || 'Submit';
    }
}

function markFieldError(inputId, show) {
    const el = document.getElementById(inputId);
    if (!el) return;
    if (show) el.classList.add('error');
    else       el.classList.remove('error');
}

// ─────────────────────────────────────────────
//  REGISTRATION PAGE
// ─────────────────────────────────────────────

function initRegisterPage() {
    const form = document.getElementById('register-form');
    if (!form) return;

    // If already logged in, redirect
    if (Session.isLoggedIn()) {
        redirectByRole(Session.getRole());
        return;
    }

    // Toggle password visibility (robustly find elements within this form)
    const passwordInput = form.querySelector('input[type="password"], input[name="password"]');
    const toggleBtn = form.querySelector('.password-toggle');
    if (passwordInput && toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
            toggleBtn.textContent = isPassword ? '🙈' : '👁️';
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideMessage('register-message');

        // ── Collect values robustly using form elements ──
        const formData = new FormData(form);
        const name     = (formData.get('name') || '').trim();
        const email    = (formData.get('email') || '').trim();
        const password = formData.get('password') || '';
        const phone    = (formData.get('phone') || '').trim();
        const role     = formData.get('role') || '';

        // Helper to mark field error by input name
        const markErrorByName = (inputName, hasError) => {
            const input = form.querySelector(`[name="${inputName}"]`);
            if (input) markFieldError(input.id, hasError);
        };

        // ── Client-side Validation ──
        let hasError = false;

        if (!name) {
            markErrorByName('name', true);
            hasError = true;
        } else markErrorByName('name', false);

        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            markErrorByName('email', true);
            hasError = true;
        } else markErrorByName('email', false);

        if (!password || password.length < 6) {
            markErrorByName('password', true);
            hasError = true;
        } else markErrorByName('password', false);

        if (!role) {
            showMessage('register-message', 'Please select a role.', 'error');
            return;
        }

        if (hasError) {
            showMessage('register-message',
                'Please fix the highlighted fields. Password must be at least 6 characters.',
                'error');
            return;
        }

        // ── API Call ──
        setLoading('register-btn', true);

        const result = await api.post('/auth/register', { name, email, password, phone, role });

        setLoading('register-btn', false);

        if (result.success) {
            showMessage('register-message', result.message, 'success');
            form.reset();
            // Redirect to login after a brief pause
            setTimeout(() => {
                window.location.href = CONTEXT + '/pages/login.html';
            }, 1800);
        } else {
            showMessage('register-message', result.message || 'Registration failed.', 'error');
        }
    });

    // Clear error styling on input
    form.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('input', () => markFieldError(input.id, false));
    });
}

// ─────────────────────────────────────────────
//  LOGIN PAGE
// ─────────────────────────────────────────────

function initLoginPage() {
    const form = document.getElementById('login-form');
    if (!form) return;

    // If already logged in, redirect
    if (Session.isLoggedIn()) {
        redirectByRole(Session.getRole());
        return;
    }

    const passwordInput = form.querySelector('input[type="password"], input[name="password"]');
    const toggleBtn = form.querySelector('.password-toggle');
    if (passwordInput && toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
            toggleBtn.textContent = isPassword ? '🙈' : '👁️';
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideMessage('login-message');

        const formData = new FormData(form);
        const email    = (formData.get('email') || '').trim();
        const password = formData.get('password') || '';

        const markErrorByName = (inputName, hasError) => {
            const input = form.querySelector(`[name="${inputName}"]`);
            if (input) markFieldError(input.id, hasError);
        };

        // ── Client-side Validation ──
        let hasError = false;

        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            markErrorByName('email', true);
            hasError = true;
        } else markErrorByName('email', false);

        if (!password) {
            markErrorByName('password', true);
            hasError = true;
        } else markErrorByName('password', false);

        if (hasError) {
            showMessage('login-message', 'Please enter a valid email and password.', 'error');
            return;
        }

        // ── API Call ──
        setLoading('login-btn', true);

        const result = await api.post('/auth/login', { email, password });

        setLoading('login-btn', false);

        if (result.success && result.data) {
            // Save to local storage for fast UI reads
            Session.save(result.data);

            showMessage('login-message', 'Login successful! Redirecting...', 'success');

            // Redirect based on role (from the server response)
            setTimeout(() => {
                redirectByRole(result.data.role);
            }, 800);

        } else {
            showMessage('login-message', result.message || 'Login failed.', 'error');
        }
    });

    form.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('input', () => markFieldError(input.id, false));
    });
}

// ─────────────────────────────────────────────
//  PASSWORD VISIBILITY TOGGLE
// ─────────────────────────────────────────────

function setupPasswordToggle(inputId, toggleId) {
    const input  = document.getElementById(inputId);
    const toggle = document.getElementById(toggleId);
    if (!input || !toggle) return;

    toggle.addEventListener('click', () => {
        const isPassword = input.type === 'password';
        input.type  = isPassword ? 'text' : 'password';
        toggle.textContent = isPassword ? '🙈' : '👁️';
    });
}

// ─────────────────────────────────────────────
//  AUTO-INIT on DOMContentLoaded
// ─────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    initRegisterPage();
    initLoginPage();
});
