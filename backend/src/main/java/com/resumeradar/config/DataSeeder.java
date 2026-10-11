package com.resumeradar.config;

import com.resumeradar.entity.*;
import com.resumeradar.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final MatchReportRepository matchReportRepository;
    private final AtsIssueRepository atsIssueRepository;
    private final MentorLinkRepository mentorLinkRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${resumeradar.seed.data:true}")
    private boolean seedDataEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDataEnabled) {
            return;
        }

        // 1. Seed Roles
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        Role mentorRole = roleRepository.findByName("ROLE_MENTOR")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_MENTOR")));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        // 2. Seed Demo User (Learner)
        User learner = userRepository.findByEmail("student@resumeradar.com").orElse(null);
        if (learner == null) {
            learner = User.builder()
                    .name("Alex Learner")
                    .email("student@resumeradar.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .roles(new HashSet<>(Collections.singletonList(userRole)))
                    .build();
            learner = userRepository.save(learner);
            log.info("Seeded Demo Student: student@resumeradar.com / Password123!");
        }

        // 3. Seed Demo Mentor
        User mentor = userRepository.findByEmail("mentor@resumeradar.com").orElse(null);
        if (mentor == null) {
            mentor = User.builder()
                    .name("Sarah Mentor")
                    .email("mentor@resumeradar.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .roles(new HashSet<>(Collections.singletonList(mentorRole)))
                    .build();
            mentor = userRepository.save(mentor);
            log.info("Seeded Demo Mentor: mentor@resumeradar.com / Password123!");
        }

        // 4. Seed Demo Admin
        User admin = userRepository.findByEmail("admin@resumeradar.com").orElse(null);
        if (admin == null) {
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            adminRoles.add(userRole);
            admin = User.builder()
                    .name("Admin Leader")
                    .email("admin@resumeradar.com")
                    .passwordHash(passwordEncoder.encode("AdminPassword123!"))
                    .createdAt(LocalDateTime.now().minusDays(20))
                    .roles(adminRoles)
                    .build();
            admin = userRepository.save(admin);
            log.info("Seeded Demo Admin: admin@resumeradar.com / AdminPassword123!");
        }

        // 5. Seed Sample Job Description for Learner if none exists
        if (jobDescriptionRepository.countByUser(learner) == 0) {
            JobDescription jd = JobDescription.builder()
                    .user(learner)
                    .title("Full Stack Java Developer")
                    .text("We are seeking a Full Stack Java Developer proficient in Java, Spring Boot, MySQL, REST APIs, Git, Docker, and microservices architecture. Strong problem-solving and clean code fundamentals required.")
                    .extractedSkills("java, spring, spring boot, mysql, sql, git, docker, microservices, rest api")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();
            jobDescriptionRepository.save(jd);
        }

        // 6. Seed Sample Resume and Versions if none exists
        if (resumeRepository.countByUser(learner) == 0) {
            Resume resume = Resume.builder()
                    .user(learner)
                    .title("Software Engineer - Core Resume")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();
            resume = resumeRepository.save(resume);

            // Version 1 (Initial Draft - Weak)
            ResumeVersion v1 = ResumeVersion.builder()
                    .resume(resume)
                    .versionNumber(1)
                    .healthScore(48)
                    .extractedText("Alex Learner\nalex@example.com | 555-0199\n\nEXPERIENCE\nSoftware Developer Intern\n- Responsible for building web applications using Java and MySQL.\n- Worked on backend endpoints for student registration.\n- Helped with database queries and bug fixes.")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();
            v1 = resumeVersionRepository.save(v1);

            AtsIssue issue1 = AtsIssue.builder()
                    .resumeVersion(v1)
                    .type("MISSING_SECTION")
                    .description("Missing dedicated 'SKILLS' or 'TECHNICAL SKILLS' section header.")
                    .build();
            AtsIssue issue2 = AtsIssue.builder()
                    .resumeVersion(v1)
                    .type("WEAK_ACTION_VERBS")
                    .description("Passive bullet phrases detected ('Responsible for', 'Worked on', 'Helped with').")
                    .build();
            atsIssueRepository.save(issue1);
            atsIssueRepository.save(issue2);

            MatchReport report1 = MatchReport.builder()
                    .resumeVersion(v1)
                    .matchPercentage(50)
                    .missingKeywords("docker, spring boot, microservices, rest api")
                    .matchedKeywords("java, mysql")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build();
            matchReportRepository.save(report1);

            // Version 2 (Revised Draft - Strong)
            ResumeVersion v2 = ResumeVersion.builder()
                    .resume(resume)
                    .versionNumber(2)
                    .healthScore(82)
                    .extractedText("Alex Learner\nalex@example.com | 555-0199 | linkedin.com/in/alexlearner\n\nTECHNICAL SKILLS\nLanguages: Java, SQL\nFrameworks: Spring Boot, REST APIs, Microservices\nTools: MySQL, Docker, Git\n\nEXPERIENCE\nSoftware Developer Intern\n- Engineered high-throughput REST APIs using Spring Boot and MySQL, improving request latency by 32%.\n- Containerized backend microservices using Docker to accelerate developer onboarding from 2 days to 15 minutes.\n- Automated CI/CD build scripts using Git and Maven.")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();
            v2 = resumeVersionRepository.save(v2);

            MatchReport report2 = MatchReport.builder()
                    .resumeVersion(v2)
                    .matchPercentage(88)
                    .missingKeywords("none")
                    .matchedKeywords("java, spring boot, mysql, sql, git, docker, microservices, rest api")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();
            matchReportRepository.save(report2);

            // 7. Seed Mentor Link
            MentorLink link = MentorLink.builder()
                    .learner(learner)
                    .mentor(mentor)
                    .shareCode("RADAR-DEMO-2026")
                    .active(true)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();
            mentorLinkRepository.save(link);

            // 8. Seed Mentor Comments
            ReviewComment comment1 = ReviewComment.builder()
                    .resumeVersion(v1)
                    .mentor(mentor)
                    .comment("In v1, replace passive verbs like 'Responsible for' with active impact verbs and add exact metrics!")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();
            reviewCommentRepository.save(comment1);

            ReviewComment comment2 = ReviewComment.builder()
                    .resumeVersion(v2)
                    .mentor(mentor)
                    .comment("Tremendous progress in v2! The 32% latency metric and Docker workflow bullet points are strong.")
                    .createdAt(LocalDateTime.now().minusHours(12))
                    .build();
            reviewCommentRepository.save(comment2);

            log.info("Seeded Demo Resume, Versions v1 & v2, ATS Issues, Match Reports, and Mentor Comments!");
        }
    }
}
