package com.resumeradar.service;

import com.resumeradar.dto.ReviewCommentDto;
import com.resumeradar.dto.ScoreTrendDto;
import com.resumeradar.entity.MentorLink;
import com.resumeradar.entity.Resume;
import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.ReviewComment;
import com.resumeradar.entity.User;
import com.resumeradar.repository.MentorLinkRepository;
import com.resumeradar.repository.ResumeRepository;
import com.resumeradar.repository.ResumeVersionRepository;
import com.resumeradar.repository.ReviewCommentRepository;
import com.resumeradar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MentorService {

    private final MentorLinkRepository mentorLinkRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final UserRepository userRepository;
    private final HealthScoreService healthScoreService;

    @Transactional
    public String generateShareCode(User learner) {
        List<MentorLink> existing = mentorLinkRepository.findByLearnerOrderByCreatedAtDesc(learner);
        for (MentorLink link : existing) {
            if (link.isActive() && link.getMentor() == null) {
                return link.getShareCode();
            }
        }
        String shareCode = "RADAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        MentorLink link = MentorLink.builder()
                .learner(learner)
                .mentor(null)
                .shareCode(shareCode)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        mentorLinkRepository.save(link);
        return shareCode;
    }

    @Transactional
    public MentorLink linkMentor(User mentor, String shareCode) {
        String cleanCode = shareCode != null ? shareCode.trim().toUpperCase() : "";
        MentorLink link = mentorLinkRepository.findByShareCode(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid share code. Please double-check with the student."));

        if (!link.isActive()) {
            throw new IllegalArgumentException("This share code is no longer active.");
        }
        if (link.getLearner().getId().equals(mentor.getId())) {
            throw new IllegalArgumentException("You cannot link to your own account as mentor.");
        }

        link.setMentor(mentor);
        return mentorLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<MentorLink> getLinkedLearners(User mentor) {
        return mentorLinkRepository.findByMentorAndActiveTrueOrderByCreatedAtDesc(mentor);
    }

    @Transactional(readOnly = true)
    public List<MentorLink> getLearnerMentors(User learner) {
        return mentorLinkRepository.findByLearnerOrderByCreatedAtDesc(learner);
    }

    /**
     * Strict privacy guarantee: Mentors see ONLY score trends and metadata, NOT the raw resume text.
     */
    @Transactional(readOnly = true)
    public List<ScoreTrendDto> getLearnerScoreTrends(User mentor, Long learnerId) {
        User learner = userRepository.findById(learnerId)
                .orElseThrow(() -> new IllegalArgumentException("Learner not found"));

        boolean isLinked = mentorLinkRepository.existsByMentorAndLearnerAndActiveTrue(mentor, learner);
        if (!isLinked) {
            throw new AccessDeniedException("You are not linked as an active mentor for this learner.");
        }

        List<Resume> resumes = resumeRepository.findByUserOrderByCreatedAtDesc(learner);
        List<ScoreTrendDto> trends = new ArrayList<>();
        for (Resume resume : resumes) {
            List<ResumeVersion> versions = resumeVersionRepository.findByResumeOrderByVersionNumberAsc(resume);
            for (ResumeVersion v : versions) {
                trends.add(ScoreTrendDto.builder()
                        .versionId(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .healthScore(v.getHealthScore())
                        .scoreBand(healthScoreService.getScoreBand(v.getHealthScore()))
                        .createdAt(v.getCreatedAt())
                        .atsIssueCount(v.getAtsIssues() != null ? v.getAtsIssues().size() : 0)
                        .commentCount(v.getReviewComments() != null ? v.getReviewComments().size() : 0)
                        .build());
            }
        }
        return trends;
    }

    @Transactional
    public ReviewComment addReviewComment(User mentor, ReviewCommentDto dto) {
        ResumeVersion version = resumeVersionRepository.findById(dto.getResumeVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found"));

        User learner = version.getResume().getUser();
        boolean isLinked = mentorLinkRepository.existsByMentorAndLearnerAndActiveTrue(mentor, learner);
        if (!isLinked) {
            throw new AccessDeniedException("You are not authorized to comment on this learner's resume.");
        }

        ReviewComment comment = ReviewComment.builder()
                .resumeVersion(version)
                .mentor(mentor)
                .comment(dto.getComment().trim())
                .createdAt(LocalDateTime.now())
                .build();

        return reviewCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<ReviewComment> getCommentsForVersion(Long versionId, User user) {
        ResumeVersion version = resumeVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found"));

        User owner = version.getResume().getUser();
        boolean isOwner = owner.getId().equals(user.getId());
        boolean isLinkedMentor = mentorLinkRepository.existsByMentorAndLearnerAndActiveTrue(user, owner);

        if (!isOwner && !isLinkedMentor && !user.hasRole("ADMIN")) {
            throw new AccessDeniedException("Unauthorized to view comments for this version");
        }

        return reviewCommentRepository.findByResumeVersionOrderByCreatedAtDesc(version);
    }
}
