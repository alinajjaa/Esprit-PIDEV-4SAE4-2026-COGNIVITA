package com.cognivita.contentservice.controller;

import com.cognivita.contentservice.dto.PostRequestDto;
import com.cognivita.contentservice.dto.PostResponseDto;
import com.cognivita.contentservice.entity.Community;
import com.cognivita.contentservice.entity.Post;
import com.cognivita.contentservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PostResponseDto> create(@Valid @RequestBody PostRequestDto request) {
        Post created = service.create(toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> findAll(@RequestParam(required = false) Long communityId) {
        if (communityId != null) {
            return ResponseEntity.ok(service.findByCommunityId(communityId).stream().map(this::toResponse).toList());
        }
        return ResponseEntity.ok(service.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> update(
            @PathVariable Long id,
            @RequestBody PostRequestDto request,
            @RequestHeader("X-User") String actingUser
    ) {
        return ResponseEntity.ok(toResponse(service.update(id, toEntity(request), actingUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User") String actingUser) {
        service.delete(id, actingUser);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    private Post toEntity(PostRequestDto request) {
        Post post = new Post();
        if (request.getCommunityId() != null) {
            Community community = new Community();
            community.setId(request.getCommunityId());
            post.setCommunity(community);
        }
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(request.getAuthor());
        return post;
    }

    private PostResponseDto toResponse(Post post) {
        Long communityId = post.getCommunity() != null ? post.getCommunity().getId() : null;
        return new PostResponseDto(
                post.getId(),
                communityId,
                post.getTitle(),
                post.getContent(),
                post.getAuthor()
        );
    }
}
