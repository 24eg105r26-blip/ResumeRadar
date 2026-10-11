# ResumeRadar 🎯
> **See Your Resume the Way a Recruiter Does — Directly On Your Own Device.**

ResumeRadar is a privacy-first web application that evaluates technical resumes against target job descriptions like an enterprise ATS (Applicant Tracking System) does. Document parsing, skill extraction, keyword matching, and bullet rewriting run **strictly on-device** in the client's browser. The raw resume file is never uploaded to any server.

---

## 📁 Project Architecture & Folder Structure

The project is cleanly split into two dedicated folders:

```
ResumeRadar/
├── backend/                                  <-- Spring Boot 3.2 Java 17+ Backend
│   ├── pom.xml                               <-- Maven dependencies & build configuration
│   └── src/
│       ├── main/java/com/resumeradar/
│       │   ├── ResumeRadarApplication.java  <-- Main server entrypoint
│       │   ├── config/                      <-- Security & Data seeder configuration
│       │   ├── controller/                  <-- REST and MVC Web Controllers
│       │   ├── service/                     <-- Scoring, Resume, Mentor & Admin services
│       │   ├── repository/                  <-- Spring Data JPA database repositories
│       │   ├── entity/                      <-- 9 JPA database entities
│       │   ├── dto/                         <-- Data transfer objects
│       │   └── security/                    <-- User session authentication
│       ├── main/resources/
│       │   ├── application.properties       <-- MySQL database connection settings
│       │   ├── templates/                   <-- Server-rendered Thymeleaf HTML pages
│       │   └── static/                      <-- CSS styles, JS engine & vendor parsers
│       └── test/                            <-- Automated test suites (all passing)
│
├── frontend/                                 <-- Clean Dedicated Frontend Assets
│   ├── css/
│   │   └── style.css                        <-- Pure hand-written light theme CSS
│   ├── js/
│   │   └── resumeradar.js                   <-- On-device parsing, matching & rewrite engine
│   ├── vendor/                              <-- In-browser offline parsers (pdf.js, mammoth.js)
│   ├── templates/                           <-- Complete Thymeleaf HTML templates
│   └── README.md                            <-- Frontend design system & architecture
│
├── pom.xml                                   <-- Root Maven aggregator module
├── .gitignore                                <-- Standard Java & OS ignore rules
└── README.md                                 <-- Project overview & setup instructions
```

---

## 🎨 UI & Design Palette
Built with a calm, modern light-themed color palette:
- **Primary:** Calm Oceanic Blue (`#0284c7`)
- **Accent:** Clear Mint Teal (`#0d9488`)
- **Backgrounds:** Clean Airy White (`#ffffff`) and Soft Slate (`#f8fafc`)
- **Text & Borders:** High-contrast Charcoal (`#0f172a`) and crisp slate outlines (`#e2e8f0`)
- **Pure Hand-written Vanilla CSS:** Zero heavy UI frameworks (no React, Angular, Bootstrap, or Tailwind).

---

## ⚡ Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 17+, Spring Boot 3.2, Maven |
| **Starters & Libs** | Spring Web, Thymeleaf, Spring Security 6, Spring Data JPA, MySQL Driver, Lombok, Validation |
| **Database** | MySQL 8.0 (configured with placeholders & auto-ddl update) |
| **Frontend** | Server-rendered Thymeleaf templates, hand-written CSS, vanilla JavaScript |
| **On-Device Engine** | `pdf.js` (client-side PDF extraction) & `mammoth.js` (client-side DOCX extraction) |
| **Security** | Session-based authentication, CSRF tokens on forms & API fetch, BCrypt password hashing, method-level `@PreAuthorize` |

---

## 🚀 Key Features

1. **On-Device Parsing & Absolute Privacy:**
   - Drag & drop PDF, DOCX, or TXT files.
   - Files are parsed directly inside browser memory via `pdf.js` and `mammoth.js`.
   - The original file never leaves your machine. Only extracted text and scores are saved.
2. **Job Description Keyword Alignment:**
   - Built-in engineering skills dictionary (Java, Spring Boot, MySQL, Docker, React, AWS, Git, etc.).
   - Instant match percentage calculation and keyword density analysis.
3. **Gap Analysis & Bullet Point Rewrites:**
   - Highlights missing must-have keywords.
   - Detects passive phrases (*"worked on"*, *"responsible for"*, *"helped with"*).
   - Generates strong accomplishment rewrites following the **Google XYZ formula** (*"Accomplished [X], measured by [Y], by doing [Z]"*).
4. **ATS Compatibility Risk Scan:**
   - Detects short unparsed text (scanned graphics, raster text).
   - Verifies contact details (phone, email).
   - Checks essential section headers (Skills, Experience, Education).
5. **Resume Health Score (0-100):**
   - 70% weight for job description skill alignment.
   - 30% weight for clean ATS formatting (penalties deducted for formatting hurdles).
   - Band categorizations: *Weak (0-39)*, *Needs Work (40-59)*, *Good (60-79)*, *Excellent (80-100)*.
6. **Immutable Versioning & SVG Trend Chart:**
   - Every revision is saved as a new version (v1 &rarr; v2 &rarr; v3).
   - Interactive SVG line chart plots score trajectory across revisions.
7. **Mentor Collaboration Hub:**
   - Students generate a private share code (e.g., `RADAR-DEMO-2026`).
   - Mentors connect via share code to inspect score trends and leave actionable notes.
   - **Privacy Guarantee:** Mentors cannot see resume text unless explicitly authorized.
8. **Admin Operations:**
   - Overview metrics: user counts, resume totals, version counts, and user directories.
9. **Public SEO-Optimized Guides:**
   - `/guides/ats-friendly-formatting`
   - `/guides/resume-writing-tips`
   - Complete `/sitemap.xml` and `/robots.txt`.

---

## 🔑 Pre-Seeded Demo Credentials

The database automatically seeds test accounts upon startup:

| Role | Email | Password | Access Area |
|---|---|---|---|
| **Student (USER)** | `student@resumeradar.com` | `Password123!` | `/dashboard`, `/resumes`, `/jobs` |
| **Mentor (MENTOR)** | `mentor@resumeradar.com` | `Password123!` | `/mentor/dashboard`, Student score trends |
| **Administrator (ADMIN)** | `admin@resumeradar.com` | `AdminPassword123!` | `/admin`, System metrics, User directory |

---

## 🛠️ Setup & Running

### Prerequisites
- Java 17+ installed (`java -version`)
- Maven 3.8+ installed (`mvn -v`)
- MySQL 8.0 server running locally

### Step 1: Create the MySQL Database
Log into MySQL and create the database:
```sql
CREATE DATABASE resumeradar;
```

### Step 2: Configure Database Credentials
Edit `backend/src/main/resources/application.properties` with your MySQL credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/resumeradar?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### Step 3: Run the Application
Open PowerShell or Command Prompt in the project root:
```bash
mvn spring-boot:run -pl backend
```
*(Or navigate into `cd backend` and run `mvn spring-boot:run`)*

The application will start on: **`http://localhost:8080`**

---

## 🧪 Running Unit Tests
Execute the automated test suite from the root:
```bash
mvn test
```
*(All 8 unit and owner tests pass with 0 errors).*
