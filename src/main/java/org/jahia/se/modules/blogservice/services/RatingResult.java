package org.jahia.se.modules.blogservice.services;

/**
 * Result of a rating submission with statistics
 */
public class RatingResult {
    private final String blogPostId;
    private final double averageRating;
    private final int ratingCount;

    public RatingResult(String blogPostId, double averageRating, int ratingCount) {
        this.blogPostId = blogPostId;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public String getBlogPostId() {
        return blogPostId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }
}
