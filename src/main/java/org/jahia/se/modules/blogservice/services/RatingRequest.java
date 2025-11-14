package org.jahia.se.modules.blogservice.services;

/**
 * Request object for submitting a blog post rating
 */
public class RatingRequest {
    private final String blogPostId;
    private final int rating;
    private final String clientHash;
    private final String ipHash;
    private final String userAgent;

    private RatingRequest(Builder builder) {
        this.blogPostId = builder.blogPostId;
        this.rating = builder.rating;
        this.clientHash = builder.clientHash;
        this.ipHash = builder.ipHash;
        this.userAgent = builder.userAgent;
    }

    public String getBlogPostId() {
        return blogPostId;
    }

    public int getRating() {
        return rating;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String blogPostId;
        private int rating;
        private String clientHash;
        private String ipHash;
        private String userAgent;

        public Builder blogPostId(String blogPostId) {
            this.blogPostId = blogPostId;
            return this;
        }

        public Builder rating(int rating) {
            this.rating = rating;
            return this;
        }

        public Builder clientHash(String clientHash) {
            this.clientHash = clientHash;
            return this;
        }

        public Builder ipHash(String ipHash) {
            this.ipHash = ipHash;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public RatingRequest build() {
            return new RatingRequest(this);
        }
    }
}
