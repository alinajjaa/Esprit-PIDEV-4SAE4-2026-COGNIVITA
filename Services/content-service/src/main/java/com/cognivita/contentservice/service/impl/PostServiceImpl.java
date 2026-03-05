package com.cognivita.contentservice.service.impl;

import com.cognivita.contentservice.entity.Post;
import com.cognivita.contentservice.repository.PostRepository;
import com.cognivita.contentservice.security.ForumUserRegistry;
import com.cognivita.contentservice.service.PostService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository repository;
    private final ForumUserRegistry userRegistry;

    public PostServiceImpl(PostRepository repository, ForumUserRegistry userRegistry) {
        this.repository = repository;
        this.userRegistry = userRegistry;
    }

    @Override
    public Post create(Post post) {
        post.setAuthor(userRegistry.requireValidUser(post.getAuthor(), "author"));
        return repository.save(post);
    }

    @Override
    public List<Post> findAll() {
        return repository.findAll();
    }

    @Override
    public Post findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + id));
    }

    @Override
    public List<Post> findByCommunityId(Long communityId) {
        return repository.findByCommunityId(communityId);
    }

    @Override
    public Post update(Long id, Post post, String actingUser) {
        Post existing = findById(id);
        String actor = userRegistry.requireValidUser(actingUser, "X-User");
        if (!existing.getAuthor().equals(actor)) {
            throw new SecurityException("Only the post owner can edit this post");
        }

        boolean changed = false;

        if (post.getCommunityId() != null) {
            existing.setCommunityId(post.getCommunityId());
            changed = true;
        }
        if (post.getTitle() != null) {
            existing.setTitle(post.getTitle());
            changed = true;
        }
        if (post.getContent() != null) {
            existing.setContent(post.getContent());
            changed = true;
        }
        if (post.getAuthor() != null) {
            existing.setAuthor(userRegistry.requireValidUser(post.getAuthor(), "author"));
            changed = true;
        }

        if (!changed) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        return repository.save(existing);
    }

    @Override
    public void delete(Long id, String actingUser) {
        Post existing = findById(id);
        String actor = userRegistry.requireValidUser(actingUser, "X-User");
        if (!existing.getAuthor().equals(actor)) {
            throw new SecurityException("Only the post owner can delete this post");
        }
        repository.deleteById(id);
    }
}
