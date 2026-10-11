package com.resumeradar.repository;

import com.resumeradar.entity.AtsIssue;
import com.resumeradar.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtsIssueRepository extends JpaRepository<AtsIssue, Long> {
    List<AtsIssue> findByResumeVersion(ResumeVersion resumeVersion);
}
