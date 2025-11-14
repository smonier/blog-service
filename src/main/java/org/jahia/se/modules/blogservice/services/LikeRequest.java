package org.jahia.se.modules.blogservice.services;

import java.util.Calendar;

public class LikeRequest {

    private final String blogPostId;
    private final String clientHash;
    private final String ipHash;
    private final String userAgent;
    private final Calendar timestamp;

    private LikeRequest(Builder builder) {
        this.blogPostId = builder.blogPostId;
        this.clientHash = builder.clientHash;
        this.ipHash = builder.ipHash;
        this.userAgent = builder.userAgent;
        this.timestamp = builder.timestamp != null ? (Calendar) builder.timestamp.clone() : Calendar.getInstance();
    }

    public String getBlogPostId() {
        return blogPostId;
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

    public static Builder builder(String blogPostId) {
        return new Builder(blogPostId);
    }

    public static class Builder {
        private final String blogPostId;
        private String clientHash;
        private String ipHash;
        private String userAgent;
        private Calendar timestamp;

        private Builder(String blogPostId) {
            this.blogPostId = blogPostId;
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

        public LikeRequest build() {
            return new LikeRequest(this);
        }
    }
}
