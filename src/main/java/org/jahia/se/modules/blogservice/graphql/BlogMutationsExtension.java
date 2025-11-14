package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
public final class BlogMutationsExtension {

    private BlogMutationsExtension() {
        // utility
    }

    @GraphQLField
    @GraphQLName("blog")
    @GraphQLDescription("Blog related mutations")
    public static BlogMutations blog() {
        return new BlogMutations();
    }
}
