package com.stockanalyzer.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "news")
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String source;

    @Column(nullable = false)
    private String url;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "image_url")
    private String imageUrl;

    public News() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
