import './style.css';

const USERS_KEY = 'resumeradar_users';
const SESSION_KEY = 'resumeradar_session';

// If already logged in, redirect straight to dashboard
if (localStorage.getItem(SESSION_KEY)) {
    window.location.replace('/');
}

// Seed demo accounts if users key does not exist yet
async function seedDefaultUsers() {
    let existing = JSON.parse(localStorage.getItem(USERS_KEY));
    if (!existing) {
        existing = {};
        const studentHash = await hashPassword('Password123!');
        const mentorHash = await hashPassword('Password123!');
        existing['student@resumeradar.com'] = {
            name: 'Alex Learner',
            role: 'student',
            passwordHash: studentHash
        };
        existing['mentor@resumeradar.com'] = {
            name: 'Sarah Mentor',
            role: 'mentor',
            passwordHash: mentorHash
        };
        localStorage.setItem(USERS_KEY, JSON.stringify(existing));
    }
}
seedDefaultUsers();

let mode = 'login'; // 'login' or 'signup'

const form = document.getElementById('authForm');
const nameGroup = document.getElementById('nameGroup');
const roleGroup = document.getElementById('roleGroup');
const roleSelect = document.getElementById('roleSelect');
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

// SHA-256 password hashing
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

function setMode(newMode) {
    mode = newMode;
    alertBox.style.display = 'none';
    if (mode === 'signup') {
        nameGroup.style.display = 'block';
        roleGroup.style.display = 'block';
        submitBtn.innerText = 'Create Account';
        subtitle.innerText = 'Create your free ResumeRadar account';
        toggleText.innerText = 'Already have an account?';
        toggleLink.innerText = 'Login';
    } else {
        nameGroup.style.display = 'none';
        roleGroup.style.display = 'none';
        submitBtn.innerText = 'Login';
        subtitle.innerText = 'Login to open your Resume Health Dashboard';
        toggleText.innerText = 'New here?';
        toggleLink.innerText = 'Create an account';
    }
}

toggleLink.addEventListener('click', (e) => {
    e.preventDefault();
    setMode(mode === 'login' ? 'signup' : 'login');
});

// Demo autofill buttons
const btnStudent = document.getElementById('btnFillStudent');
if (btnStudent) {
    btnStudent.addEventListener('click', () => {
        setMode('login');
        document.getElementById('email').value = 'student@resumeradar.com';
        document.getElementById('password').value = 'Password123!';
    });
}

const btnMentor = document.getElementById('btnFillMentor');
if (btnMentor) {
    btnMentor.addEventListener('click', () => {
        setMode('login');
        document.getElementById('email').value = 'mentor@resumeradar.com';
        document.getElementById('password').value = 'Password123!';
    });
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = document.getElementById('email').value.trim().toLowerCase();
    const password = document.getElementById('password').value;
    const fullName = document.getElementById('fullName').value.trim();
    const role = roleSelect ? roleSelect.value : 'student';

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

        // Save new user account
        users[email] = {
            name: fullName || email.split('@')[0],
            role: role || 'student',
            passwordHash: passwordHash
        };
        localStorage.setItem(USERS_KEY, JSON.stringify(users));

        // Switch to login form with success message (do NOT auto-login)
        setMode('login');
        document.getElementById('password').value = '';
        showAlert('Account created successfully! Please login with your password.', 'success');
    } else {
        // Login mode
        const user = users[email];
        if (!user || user.passwordHash !== passwordHash) {
            showAlert('Invalid email or password. New user? Click "Create an account" below.', 'danger');
            return;
        }

        // Save session object with role
        const sessionData = {
            email: email,
            name: user.name || email.split('@')[0],
            role: user.role || 'student'
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(sessionData));

        // Go to dashboard
        window.location.href = '/';
    }
});
