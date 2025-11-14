package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public final class BlogQueriesExtension {

    private BlogQueriesExtension() {
        // utility
    }

    @GraphQLField
    @GraphQLName("blog")
    @GraphQLDescription("Blog related queries")
    public static BlogQueries blog() {
        return new BlogQueries();
    }
}
