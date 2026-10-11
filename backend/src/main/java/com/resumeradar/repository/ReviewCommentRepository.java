package com.resumeradar.repository;

import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.ReviewComment;
import com.resumeradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByResumeVersionOrderByCreatedAtDesc(ResumeVersion resumeVersion);
    List<ReviewComment> findByMentorOrderByCreatedAtDesc(User mentor);
    long countByMentor(User mentor);
}
