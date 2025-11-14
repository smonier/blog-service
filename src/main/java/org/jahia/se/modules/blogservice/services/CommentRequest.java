package org.jahia.se.modules.blogservice.services;

import java.util.Calendar;

public class CommentRequest {

    private final String blogPostId;
    private final String comment;
    private final String author;
    private final String authorEmail;
    private final String clientHash;
    private final String ipHash;
    private final String userAgent;
    private final Calendar timestamp;

    private CommentRequest(Builder builder) {
        this.blogPostId = builder.blogPostId;
        this.comment = builder.comment;
        this.author = builder.author;
        this.authorEmail = builder.authorEmail;
        this.clientHash = builder.clientHash;
        this.ipHash = builder.ipHash;
        this.userAgent = builder.userAgent;
        this.timestamp = builder.timestamp != null ? (Calendar) builder.timestamp.clone() : Calendar.getInstance();
    }

    public String getBlogPostId() {
        return blogPostId;
    }

    public String getComment() {
        return comment;
    }

    public String getAuthor() {
        return author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getClientHash() {
        return clientHash;
    }

    public String getIpHash() {
        return ipHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Calendar getTimestamp() {
        return (Calendar) timestamp.clone();
    }

    public static Builder builder(String blogPostId, String comment) {
        return new Builder(blogPostId, comment);
    }

    public static class Builder {
        private final String blogPostId;
        private final String comment;
        private String author;
        private String authorEmail;
        private String clientHash;
        private String ipHash;
        private String userAgent;
        private Calendar timestamp;

        private Builder(String blogPostId, String comment) {
            this.blogPostId = blogPostId;
            this.comment = comment;
        }

        public Builder withAuthor(String author) {
            this.author = author;
            return this;
        }

        public Builder withAuthorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
            return this;
        }

        public Builder withClientHash(String clientHash) {
            this.clientHash = clientHash;
            return this;
        }

        public Builder withIpHash(String ipHash) {
            this.ipHash = ipHash;
            return this;
        }

        public Builder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder withTimestamp(Calendar timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public CommentRequest build() {
            return new CommentRequest(this);
        }
    }
}
