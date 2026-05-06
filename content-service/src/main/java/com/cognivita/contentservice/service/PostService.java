package com.cognivita.contentservice.service;

import com.cognivita.contentservice.entity.Post;

import java.util.List;

public interface PostService {
    Post create(Post post);
    List<Post> findAll();
    Post findById(Long id);
    List<Post> findByCommunityId(Long communityId);
    Post update(Long id, Post post, String actingUser);
    void delete(Long id, String actingUser);
}
