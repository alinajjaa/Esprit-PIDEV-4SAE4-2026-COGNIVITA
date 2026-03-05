package com.cognivita.contentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter @Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 80)
    private String author;

    // Keep lists (bidirectional mapping)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Vote> votes = new ArrayList<>();

    // Helpers (important if you keep the lists)
    public void addComment(Comment c) {
        comments.add(c);
        c.setPost(this);
    }

    public void removeComment(Comment c) {
        comments.remove(c);
        c.setPost(null);
    }

    public void addVote(Vote v) {
        votes.add(v);
        v.setPost(this);
    }

    public void removeVote(Vote v) {
        votes.remove(v);
        v.setPost(null);
    }
}
