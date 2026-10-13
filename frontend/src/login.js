import './style.css';

const USERS_KEY = 'resumeradar_users';
const SESSION_KEY = 'resumeradar_session';

// If already logged in, go straight to the dashboard
if (localStorage.getItem(SESSION_KEY)) {
    window.location.replace('/');
}

let mode = 'login'; // 'login' or 'signup'

const form = document.getElementById('authForm');
const nameGroup = document.getElementById('nameGroup');
const submitBtn = document.getElementById('submitBtn');
const subtitle = document.getElementById('formSubtitle');
const toggleText = document.getElementById('toggleText');
const toggleLink = document.getElementById('toggleLink');
const alertBox = document.getElementById('statusAlert');

function showAlert(text, type) {
    alertBox.className = 'alert alert-' + type;
    alertBox.innerText = text;
    alertBox.style.display = 'flex';
}

// Hash the password so it is not stored as plain text
async function hashPassword(text) {
    const data = new TextEncoder().encode(text);
    const buffer = await crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(buffer))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');
}

function getUsers() {
    return JSON.parse(localStorage.getItem(USERS_KEY)) || {};
}

toggleLink.addEventListener('click', (e) => {
    e.preventDefault();
    alertBox.style.display = 'none';
    if (mode === 'login') {
        mode = 'signup';
        nameGroup.style.display = 'block';
        submitBtn.innerText = 'Create Account';
        subtitle.innerText = 'Create your free ResumeRadar account';
        toggleText.innerText = 'Already have an account?';
        toggleLink.innerText = 'Login';
    } else {
        mode = 'login';
        nameGroup.style.display = 'none';
        submitBtn.innerText = 'Login';
        subtitle.innerText = 'Login to open your Resume Health Dashboard';
        toggleText.innerText = 'New here?';
        toggleLink.innerText = 'Create an account';
    }
});

form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = document.getElementById('email').value.trim().toLowerCase();
    const password = document.getElementById('password').value;
    const fullName = document.getElementById('fullName').value.trim();

    if (password.length < 6) {
        showAlert('Password must be at least 6 characters.', 'warning');
        return;
    }

    const users = getUsers();
    const passwordHash = await hashPassword(password);

    if (mode === 'signup') {
        if (users[email]) {
            showAlert('This email is already registered. Please login.', 'warning');
            return;
        }
        users[email] = { name: fullName || email, passwordHash };
        localStorage.setItem(USERS_KEY, JSON.stringify(users));
        localStorage.setItem(SESSION_KEY, email);
        window.location.href = '/';
    } else {
        if (!users[email] || users[email].passwordHash !== passwordHash) {
            showAlert('Wrong email or password. New user? Create an account first.', 'danger');
            return;
        }
        localStorage.setItem(SESSION_KEY, email);
        window.location.href = '/';
    }
});
