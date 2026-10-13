/**
 * ResumeRadar — Client-side On-Device Parsing, ATS Scoring & Rewriting Engine
 * 
 * Strict Privacy Guarantee: The raw PDF/DOCX file is NEVER sent to any server.
 * Only the extracted text and computed scores are processed on-device.
 */

// Built-in Skills Dictionary for Tech & Engineering Roles
const SKILLS_DICTIONARY = [
    "java", "spring", "spring boot", "python", "javascript", "typescript",
    "react", "angular", "vue", "node", "nodejs", "node.js", "express",
    "sql", "mysql", "postgresql", "postgres", "mongodb", "redis", "oracle",
    "docker", "kubernetes", "k8s", "aws", "azure", "gcp", "git", "github",
    "ci/cd", "rest", "rest api", "graphql", "microservices", "kafka", "rabbitmq",
    "linux", "bash", "html", "html5", "css", "css3", "sass", "c++", "c#", ".net",
    "golang", "go", "rust", "junit", "mockito", "selenium", "jest", "pytest",
    "agile", "scrum", "jira", "terraform", "maven", "gradle", "hibernate", "jpa"
];

// Weak bullet point phrase detectors
const WEAK_PHRASES = [
    { regex: /\b(worked on|working on)\b/gi, label: "worked on" },
    { regex: /\b(responsible for|was responsible for)\b/gi, label: "responsible for" },
    { regex: /\b(helped with|helped to|assisted with|assisted in)\b/gi, label: "helped with" },
    { regex: /\b(involved in|part of team that)\b/gi, label: "involved in" },
    { regex: /\b(handled|duties included)\b/gi, label: "handled / duties included" }
];

// Action verbs suggestions
const STRONG_ACTION_VERBS = [
    "Architected", "Engineered", "Developed", "Spearheaded", "Optimized",
    "Designed", "Automated", "Streamlined", "Deployed", "Refactored"
];

/**
 * Extracts text from a PDF file using pdf.js in-browser
 */
async function extractTextFromPDF(file) {
    const pdfLib = window.pdfjsLib || (typeof pdfjsLib !== 'undefined' ? pdfjsLib : null);
    if (!pdfLib) {
        throw new Error("PDF parser library (pdf.js) is not loaded. Please refresh or check vendor scripts.");
    }
    try {
        pdfLib.GlobalWorkerOptions.workerSrc = '/vendor/pdf.worker.min.js';
    } catch (_) {}

    const arrayBuffer = await file.arrayBuffer();
    const loadingTask = pdfLib.getDocument({ data: new Uint8Array(arrayBuffer) });
    const pdf = await loadingTask.promise;
    let fullText = "";

    for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
        const page = await pdf.getPage(pageNum);
        const textContent = await page.getTextContent();
        const pageItems = textContent.items.map(item => item.str);
        fullText += pageItems.join(" ") + "\n\n";
    }
    return fullText.trim();
}

/**
 * Extracts text from a DOCX file using mammoth.js in-browser
 */
async function extractTextFromDOCX(file) {
    const mammothLib = window.mammoth || (typeof mammoth !== 'undefined' ? mammoth : null);
    if (!mammothLib) {
        throw new Error("DOCX parser library (mammoth.js) is not loaded. Please refresh or check vendor scripts.");
    }
    const arrayBuffer = await file.arrayBuffer();
    const result = await mammothLib.extractRawText({ arrayBuffer: arrayBuffer });
    return result.value.trim();
}

/**
 * Parse any supported resume file (PDF, DOCX, TXT)
 */
async function parseResumeFile(file) {
    if (!file) throw new Error("No file selected.");
    const fileName = file.name.toLowerCase();
    if (fileName.endsWith(".pdf")) {
        return await extractTextFromPDF(file);
    } else if (fileName.endsWith(".docx")) {
        return await extractTextFromDOCX(file);
    } else if (fileName.endsWith(".txt")) {
        return await file.text();
    } else {
        throw new Error("Unsupported format (" + file.name + "). Please upload a PDF, DOCX, or TXT file.");
    }
}

/**
 * Extracts skills from target job description using dictionary matching
 */
function extractSkillsFromJobDescription(jdText) {
    if (!jdText) return [];
    const lowerJd = jdText.toLowerCase();
    const detectedSkills = new Set();

    SKILLS_DICTIONARY.forEach(skill => {
        const escaped = skill.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
        const pattern = new RegExp("(?:^|[^a-zA-Z0-9#+])" + escaped + "(?:$|[^a-zA-Z0-9#+])", "i");
        if (pattern.test(lowerJd)) {
            detectedSkills.add(skill);
        }
    });

    return Array.from(detectedSkills);
}

/**
 * Computes keyword matches and gaps between resume and job description
 */
