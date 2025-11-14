package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.se.modules.blogservice.services.LikeResult;

@GraphQLName("LikePayload")
public class LikePayload {

    private final boolean success;
    private final String code;

    public LikePayload(boolean success, String code) {
        this.success = success;
        this.code = code;
    }

    public LikePayload(LikeResult result) {
        this(result.isSuccess(), result.getCode());
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
}
