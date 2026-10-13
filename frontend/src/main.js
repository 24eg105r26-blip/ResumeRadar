import './style.css';
import { ResumeRadar } from './resumeradar.js';

window.ResumeRadar = ResumeRadar;

const SESSION_KEY = 'resumeradar_session';
const FEEDBACK_KEY = 'resumeradar_mentor_feedback';
const LINKS_KEY = 'resumeradar_mentor_links';
const SHARECODE_KEY = 'resumeradar_student_sharecode';

// Parse active session
function getSession() {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch (_) {
        // Fallback if session was saved as plain string email
        return { email: raw, name: raw.split('@')[0], role: 'student' };
    }
}

const currentSession = getSession();

// Seed initial student sharecode and demo mentor link if not present
function initMentorData() {
    if (!localStorage.getItem(SHARECODE_KEY)) {
        localStorage.setItem(SHARECODE_KEY, 'RADAR-DEMO-2026');
    }
    if (!localStorage.getItem(FEEDBACK_KEY)) {
        const initialNotes = [
            {
                mentorName: 'Sarah Mentor',
                studentEmail: 'student@resumeradar.com',
                note: 'Great start! In your next version, quantify your database latency improvements and add the Docker tool tags.',
                date: new Date(Date.now() - 3600000 * 4).toLocaleDateString()
            }
        ];
        localStorage.setItem(FEEDBACK_KEY, JSON.stringify(initialNotes));
    }
    if (!localStorage.getItem(LINKS_KEY)) {
        const initialLinks = [
            {
                mentorEmail: 'mentor@resumeradar.com',
                studentEmail: 'student@resumeradar.com',
                studentName: 'Alex Learner',
                shareCode: 'RADAR-DEMO-2026'
            }
        ];
        localStorage.setItem(LINKS_KEY, JSON.stringify(initialLinks));
    }
}
initMentorData();

// Student real version history (starts clean or from localStorage)
let versionsHistory = JSON.parse(localStorage.getItem('resumeradar_versions')) || [];
let currentScanResult = null;
let currentSelectedFile = null;

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    renderUserBar();
    renderTrend();

    // Check if user is a mentor or student
    if (currentSession && currentSession.role === 'mentor') {
        initMentorDashboard();
    } else {
        initStudentDashboard();
    }

    setupDropzone();
    setupEventListeners();
}

function renderUserBar() {
    if (!currentSession) return;
    const userBadge = document.getElementById('userNavBadge');
    if (userBadge) {
        const roleLabel = currentSession.role === 'mentor' ? '🧑‍🏫 Mentor' : '👤 Student';
        userBadge.innerText = `${currentSession.name} (${roleLabel})`;
    }

    // If mentor, hide student mentor-code button and show mentor banner instead
    const mentorBtn = document.getElementById('btnMentorCode');
    if (mentorBtn) {
        if (currentSession.role === 'mentor') {
            mentorBtn.style.display = 'none';
        } else {
            mentorBtn.style.display = 'inline-flex';
        }
    }
}

function initStudentDashboard() {
    // Show student dashboard
    const mentorPanel = document.getElementById('mentorHubPanel');
    if (mentorPanel) mentorPanel.style.display = 'none';

    const studentGrid = document.querySelector('.dashboard-grid');
    if (studentGrid) studentGrid.style.display = 'grid';

    // Set initial clean gauge state
    if (versionsHistory.length > 0) {
        const latest = versionsHistory[versionsHistory.length - 1];
        ResumeRadar.updateScoreGauge(latest.healthScore);
        const verBadge = document.getElementById('currentVersionBadge');
        if (verBadge) verBadge.innerText = `v${latest.versionNumber}`;
        const matchPctEl = document.getElementById('statMatchPct');
        if (matchPctEl && latest.matchPercentage !== undefined) {
            matchPctEl.innerText = `${latest.matchPercentage}%`;
        }
    } else {
        ResumeRadar.updateScoreGauge(null);
    }

    renderStudentMentorFeedback();
}

