package com.cognivita.contentservice.service;

import com.cognivita.contentservice.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment create(Comment comment);
    List<Comment> findAll();
    Comment findById(Long id);
    List<Comment> findByPostId(Long postId);
    Comment update(Long id, Comment comment, String actingUser);
    void delete(Long id, String actingUser);
}
