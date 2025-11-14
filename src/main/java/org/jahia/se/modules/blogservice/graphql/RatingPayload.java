package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * GraphQL payload for rating operations
 */
@GraphQLName("BlogRatingPayload")
public class RatingPayload {
    private final String postId;
    private final double averageRating;
    private final int ratingCount;

    public RatingPayload(String postId, double averageRating, int ratingCount) {
        this.postId = postId;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    @GraphQLField
    public String getPostId() {
        return postId;
    }

    @GraphQLField
    public double getAverageRating() {
        return averageRating;
    }

    @GraphQLField
    public int getRatingCount() {
        return ratingCount;
    }
}
