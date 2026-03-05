package com.cognivita.comunityservices.repository;

import com.cognivita.comunityservices.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Community> findByNameIgnoreCase(String name);
}
