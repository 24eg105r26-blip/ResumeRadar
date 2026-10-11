package com.resumeradar.repository;

import com.resumeradar.entity.Resume;
import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
    List<ResumeVersion> findByResumeOrderByVersionNumberAsc(Resume resume);
    List<ResumeVersion> findByResumeOrderByVersionNumberDesc(Resume resume);
    Optional<ResumeVersion> findTopByResumeOrderByVersionNumberDesc(Resume resume);
    Optional<ResumeVersion> findByIdAndResume_User(Long id, User user);
}