function initMentorDashboard() {
    const mentorPanel = document.getElementById('mentorHubPanel');
    if (mentorPanel) mentorPanel.style.display = 'block';

    renderLinkedStudentsList();
}

function renderTrend() {
    ResumeRadar.renderScoreTrendSvg('trendChartContainer', versionsHistory);
    const countEl = document.getElementById('versionCountText');
    if (countEl) {
        countEl.innerText = `${versionsHistory.length} versions recorded`;
    }
}

function setupDropzone() {
    const dropzone = document.getElementById('resumeDropzone');
    const fileInput = document.getElementById('resumeFileInput');
    if (!dropzone || !fileInput) return;

    fileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files.length > 0) {
            handleSelectedFile(e.target.files[0]);
        }
    });

    ['dragenter', 'dragover'].forEach(name => {
        dropzone.addEventListener(name, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(name => {
        dropzone.addEventListener(name, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.remove('dragover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            handleSelectedFile(e.dataTransfer.files[0]);
        }
    });

    dropzone.addEventListener('click', () => {
        fileInput.click();
    });
}

function handleSelectedFile(file) {
    currentSelectedFile = file;
    const dropText = document.getElementById('dropzoneText');
    if (dropText) {
        dropText.innerHTML = `<strong>Selected:</strong> ${escapeHtml(file.name)} <span style="color:var(--primary); font-weight:600;">(${(file.size / 1024).toFixed(1)} KB)</span>`;
    }
    const dropSub = document.querySelector('.dropzone-subtext');
    if (dropSub) {
        dropSub.innerText = 'File loaded! Click "⚡ Run On-Device Scan" below.';
    }
    showAlert(`Loaded "${file.name}". Click "⚡ Run On-Device Scan" to process on-device.`, 'info');
}

function setupEventListeners() {
    const scanBtn = document.getElementById('btnScan');
    if (scanBtn) scanBtn.addEventListener('click', performScan);

    const saveBtn = document.getElementById('btnSaveVersion');
    if (saveBtn) saveBtn.addEventListener('click', saveVersion);

    const sampleJdSelect = document.getElementById('savedJdSelect');
    if (sampleJdSelect) {
        sampleJdSelect.addEventListener('change', (e) => {
            const val = e.target.value;
            const jdArea = document.getElementById('jobDescriptionText');
            if (val === 'java') {
                jdArea.value = "We are seeking a Full Stack Java Developer proficient in Java, Spring Boot, MySQL, REST APIs, Git, Docker, and microservices architecture. Strong problem-solving and clean code fundamentals required.";
            } else if (val === 'react') {
                jdArea.value = "Looking for a Frontend Engineer with React, TypeScript, JavaScript, CSS, HTML5, REST API integration, and automated testing experience.";
            } else if (val === 'python') {
                jdArea.value = "Hiring a Backend Python Engineer proficient in Python, SQL, PostgreSQL, Docker, AWS, Git, and RESTful microservices.";
            }
        });
    }
}

async function performScan() {
    const jdText = document.getElementById('jobDescriptionText').value.trim();
    const hiddenText = document.getElementById('hiddenExtractedText').value.trim();

    if (!currentSelectedFile && !hiddenText) {
        showAlert("Please upload a resume file (PDF, DOCX, or TXT) first.", "danger");
        return;
    }
    if (!jdText) {
        showAlert("Please paste the target job description to match against.", "warning");
        return;
    }

    const progressEl = document.getElementById('parseProgress');
    progressEl.style.display = 'block';
    progressEl.innerText = "Extracting text strictly on-device in browser...";

    try {
        let resumeText = "";
        if (currentSelectedFile) {
            resumeText = await ResumeRadar.parseResumeFile(currentSelectedFile);
            document.getElementById('hiddenExtractedText').value = resumeText;
        } else {
            resumeText = hiddenText;
        }

        if (!resumeText || resumeText.length < 20) {
            throw new Error("Could not extract text. Please ensure document contains selectable text, not scanned image graphics.");
        }

        progressEl.innerText = "Matching skills dictionary and checking ATS format...";

        const jdSkills = ResumeRadar.extractSkillsFromJobDescription(jdText);
        const matchResult = ResumeRadar.compareResumeWithJob(resumeText, jdSkills);
        const atsIssues = ResumeRadar.checkAtsCompatibility(resumeText);
        const rewrites = ResumeRadar.generateRewriteSuggestions(resumeText, matchResult.matchedKeywords);
        const healthScore = ResumeRadar.calculateHealthScore(matchResult.matchPercentage, atsIssues.length);

        ResumeRadar.updateScoreGauge(healthScore);
        document.getElementById('statMatchPct').innerText = `${matchResult.matchPercentage}%`;
        document.getElementById('statAtsPenalty').innerText = `${atsIssues.length * 6} Pts (${atsIssues.length} issues)`;

        // Missing skills
        const missingEl = document.getElementById('missingSkillsContainer');
        if (matchResult.missingKeywords.length > 0) {
            missingEl.innerHTML = matchResult.missingKeywords
                .map(s => `<span class="skill-tag skill-missing">&minus; ${escapeHtml(s)}</span>`).join('');
        } else {
            missingEl.innerHTML = `<span class="skill-tag skill-matched">✓ All identified skills present in resume!</span>`;
        }

        // Matched skills
        const matchedEl = document.getElementById('matchedSkillsContainer');
        if (matchResult.matchedKeywords.length > 0) {
            matchedEl.innerHTML = matchResult.matchedKeywords
                .map(s => `<span class="skill-tag skill-matched">&check; ${escapeHtml(s)}</span>`).join('');
        } else {
            matchedEl.innerHTML = `<span style="font-size:0.875rem; color:var(--danger);">No target skills matched yet.</span>`;
        }

        // Rewrites
        const rewritesEl = document.getElementById('bulletRewritesContainer');
        if (rewrites.length > 0) {
            rewritesEl.innerHTML = rewrites.map(rw => `
                <div class="rewrite-card">
                    <div class="rewrite-original"><strong>Detected Passive:</strong> &ldquo;${escapeHtml(rw.original)}&rdquo;</div>
                    <div class="rewrite-suggestion"><strong>Google XYZ Suggestion:</strong> &ldquo;${escapeHtml(rw.suggested)}&rdquo;</div>
                </div>
            `).join('');
        } else {
            rewritesEl.innerHTML = `<p style="font-size:0.875rem; color:var(--success);">✓ Strong action-oriented phrasing detected across parsed bullets!</p>`;
        }

        // ATS Issues list
        const atsList = document.getElementById('atsIssuesList');
        if (atsIssues.length > 0) {
            atsList.innerHTML = `
                <ul class="checklist">
                    ${atsIssues.map(i => `
                        <li class="checklist-item">
                            <span style="color:var(--danger); font-weight:700;">⚠</span>
                            <div class="checklist-text"><strong>${escapeHtml(i.type)}:</strong> ${escapeHtml(i.description)}</div>
                        </li>
                    `).join('')}
                </ul>
            `;
        } else {
            atsList.innerHTML = `<p style="font-size:0.875rem; color:var(--success);">✓ Clean ATS formatting: Essential headers and contact details detected.</p>`;
        }

        currentScanResult = {
            healthScore,
            matchPercentage: matchResult.matchPercentage,
            matchedKeywords: matchResult.matchedKeywords,
            missingKeywords: matchResult.missingKeywords,
            atsIssues
        };

        const saveBtn = document.getElementById('btnSaveVersion');
        if (saveBtn) saveBtn.disabled = false;

        progressEl.style.display = 'none';
        showAlert(`Scan completed! Your real ATS Health Score is ${healthScore}/100. Click "💾 Save as New Version" to record your progress.`, "success");

    } catch (err) {
        progressEl.style.display = 'none';
        showAlert("Scan error: " + err.message, "danger");
    }
}

function saveVersion() {
    if (!currentScanResult) {
        showAlert("Please run a scan before saving.", "warning");
        return;
    }

    const nextVer = versionsHistory.length + 1;
    const band = ResumeRadar.getHealthScoreBand(currentScanResult.healthScore);

    const newEntry = {
        versionNumber: nextVer,
        healthScore: currentScanResult.healthScore,
        scoreBand: band.band,
        matchPercentage: currentScanResult.matchPercentage,
        createdAt: new Date().toISOString()
    };

    versionsHistory.push(newEntry);
    localStorage.setItem('resumeradar_versions', JSON.stringify(versionsHistory));

    const verBadge = document.getElementById('currentVersionBadge');
    if (verBadge) verBadge.innerText = `v${nextVer}`;

    renderTrend();
    showAlert(`Successfully recorded Version v${nextVer} (Health Score: ${currentScanResult.healthScore}%)!`, "success");
}

/* ==========================================================================
   MENTOR & STUDENT LINK COLLABORATION SYSTEM
   ========================================================================== */

// 1. Student opens Mentor Modal
window.toggleMentorModal = () => {
    const modal = document.getElementById('mentorLinkModal');
    if (!modal) return;
    const isHidden = modal.style.display === 'none' || !modal.style.display;
    modal.style.display = isHidden ? 'flex' : 'none';

    if (isHidden) {
        const code = localStorage.getItem(SHARECODE_KEY) || 'RADAR-DEMO-2026';
        const displayEl = document.getElementById('modalStudentShareCode');
        if (displayEl) displayEl.innerText = code;
        renderStudentMentorFeedback();
    }
};

window.closeMentorModal = () => {
    const modal = document.getElementById('mentorLinkModal');
    if (modal) modal.style.display = 'none';
};

window.generateNewShareCode = () => {
    const newCode = "RADAR-" + Math.random().toString(36).substring(2, 8).toUpperCase();
    localStorage.setItem(SHARECODE_KEY, newCode);
    const displayEl = document.getElementById('modalStudentShareCode');
    if (displayEl) displayEl.innerText = newCode;
    showAlert(`New share code generated: ${newCode}`, 'success');
};

window.copyShareCode = () => {
    const code = localStorage.getItem(SHARECODE_KEY) || 'RADAR-DEMO-2026';
    navigator.clipboard.writeText(code).then(() => {
        showAlert(`Share code "${code}" copied to clipboard! Share it with your mentor.`, 'success');
    }).catch(() => {
        showAlert(`Share code is: ${code}`, 'info');
    });
};

function renderStudentMentorFeedback() {
    const feedbackList = JSON.parse(localStorage.getItem(FEEDBACK_KEY)) || [];
    const container = document.getElementById('studentMentorFeedbackList');
    if (!container) return;

    if (feedbackList.length === 0) {
        container.innerHTML = '<p style="font-size:0.875rem; color:var(--text-muted);">No mentor feedback received yet. Share your code with a mentor to get guidance!</p>';
        return;
    }

    container.innerHTML = feedbackList.map(item => `
        <div class="card" style="background-color:var(--teal-light); border-color:#99f6e4; padding:0.75rem; margin-bottom:0.5rem;">
            <div style="font-size:0.875rem; color:#115e59; font-weight:500;">&ldquo;${escapeHtml(item.note)}&rdquo;</div>
            <div style="font-size:0.75rem; color:#0f766e; margin-top:0.25rem;">
                By <strong>${escapeHtml(item.mentorName)}</strong> &bull; <span>${escapeHtml(item.date)}</span>
            </div>
        </div>
    `).join('');
}

// 2. Mentor Hub Logic
function renderLinkedStudentsList() {
    const links = JSON.parse(localStorage.getItem(LINKS_KEY)) || [];
    const container = document.getElementById('linkedStudentsContainer');
    if (!container) return;

    if (links.length === 0) {
        container.innerHTML = '<p style="color:var(--text-muted); padding:1rem 0;">No students connected yet. Enter a student share code above to link!</p>';
        return;
    }

    container.innerHTML = links.map(link => `
        <div class="card" style="margin-bottom:1rem; border-left:4px solid var(--teal);">
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:0.5rem; margin-bottom:0.5rem;">
                <div>
                    <strong style="font-size:1.05rem;">👤 ${escapeHtml(link.studentName)}</strong>
                    <span style="color:var(--text-muted); font-size:0.8125rem;"> (${escapeHtml(link.studentEmail)})</span>
                </div>
                <span class="badge badge-teal">Code: ${escapeHtml(link.shareCode)}</span>
            </div>
            <div style="font-size:0.8125rem; color:var(--text-muted); margin-bottom:0.75rem;">
                Tracking student score curve across revisions (v1 &rarr; v2 &rarr; v3). Raw resume text remains private.
            </div>
            <div class="trend-chart-box" id="mentorStudentChart"></div>
        </div>
    `).join('');

    // Render trend for the student
    ResumeRadar.renderScoreTrendSvg('mentorStudentChart', versionsHistory);
}

window.linkStudentByCode = () => {
    const codeInput = document.getElementById('mentorStudentCodeInput');
    if (!codeInput) return;
    const code = codeInput.value.trim().toUpperCase();

    if (!code) {
        showAlert("Please enter a valid student share code.", "warning");
        return;
    }

    const currentCode = localStorage.getItem(SHARECODE_KEY) || 'RADAR-DEMO-2026';
    if (code !== currentCode && code !== 'RADAR-DEMO-2026') {
        showAlert(`Code "${code}" not found. For testing, try demo code: RADAR-DEMO-2026`, "danger");
        return;
    }

    const links = JSON.parse(localStorage.getItem(LINKS_KEY)) || [];
    const alreadyLinked = links.some(l => l.shareCode === code);
    if (!alreadyLinked) {
        links.push({
            mentorEmail: currentSession ? currentSession.email : 'mentor@resumeradar.com',
            studentEmail: 'student@resumeradar.com',
            studentName: 'Alex Learner',
            shareCode: code
        });
        localStorage.setItem(LINKS_KEY, JSON.stringify(links));
    }

    codeInput.value = '';
    renderLinkedStudentsList();
    showAlert(`Successfully connected with student (Code: ${code})!`, "success");
};

window.postMentorFeedback = () => {
    const noteArea = document.getElementById('mentorFeedbackText');
    if (!noteArea) return;
    const note = noteArea.value.trim();

    if (!note) {
        showAlert("Please write some feedback before posting.", "warning");
        return;
    }

    const feedbackList = JSON.parse(localStorage.getItem(FEEDBACK_KEY)) || [];
    feedbackList.unshift({
        mentorName: currentSession ? currentSession.name : 'Sarah Mentor',
        studentEmail: 'student@resumeradar.com',
        note: note,
        date: new Date().toLocaleDateString()
    });
    localStorage.setItem(FEEDBACK_KEY, JSON.stringify(feedbackList));

    noteArea.value = '';
    showAlert("Feedback posted! The student will see your notes immediately on their dashboard.", "success");
};

function showAlert(text, type) {
    const el = document.getElementById('statusAlert');
    if (!el) return;
    el.className = `alert alert-${type}`;
    el.innerHTML = text;
    el.style.display = 'flex';
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[m]));
}

// Global window assignments
window.performScan = performScan;
window.saveVersion = saveVersion;
window.logout = () => {
    localStorage.removeItem(SESSION_KEY);
    window.location.href = '/login.html';
};