function compareResumeWithJob(resumeText, jdSkills) {
    const lowerResume = (resumeText || "").toLowerCase();
    const matched = [];
    const missing = [];

    jdSkills.forEach(skill => {
        const escaped = skill.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
        const pattern = new RegExp("(?:^|[^a-zA-Z0-9#+])" + escaped + "(?:$|[^a-zA-Z0-9#+])", "i");
        if (pattern.test(lowerResume)) {
            matched.push(skill);
        } else {
            missing.push(skill);
        }
    });

    const matchPercentage = jdSkills.length > 0 
        ? Math.round((matched.length / jdSkills.length) * 100) 
        : (lowerResume.length > 50 ? 50 : 0);

    return {
        matchPercentage,
        matchedKeywords: matched,
        missingKeywords: missing
    };
}

/**
 * Checks for ATS formatting hurdles and risks
 */
function checkAtsCompatibility(resumeText) {
    const issues = [];
    const text = resumeText || "";
    const lower = text.toLowerCase();

    // 1. Length & Graphic / Image Risk
    if (text.length < 250) {
        issues.push({
            type: "LAYOUT_RISK",
            description: "Extracted text is very short (" + text.length + " characters). If this is a full resume, text may be stored inside images or scanned graphics that ATS parsers cannot read."
        });
    }

    // 2. Email presence
    const emailRegex = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/;
    if (!emailRegex.test(text)) {
        issues.push({
            type: "CONTACT_INFO",
            description: "No standard email address detected. Ensure your email is in plain text body, not trapped in an icon image."
        });
    }

    // 3. Phone presence
    const phoneRegex = /(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}/;
    if (!phoneRegex.test(text)) {
        issues.push({
            type: "CONTACT_INFO",
            description: "No phone number detected. Ensure your contact number is placed in plain body text."
        });
    }

    // 4. Missing standard sections
    const hasEducation = /\b(education|university|college|bachelor|master|degree|b\.tech|b\.s|b\.e)\b/i.test(lower);
    if (!hasEducation) {
        issues.push({
            type: "MISSING_SECTION",
            description: "Missing dedicated 'Education' section heading."
        });
    }

    const hasExperience = /\b(experience|work history|employment|internship|projects)\b/i.test(lower);
    if (!hasExperience) {
        issues.push({
            type: "MISSING_SECTION",
            description: "Missing dedicated 'Experience' or 'Projects' section heading."
        });
    }

    const hasSkillsSection = /\b(skills|technical skills|technologies|proficiencies|tools)\b/i.test(lower);
    if (!hasSkillsSection) {
        issues.push({
            type: "MISSING_SECTION",
            description: "Missing dedicated 'Technical Skills' or 'Skills' section heading."
        });
    }

    return issues;
}

/**
 * Finds weak bullets and generates Google XYZ formula rewrite suggestions
 */
function generateRewriteSuggestions(resumeText, matchedSkills) {
    const suggestions = [];
    const lines = (resumeText || "").split(/\r?\n/);
    const primarySkill = (matchedSkills && matchedSkills.length > 0) ? matchedSkills[0] : "core technologies";

    for (const rawLine of lines) {
        const line = rawLine.trim();
        if (line.length < 15 || line.length > 300) continue;

        for (const weak of WEAK_PHRASES) {
            if (weak.regex.test(line)) {
                const verb = STRONG_ACTION_VERBS[Math.floor(Math.random() * STRONG_ACTION_VERBS.length)];
                const rewritten = verb + " [feature/system] using " + primarySkill + ", improving [metric, e.g. latency, throughput] by X% across [team/users].";
                
                suggestions.push({
                    original: line,
                    weakPhrase: weak.label,
                    suggested: rewritten
                });
                break;
            }
        }
        if (suggestions.length >= 4) break;
    }

    return suggestions;
}

/**
 * Computes the Resume Health Score (0-100)
 */
function calculateHealthScore(matchPercentage, atsIssueCount) {
    const clampedMatch = Math.max(0, Math.min(100, matchPercentage));
    const matchPart = clampedMatch * 0.70;
    const atsPart = Math.max(0, 30.0 - (atsIssueCount * 6));
    return Math.max(0, Math.min(100, Math.round(matchPart + atsPart)));
}

function getHealthScoreBand(score) {
    if (score >= 80) return { band: "Excellent", desc: "Interview-ready: High keyword alignment and clean ATS structure.", color: "#0d9488" };
    if (score >= 60) return { band: "Good", desc: "Strong, minor gaps: Solid foundation with minor skill or formatting adjustments needed.", color: "#0284c7" };
    if (score >= 40) return { band: "Needs Work", desc: "Needs revision: Critical keywords missing or formatting hurdles detected.", color: "#d97706" };
    return { band: "Weak", desc: "Major rework: Low job alignment or severe ATS formatting blockers.", color: "#dc2626" };
}

/**
 * Draws an SVG Score Trend line chart into a target container
 */
