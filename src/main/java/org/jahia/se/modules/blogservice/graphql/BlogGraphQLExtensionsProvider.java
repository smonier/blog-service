package org.jahia.se.modules.blogservice.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

/**
 * Exposes this bundle's @GraphQLTypeExtension classes to the DX GraphQL provider.
 */
@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class BlogGraphQLExtensionsProvider implements DXGraphQLExtensionsProvider {
    // Marker component; no implementation required.
}
