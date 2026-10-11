package com.resumeradar.repository;

import com.resumeradar.entity.MentorLink;
import com.resumeradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorLinkRepository extends JpaRepository<MentorLink, Long> {
    Optional<MentorLink> findByShareCode(String shareCode);
    List<MentorLink> findByLearnerOrderByCreatedAtDesc(User learner);
    List<MentorLink> findByMentorAndActiveTrueOrderByCreatedAtDesc(User mentor);
    Optional<MentorLink> findByMentorAndLearner(User mentor, User learner);
    boolean existsByMentorAndLearnerAndActiveTrue(User mentor, User learner);
}
