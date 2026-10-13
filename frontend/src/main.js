import './style.css';
import { ResumeRadar } from './resumeradar.js';

window.ResumeRadar = ResumeRadar;

// Use real saved history or start fresh with empty array (NO FAKE HARDCODED DATA)
let versionsHistory = JSON.parse(localStorage.getItem('resumeradar_versions')) || [];
let currentScanResult = null;
let currentSelectedFile = null;

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function initApp() {
    renderTrend();

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
        // Clean initial state: no fake score
        ResumeRadar.updateScoreGauge(null);
        const verBadge = document.getElementById('currentVersionBadge');
        if (verBadge) verBadge.innerText = 'Draft';
        const matchPctEl = document.getElementById('statMatchPct');
        if (matchPctEl) matchPctEl.innerText = '--%';
        const penaltyEl = document.getElementById('statAtsPenalty');
        if (penaltyEl) penaltyEl.innerText = '--';
    }

    setupDropzone();
    setupEventListeners();
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

    // File input change
    fileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files.length > 0) {
            handleSelectedFile(e.target.files[0]);
        }
    });

    // Drag and drop events
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

    // Clicking the dropzone opens file dialog
    dropzone.addEventListener('click', () => {
        fileInput.click();
    });
}

function handleSelectedFile(file) {
    currentSelectedFile = file;
    const dropText = document.getElementById('dropzoneText');
    if (dropText) {
        dropText.innerHTML = `<strong>Selected File:</strong> ${escapeHtml(file.name)} <span style="color:var(--primary); font-weight:600;">(${(file.size / 1024).toFixed(1)} KB)</span>`;
    }
    const dropSub = document.querySelector('.dropzone-subtext');
    if (dropSub) {
        dropSub.innerText = 'Ready for scan! Click "⚡ Run On-Device Scan" below.';
    }
    showAlert(`Loaded "${file.name}". Click "⚡ Run On-Device Scan" to process locally.`, 'info');
}

function setupEventListeners() {
    const scanBtn = document.getElementById('btnScan');
    if (scanBtn) scanBtn.addEventListener('click', performScan);

    const saveBtn = document.getElementById('btnSaveVersion');
    if (saveBtn) saveBtn.addEventListener('click', saveVersion);

    const mentorBtn = document.getElementById('btnMentorCode');
    if (mentorBtn) mentorBtn.addEventListener('click', toggleMentorModal);

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
        showAlert("Please upload your resume file (PDF, DOCX, or TXT) first.", "danger");
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
            throw new Error("Could not extract text from document. Ensure file contains selectable text rather than scanned images.");
        }

        progressEl.innerText = "Scanning skills dictionary, ATS format & phrasing...";

        const jdSkills = ResumeRadar.extractSkillsFromJobDescription(jdText);
        const matchResult = ResumeRadar.compareResumeWithJob(resumeText, jdSkills);
        const atsIssues = ResumeRadar.checkAtsCompatibility(resumeText);
        const rewrites = ResumeRadar.generateRewriteSuggestions(resumeText, matchResult.matchedKeywords);
        const healthScore = ResumeRadar.calculateHealthScore(matchResult.matchPercentage, atsIssues.length);

        // Update Gauge with Real Score
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

function toggleMentorModal() {
    const banner = document.getElementById('mentorCodeBanner');
    if (!banner) return;
    const isHidden = banner.style.display === 'none' || banner.style.display === '';
    banner.style.display = isHidden ? 'block' : 'none';
    if (isHidden) {
        banner.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

function generateCode() {
    const code = "RADAR-" + Math.random().toString(36).substring(2, 8).toUpperCase();
    const codeEl = document.getElementById('displayShareCode');
    if (codeEl) codeEl.innerText = code;
    showAlert(`New mentor share code generated: ${code}`, "success");
}

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

// Window global bindings for inline HTML triggers
window.toggleMentorModal = toggleMentorModal;
window.generateCode = generateCode;
window.performScan = performScan;
window.saveVersion = saveVersion;
