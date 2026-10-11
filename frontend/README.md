# ResumeRadar — Frontend 💻

This directory contains the user interface components, styles, on-device parsing libraries, and templates for ResumeRadar.

## 📁 Directory Structure
```
frontend/
├── css/
│   └── style.css            # Hand-written light-themed vanilla CSS with CSS variables
├── js/
│   └── resumeradar.js       # On-device PDF/DOCX parsing, skills matcher, and rewrite engine
├── vendor/
│   ├── pdf.min.js           # PDF.js library for in-browser PDF parsing
│   ├── pdf.worker.min.js    # PDF.js web worker
│   └── mammoth.browser.min.js # Mammoth.js for in-browser DOCX parsing
└── templates/               # Thymeleaf server-rendered HTML templates
    ├── index.html           # Landing page
    ├── dashboard.html       # One-screen ATS command center
    ├── login.html           # Sign-in page
    ├── register.html        # Registration page
    ├── error.html           # Friendly error page
    ├── fragments/           # Reusable header and footer
    ├── resumes/             # Resume list, version history & detail
    ├── jobs/                # Job descriptions management
    ├── mentor/              # Mentor hub & student review view
    ├── admin/               # System administrator dashboard
    └── guides/              # Public SEO guide pages
```

## 🎨 Design System
- **Theme:** Modern Light Mode
- **Palette:** Crisp whites, soft slates (`#f8fafc`), calm ocean blue (`#0284c7`), and mint teal (`#0d9488`).
- **Frameworks:** Pure Vanilla CSS and Vanilla JavaScript — zero bloated external frameworks (no Bootstrap, Tailwind, or React).

## 🔒 On-Device Privacy Architecture
All file parsing takes place in browser client memory via `pdf.js` and `mammoth.js`. The raw document file is **never uploaded** to the backend server.
