package com.cognivita.contentservice.service;

import com.cognivita.contentservice.entity.Vote;

import java.util.List;

public interface VoteService {
    Vote createOrUpdate(Vote vote);
    List<Vote> findAll();
    Vote findById(Long id);
    List<Vote> findByPostId(Long postId);
    void delete(Long id);
}
