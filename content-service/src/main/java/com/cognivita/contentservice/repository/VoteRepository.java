package com.cognivita.contentservice.repository;

import com.cognivita.contentservice.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByPost_Id(Long postId);
    Optional<Vote> findByPost_IdAndUsername(Long postId, String username);
}
