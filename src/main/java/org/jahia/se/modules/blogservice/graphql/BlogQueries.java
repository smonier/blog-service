package org.jahia.se.modules.blogservice.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.se.modules.blogservice.services.BlogCommentService;
import org.jahia.se.modules.blogservice.services.BlogRatingService;
import org.jahia.se.modules.blogservice.services.BlogServiceException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Blog GraphQL queries implementation
 */
public class BlogQueries {

    private static final Logger logger = LoggerFactory.getLogger(BlogQueries.class);

    @Inject
    @GraphQLOsgiService
    private BlogCommentService blogCommentService;

    @Inject
    @GraphQLOsgiService
    private BlogRatingService blogRatingService;

    /**
     * Retrieve all comments for a blog post
     * @param postId The blog post UUID
     * @return Comments payload with list of comments and total count
     */
    @GraphQLField
    @GraphQLName("getComments")
    public CommentsPayload getComments(@GraphQLName("postId") @GraphQLNonNull String postId) {
        try {
            List<BlogCommentService.CommentData> commentDataList = blogCommentService.getComments(postId);
            
            List<Comment> comments = commentDataList.stream()
                    .map(data -> new Comment(
                            data.getUuid(),
                            data.getAuthorName(),
                            data.getBody(),
                            data.getCreated(),
                            data.getStatus()
                    ))
                    .collect(Collectors.toList());

            return new CommentsPayload(postId, comments, comments.size());
        } catch (BlogServiceException e) {
            logger.error("Failed to retrieve comments for post={}", postId, e);
            throw new RuntimeException("Failed to retrieve comments", e);
        }
    }

    /**
     * Retrieve rating statistics for a blog post
     * @param postId The blog post UUID
     * @return Rating payload with average rating and count
     */
    @GraphQLField
    @GraphQLName("getRating")
    public RatingPayload getRating(@GraphQLName("postId") @GraphQLNonNull String postId) {
        try {
            BlogRatingService.RatingStats stats = blogRatingService.getRatingStats(postId);
            return new RatingPayload(postId, stats.getAverageRating(), stats.getRatingCount());
        } catch (BlogServiceException e) {
            logger.error("Failed to retrieve rating stats for post={}", postId, e);
            throw new RuntimeException("Failed to retrieve rating stats", e);
        }
    }
}
