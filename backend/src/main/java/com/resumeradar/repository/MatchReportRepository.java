package com.resumeradar.repository;

import com.resumeradar.entity.MatchReport;
import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchReportRepository extends JpaRepository<MatchReport, Long> {
    List<MatchReport> findByResumeVersionOrderByCreatedAtDesc(ResumeVersion resumeVersion);
    Optional<MatchReport> findTopByResumeVersionOrderByCreatedAtDesc(ResumeVersion resumeVersion);
    Optional<MatchReport> findByIdAndResumeVersion_Resume_User(Long id, User user);
}
