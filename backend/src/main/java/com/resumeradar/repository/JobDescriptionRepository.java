package com.resumeradar.repository;

import com.resumeradar.entity.JobDescription;
import com.resumeradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findByUserOrderByCreatedAtDesc(User user);
    Optional<JobDescription> findByIdAndUser(Long id, User user);
    long countByUser(User user);
}
