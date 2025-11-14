package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("CommentStatusPayload")
@GraphQLDescription("Payload for comment status update operations")
public class CommentStatusPayload {
    private final boolean success;
    private final String message;

    public CommentStatusPayload(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    @GraphQLField
    @GraphQLName("success")
    @GraphQLDescription("Whether the operation was successful")
    public boolean isSuccess() {
        return success;
    }

    @GraphQLField
    @GraphQLName("message")
    @GraphQLDescription("Status message")
    public String getMessage() {
        return message;
    }
}
