package com.resumeradar.service;

import com.resumeradar.dto.JobDescriptionDto;
import com.resumeradar.entity.JobDescription;
import com.resumeradar.entity.User;
import com.resumeradar.repository.JobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobDescriptionService {

    private final JobDescriptionRepository jobDescriptionRepository;

    @Transactional
    public JobDescription saveJobDescription(User user, JobDescriptionDto dto) {
        JobDescription jd;
        if (dto.getId() != null) {
            jd = jobDescriptionRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Job Description not found"));
            if (!jd.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You do not own this job description");
            }
            jd.setTitle(dto.getTitle().trim());
            jd.setText(dto.getText().trim());
            jd.setExtractedSkills(dto.getExtractedSkills());
        } else {
            jd = JobDescription.builder()
                    .user(user)
                    .title(dto.getTitle().trim())
                    .text(dto.getText().trim())
                    .extractedSkills(dto.getExtractedSkills())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        return jobDescriptionRepository.save(jd);
    }

    @Transactional(readOnly = true)
    public List<JobDescription> getUserJobDescriptions(User user) {
        return jobDescriptionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public JobDescription getJobDescription(Long id, User user) {
        JobDescription jd = jobDescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job Description not found: " + id));
        if (!jd.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized access to job description");
        }
        return jd;
    }

    @Transactional
    public void deleteJobDescription(Long id, User user) {
        JobDescription jd = getJobDescription(id, user);
        jobDescriptionRepository.delete(jd);
    }
}
