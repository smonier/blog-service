package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.se.modules.blogservice.services.CommentResult;

@GraphQLName("CommentPayload")
public class CommentPayload {

    private final boolean success;
    private final String code;
    private final String commentId;

    public CommentPayload(boolean success, String code, String commentId) {
        this.success = success;
        this.code = code;
        this.commentId = commentId;
    }

    public CommentPayload(CommentResult result) {
        this(result.isSuccess(), result.getCode(), result.getCommentId());
    }

    @GraphQLField
    @GraphQLNonNull
    public boolean isSuccess() {
        return success;
    }

    @GraphQLField
    @GraphQLNonNull
    public String getCode() {
        return code;
    }

    @GraphQLField
    public String getCommentId() {
        return commentId;
    }

    @GraphQLField
    @GraphQLName("uuid")
    public String getUuid() {
        return commentId;
    }

    @GraphQLField
    @GraphQLName("status")
    @GraphQLNonNull
    public String getStatus() {
        return code;
    }

    @GraphQLField
    @GraphQLName("message")
    public String getMessage() {
        return success ? "Comment created successfully" : "Comment creation failed: " + code;
    }
}
