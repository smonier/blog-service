package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * GraphQL representation of a blog comment
 */
@GraphQLName("BlogComment")
public class Comment {
    private final String uuid;
    private final String authorName;
    private final String body;
    private final String created;
    private final String status;

    public Comment(String uuid, String authorName, String body, String created, String status) {
        this.uuid = uuid;
        this.authorName = authorName;
        this.body = body;
        this.created = created;
        this.status = status;
    }

    @GraphQLField
    public String getUuid() {
        return uuid;
    }

    @GraphQLField
    public String getAuthorName() {
        return authorName;
    }

    @GraphQLField
    public String getBody() {
        return body;
    }

    @GraphQLField
    public String getCreated() {
        return created;
    }

    @GraphQLField
    public String getStatus() {
        return status;
    }
}
