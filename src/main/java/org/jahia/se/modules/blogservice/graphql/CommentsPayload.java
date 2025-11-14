package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

import java.util.List;

/**
 * GraphQL payload for getComments query
 */
@GraphQLName("BlogCommentsPayload")
public class CommentsPayload {
    private final String postId;
    private final List<Comment> comments;
    private final int total;

    public CommentsPayload(String postId, List<Comment> comments, int total) {
        this.postId = postId;
        this.comments = comments;
        this.total = total;
    }

    @GraphQLField
    public String getPostId() {
        return postId;
    }

    @GraphQLField
    public List<Comment> getComments() {
        return comments;
    }

    @GraphQLField
    public int getTotal() {
        return total;
    }
}
