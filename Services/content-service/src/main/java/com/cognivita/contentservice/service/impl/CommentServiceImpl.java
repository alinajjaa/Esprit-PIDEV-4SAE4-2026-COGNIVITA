package com.cognivita.contentservice.service.impl;

import com.cognivita.contentservice.entity.Comment;
import com.cognivita.contentservice.entity.Post;
import com.cognivita.contentservice.repository.CommentRepository;
import com.cognivita.contentservice.repository.PostRepository;
import com.cognivita.contentservice.security.ForumUserRegistry;
import com.cognivita.contentservice.service.CommentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository repository;
    private final PostRepository postRepository;
    private final ForumUserRegistry userRegistry;

    public CommentServiceImpl(
            CommentRepository repository,
            PostRepository postRepository,
            ForumUserRegistry userRegistry
    ) {
        this.repository = repository;
        this.postRepository = postRepository;
        this.userRegistry = userRegistry;
    }

    @Override
    public Comment create(Comment comment) {
        comment.setAuthor(userRegistry.requireValidUser(comment.getAuthor(), "author"));
        if (comment.getPost() == null || comment.getPost().getId() == null) {
            throw new IllegalArgumentException("post.id is required");
        }
        comment.setPost(postRepository.findById(comment.getPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + comment.getPost().getId())));
        return repository.save(comment);
    }

    @Override
    public List<Comment> findAll() {
        return repository.findAll();
    }

    @Override
    public Comment findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + id));
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return repository.findByPost_Id(postId);
    }

    @Override
    public Comment update(Long id, Comment comment, String actingUser) {
        Comment existing = findById(id);
        String actor = userRegistry.requireValidUser(actingUser, "X-User");
        if (!canManageComment(existing, actor)) {
            throw new SecurityException("Only the comment owner or the post owner can edit this comment");
        }

        boolean changed = false;

        if (comment.getPost() != null && comment.getPost().getId() != null) {
            existing.setPost(postRepository.findById(comment.getPost().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + comment.getPost().getId())));
            changed = true;
        }
        if (comment.getContent() != null) {
            existing.setContent(comment.getContent());
            changed = true;
        }
        if (comment.getAuthor() != null) {
            existing.setAuthor(userRegistry.requireValidUser(comment.getAuthor(), "author"));
            changed = true;
        }

        if (!changed) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        return repository.save(existing);
    }

    @Override
    public void delete(Long id, String actingUser) {
        Comment existing = findById(id);
        String actor = userRegistry.requireValidUser(actingUser, "X-User");
        if (!canManageComment(existing, actor)) {
            throw new SecurityException("Only the comment owner or the post owner can delete this comment");
        }
        repository.deleteById(id);
    }

    private boolean canManageComment(Comment comment, String actor) {
        if (actor.equals(comment.getAuthor())) {
            return true;
        }
        Post post = postRepository.findById(comment.getPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + comment.getPost().getId()));
        return actor.equals(post.getAuthor());
    }
}
