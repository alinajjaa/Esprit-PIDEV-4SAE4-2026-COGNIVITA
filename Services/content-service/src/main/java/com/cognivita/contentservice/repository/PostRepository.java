package com.cognivita.contentservice.repository;

import com.cognivita.contentservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCommunity_Id(Long communityId);
}
