package com.cognivita.contentservice.dto;

public class PostResponseDto {

    private Long id;
    private Long communityId;
    private String title;
    private String content;
    private String author;

    public PostResponseDto() {
    }

    public PostResponseDto(Long id, Long communityId, String title, String content, String author) {
        this.id = id;
        this.communityId = communityId;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
