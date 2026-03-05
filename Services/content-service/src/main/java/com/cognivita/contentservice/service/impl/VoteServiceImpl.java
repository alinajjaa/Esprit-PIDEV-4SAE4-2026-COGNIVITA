package com.cognivita.contentservice.service.impl;

import com.cognivita.contentservice.entity.Vote;
import com.cognivita.contentservice.repository.PostRepository;
import com.cognivita.contentservice.repository.VoteRepository;
import com.cognivita.contentservice.security.ForumUserRegistry;
import com.cognivita.contentservice.service.VoteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoteServiceImpl implements VoteService {

    private final VoteRepository repository;
    private final PostRepository postRepository;
    private final ForumUserRegistry userRegistry;

    public VoteServiceImpl(
            VoteRepository repository,
            PostRepository postRepository,
            ForumUserRegistry userRegistry
    ) {
        this.repository = repository;
        this.postRepository = postRepository;
        this.userRegistry = userRegistry;
    }

    @Override
    public Vote createOrUpdate(Vote vote) {
        if (vote.getValue() == null || (vote.getValue() != 1 && vote.getValue() != -1)) {
            throw new IllegalArgumentException("Vote value must be 1 or -1");
        }
        if (vote.getPost() == null || vote.getPost().getId() == null) {
            throw new IllegalArgumentException("post.id is required");
        }
        vote.setUsername(userRegistry.requireValidUser(vote.getUsername(), "username"));

        Long postId = vote.getPost().getId();
        vote.setPost(postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId)));

        return repository.findByPost_IdAndUsername(postId, vote.getUsername())
                .map(existing -> {
                    existing.setValue(vote.getValue());
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(vote));
    }

    @Override
    public List<Vote> findAll() {
        return repository.findAll();
    }

    @Override
    public Vote findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found with id: " + id));
    }

    @Override
    public List<Vote> findByPostId(Long postId) {
        return repository.findByPost_Id(postId);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Vote not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
