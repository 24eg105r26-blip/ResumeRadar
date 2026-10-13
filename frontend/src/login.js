import './style.css';

const USERS_KEY = 'resumeradar_users';
const SESSION_KEY = 'resumeradar_session';

// If already logged in, redirect directly to dashboard
if (localStorage.getItem(SESSION_KEY)) {
    window.location.replace('/');
}

// Pre-computed SHA-256 for 'Password123!'
const DEMO_PASSWORD_HASH = 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea';

const DEFAULT_USERS = {
    'student@resumeradar.com': {
        name: 'Alex Learner',
        role: 'student',
        passwordHash: DEMO_PASSWORD_HASH
    },
    'mentor@resumeradar.com': {
        name: 'Sarah Mentor',
        role: 'mentor',
        passwordHash: DEMO_PASSWORD_HASH
    }
};

// Always retrieve users with guaranteed demo accounts
function getUsers() {
    let users = {};
    try {
        users = JSON.parse(localStorage.getItem(USERS_KEY)) || {};
    } catch (_) {
        users = {};
    }

    let needsSave = false;
    for (const [email, demoUser] of Object.entries(DEFAULT_USERS)) {
        if (!users[email]) {
            users[email] = demoUser;
            needsSave = true;
        }
    }

    if (needsSave) {
        localStorage.setItem(USERS_KEY, JSON.stringify(users));
    }
    return users;
}
// Seed immediately
getUsers();

let selectedRole = 'student'; // 'student' or 'mentor'
let mode = 'login';          // 'login' or 'signup'

const form = document.getElementById('authForm');
const nameGroup = document.getElementById('nameGroup');
const fullNameInput = document.getElementById('fullName');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const submitBtn = document.getElementById('submitBtn');
const subtitle = document.getElementById('formSubtitle');
const toggleText = document.getElementById('toggleText');
const toggleLink = document.getElementById('toggleLink');
const alertBox = document.getElementById('statusAlert');
const tabStudent = document.getElementById('tabStudent');
const tabMentor = document.getElementById('tabMentor');
const btnQuickDemo = document.getElementById('btnQuickDemo');

function showAlert(text, type) {
    alertBox.className = 'alert alert-' + type;
    alertBox.innerText = text;
    alertBox.style.display = 'flex';
}

async function hashPassword(text) {
    const data = new TextEncoder().encode(text);
    const buffer = await crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(buffer))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');
}

function updateUI() {
    alertBox.style.display = 'none';

    // Update Portal Tabs
    if (selectedRole === 'student') {
        tabStudent.className = 'btn btn-primary btn-sm';
        tabMentor.className = 'btn btn-outline btn-sm';
        if (btnQuickDemo) {
            btnQuickDemo.innerText = '👉 Click to sign in as Demo Student (Alex Learner)';
        }
    } else {
        tabMentor.className = 'btn btn-teal btn-sm';
        tabStudent.className = 'btn btn-outline btn-sm';
        if (btnQuickDemo) {
            btnQuickDemo.innerText = '👉 Click to sign in as Demo Mentor (Sarah Mentor)';
        }
    }

    // Update Mode (Login vs Signup)
    if (mode === 'signup') {
        nameGroup.style.display = 'block';
        submitBtn.innerText = `Create ${selectedRole === 'mentor' ? 'Mentor' : 'Student'} Account`;
        subtitle.innerText = selectedRole === 'mentor' 
            ? 'Create your free ResumeRadar Mentor account' 
            : 'Create your free ResumeRadar Student account';
        toggleText.innerText = 'Already have an account?';
        toggleLink.innerText = 'Login';
    } else {
        nameGroup.style.display = 'none';
        submitBtn.innerText = `Login as ${selectedRole === 'mentor' ? 'Mentor' : 'Student'}`;
        subtitle.innerText = selectedRole === 'mentor'
            ? 'Mentor Portal — Track student score curves & leave guidance'
            : 'Student Portal — Scan resumes and view health scores';
        toggleText.innerText = 'New here?';
        toggleLink.innerText = 'Create an account';
    }
}

// Tab click listeners
tabStudent.addEventListener('click', () => {
    selectedRole = 'student';
    updateUI();
});

tabMentor.addEventListener('click', () => {
    selectedRole = 'mentor';
    updateUI();
});

// Toggle login / signup mode
toggleLink.addEventListener('click', (e) => {
    e.preventDefault();
    mode = (mode === 'login') ? 'signup' : 'login';
    updateUI();
});

// Quick demo sign-in button
if (btnQuickDemo) {
    btnQuickDemo.addEventListener('click', () => {
        const demoEmail = selectedRole === 'student' ? 'student@resumeradar.com' : 'mentor@resumeradar.com';
        const users = getUsers();
        const demoUser = users[demoEmail];

        emailInput.value = demoEmail;
        passwordInput.value = 'Password123!';

        // Direct login
        const sessionData = {
            email: demoEmail,
            name: demoUser.name,
            role: demoUser.role
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(sessionData));
        showAlert(`Signing in as ${demoUser.name}...`, 'info');
        setTimeout(() => {
            window.location.href = '/';
        }, 300);
    });
}

// Form Submission
form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email = emailInput.value.trim().toLowerCase();
    const password = passwordInput.value;
    const fullName = fullNameInput ? fullNameInput.value.trim() : '';

    if (password.length < 6) {
        showAlert('Password must be at least 6 characters.', 'warning');
        return;
    }

    const users = getUsers();
    const passwordHash = await hashPassword(password);

    if (mode === 'signup') {
        if (users[email]) {
            showAlert('This email is already registered. Please login with your password.', 'warning');
            mode = 'login';
            updateUI();
            return;
        }

        // Save new user account with chosen role
        users[email] = {
            name: fullName || email.split('@')[0],
            role: selectedRole,
            passwordHash: passwordHash
        };
        localStorage.setItem(USERS_KEY, JSON.stringify(users));

        // Switch to login view (do NOT auto-login)
        mode = 'login';
        passwordInput.value = '';
        updateUI();
        showAlert('Account created successfully! Please enter your password to login.', 'success');

    } else {
        // Login mode
        const user = users[email];

        if (!user) {
            showAlert('No account found with this email. Click "Create an account" below to sign up.', 'warning');
            return;
        }

        if (user.passwordHash !== passwordHash) {
            showAlert('Incorrect password. Please verify and try again.', 'danger');
            return;
        }

        // Save session object with role
        const sessionData = {
            email: email,
            name: user.name || email.split('@')[0],
            role: user.role || selectedRole
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(sessionData));

        showAlert(`Welcome back, ${user.name}! Redirecting to dashboard...`, 'success');
        setTimeout(() => {
            window.location.href = '/';
        }, 300);
    }
});

// Initial UI setup
updateUI();
