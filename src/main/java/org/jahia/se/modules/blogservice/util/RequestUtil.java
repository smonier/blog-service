package org.jahia.se.modules.blogservice.util;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import org.jahia.modules.graphql.provider.dxm.util.ContextUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class RequestUtil {

    private RequestUtil() {
    }

    public static Optional<HttpServletRequest> extractHttpServletRequest(DataFetchingEnvironment environment) {
        HttpServletRequest request = ContextUtil.getHttpServletRequest(environment.getContext());
        if (request != null) {
            return Optional.of(request);
        }

        GraphQLContext graphQLContext = environment.getGraphQlContext();
        if (graphQLContext != null) {
            request = graphQLContext.get(HttpServletRequest.class);
            if (request == null && graphQLContext.hasKey("httpServletRequest")) {
                request = graphQLContext.get("httpServletRequest");
            }
            if (request != null) {
                return Optional.of(request);
            }
        }

        return Optional.empty();
    }
}