function renderScoreTrendSvg(containerId, scoreData) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!scoreData || scoreData.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: var(--text-muted); padding: 2.5rem 1rem;">No versions recorded yet. Upload your resume and click \'Run On-Device Scan\' to compute your initial score!</p>';
        return;
    }

    const width = 540;
    const height = 150;
    const padding = 35;

    const points = scoreData.map((d, i) => {
        const x = scoreData.length === 1 
            ? width / 2 
            : padding + (i / (scoreData.length - 1)) * (width - 2 * padding);
        const y = height - padding - (d.healthScore / 100) * (height - 2 * padding);
        return { x, y, score: d.healthScore, version: d.versionNumber };
    });

    let pathD = "";
    points.forEach((pt, i) => {
        pathD += (i === 0 ? "M " : " L ") + pt.x + " " + pt.y;
    });

    let dotsHtml = "";
    points.forEach(pt => {
        const bandInfo = getHealthScoreBand(pt.score);
        dotsHtml += `
            <circle cx="${pt.x}" cy="${pt.y}" r="6" fill="${bandInfo.color}" stroke="#ffffff" stroke-width="2"/>
            <text x="${pt.x}" y="${pt.y - 12}" font-size="11" font-weight="700" fill="#0f172a" text-anchor="middle">v${pt.version}: ${pt.score}%</text>
        `;
    });

    const y40 = height - padding - (40 / 100) * (height - 2 * padding);
    const y60 = height - padding - (60 / 100) * (height - 2 * padding);
    const y80 = height - padding - (80 / 100) * (height - 2 * padding);

    const svgHtml = `
        <svg viewBox="0 0 ${width} ${height}" class="trend-svg" xmlns="http://www.w3.org/2000/svg">
            <line x1="${padding}" y1="${y40}" x2="${width - padding}" y2="${y40}" stroke="#e2e8f0" stroke-dasharray="3,3" />
            <line x1="${padding}" y1="${y60}" x2="${width - padding}" y2="${y60}" stroke="#e2e8f0" stroke-dasharray="3,3" />
            <line x1="${padding}" y1="${y80}" x2="${width - padding}" y2="${y80}" stroke="#e2e8f0" stroke-dasharray="3,3" />
            <text x="${padding - 5}" y="${y40 + 4}" font-size="9" fill="#94a3b8" text-anchor="end">40</text>
            <text x="${padding - 5}" y="${y60 + 4}" font-size="9" fill="#94a3b8" text-anchor="end">60</text>
            <text x="${padding - 5}" y="${y80 + 4}" font-size="9" fill="#94a3b8" text-anchor="end">80</text>
            <path d="${pathD}" fill="none" stroke="#0284c7" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            ${dotsHtml}
        </svg>
    `;

    container.innerHTML = svgHtml;
}

/**
 * Updates the circular radial Score Gauge
 */
function updateScoreGauge(score) {
    const circle = document.getElementById("gaugeFillCircle");
    const numberEl = document.getElementById("gaugeScoreNumber");
    const bandEl = document.getElementById("gaugeBandTitle");
    const descEl = document.getElementById("gaugeBandDescription");

    if (!circle || !numberEl) return;

    if (score === null || score === undefined || score === "--") {
        circle.style.strokeDashoffset = "439.82";
        circle.style.stroke = "var(--primary)";
        numberEl.innerText = "--";
        numberEl.style.color = "var(--text-primary)";
        if (bandEl) {
            bandEl.innerText = "Awaiting Scan";
            bandEl.style.color = "var(--text-primary)";
        }
        if (descEl) {
            descEl.innerText = "Upload your resume (PDF/DOCX/TXT) and click 'Run On-Device Scan' to evaluate your ATS score.";
        }
        return;
    }

    const band = getHealthScoreBand(score);
    const radius = 70;
    const circumference = 2 * Math.PI * radius; // ~439.82

    circle.style.strokeDasharray = `${circumference} ${circumference}`;
    const offset = circumference - (score / 100) * circumference;
    circle.style.strokeDashoffset = offset;
    circle.style.stroke = band.color;

    numberEl.innerText = score;
    numberEl.style.color = band.color;

    if (bandEl) {
        bandEl.innerText = band.band;
        bandEl.style.color = band.color;
    }
    if (descEl) {
        descEl.innerText = band.desc;
    }
}

const ResumeRadar = {
    parseResumeFile,
    extractSkillsFromJobDescription,
    compareResumeWithJob,
    checkAtsCompatibility,
    generateRewriteSuggestions,
    calculateHealthScore,
    getHealthScoreBand,
    renderScoreTrendSvg,
    updateScoreGauge
};

// Expose globally on window for all browser environments
if (typeof window !== 'undefined') {
    window.ResumeRadar = ResumeRadar;
}

export { ResumeRadar };
