package com.stockanalyzer.dto;

public class NewsItem {
    private String title;
    private String source;
    private String url;
    private String publishedAt;
    private String summary;
    private String imageUrl;

    public NewsItem() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
