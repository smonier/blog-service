package org.jahia.se.modules.blogservice.services;

import org.apache.commons.lang3.StringUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

/**
 * Handles the JCR operations required to persist blog ratings under the LIVE workspace as UGC.
 */
@Component(service = BlogRatingService.class, immediate = true)
public class BlogRatingService {

    private static final Logger logger = LoggerFactory.getLogger(BlogRatingService.class);

    /**
     * Submit a rating for a blog post
     * @param request The rating request
     * @return Rating result with statistics
     * @throws BlogServiceException if submission fails
     */
    public RatingResult submit(RatingRequest request) throws BlogServiceException {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Submitting rating {} for blogPost={} clientHashPresent={} ipHashPresent={}",
                        request.getRating(), request.getBlogPostId(),
                        StringUtils.isNotBlank(request.getClientHash()), StringUtils.isNotBlank(request.getIpHash()));
            }
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<RatingResult>) session -> execute(session, request));
        } catch (RepositoryException e) {
            logger.error("Rating persistence failed for blogPost={}", request.getBlogPostId(), e);
            throw new BlogServiceException("Unable to execute rating persistence", e);
        }
    }

    private RatingResult execute(JCRSessionWrapper session, RatingRequest request) throws RepositoryException {
        JCRNodeWrapper blogPost = session.getNodeByUUID(request.getBlogPostId());

        JCRSiteNode site = blogPost.getResolveSite();
        if (site == null) {
            throw new RepositoryException("Unable to resolve site for blog post " + request.getBlogPostId());
        }

        String basePath = "/sites/" + site.getName() + "/contents/ugc/blogs/" + request.getBlogPostId() + "/ratings";
        if (logger.isDebugEnabled()) {
            logger.debug("Using ratings base path {}", basePath);
        }
        JCRNodeWrapper ratingsFolder = getOrCreateRatingsFolder(session, basePath);
        if (logger.isDebugEnabled()) {
            logger.debug("Ratings folder primary type={} mixins={}", ratingsFolder.getPrimaryNodeTypeName(),
                    Arrays.toString(ratingsFolder.getMixinNodeTypes()));
        }

        // Check for existing rating and update or create new
        JCRNodeWrapper existingRating = findExistingRating(ratingsFolder, request);
        if (existingRating != null) {
            logger.info("Updating existing rating for blogPost={}", request.getBlogPostId());
            existingRating.setProperty("rating", request.getRating());
            existingRating.setProperty("ts", Calendar.getInstance());
        } else {
            String ratingNodeName = "r-" + UUID.randomUUID();
            JCRNodeWrapper ratingNode = ratingsFolder.addNode(ratingNodeName, "jsblognt:rating");

            ratingNode.setProperty("blogPostId", request.getBlogPostId());
            ratingNode.setProperty("rating", request.getRating());

            if (StringUtils.isNotBlank(request.getClientHash())) {
                ratingNode.setProperty("clientHash", request.getClientHash());
            }

            if (StringUtils.isNotBlank(request.getIpHash())) {
                ratingNode.setProperty("ipHash", request.getIpHash());
            }

            if (StringUtils.isNotBlank(request.getUserAgent())) {
                ratingNode.setProperty("ua", request.getUserAgent());
            }

            ratingNode.setProperty("ts", Calendar.getInstance());

            logger.info("Created new rating node {} for blogPost={}", ratingNode.getPath(), request.getBlogPostId());
        }

        session.save();

        // Calculate average and count
        RatingStats stats = calculateRatingStats(ratingsFolder);

        return new RatingResult(request.getBlogPostId(), stats.getAverageRating(), stats.getRatingCount());
    }

    private JCRNodeWrapper findExistingRating(JCRNodeWrapper ratingsFolder, RatingRequest request) throws RepositoryException {
        NodeIterator iterator = ratingsFolder.getNodes();
        while (iterator.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
            if (node.isNodeType("jsblognt:rating")) {
                boolean clientMatch = false;
                boolean ipMatch = false;

                if (StringUtils.isNotBlank(request.getClientHash()) && node.hasProperty("clientHash")) {
                    clientMatch = request.getClientHash().equals(node.getProperty("clientHash").getString());
                }

                if (StringUtils.isNotBlank(request.getIpHash()) && node.hasProperty("ipHash")) {
                    ipMatch = request.getIpHash().equals(node.getProperty("ipHash").getString());
                }

                if (clientMatch || ipMatch) {
                    return node;
                }
            }
        }
        return null;
    }

    private RatingStats calculateRatingStats(JCRNodeWrapper ratingsFolder) throws RepositoryException {
        NodeIterator iterator = ratingsFolder.getNodes();
        long totalRating = 0;
        int count = 0;

        while (iterator.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
            if (node.isNodeType("jsblognt:rating") && node.hasProperty("rating")) {
                totalRating += node.getProperty("rating").getLong();
                count++;
            }
        }

        double average = count > 0 ? (double) totalRating / count : 0.0;
        return new RatingStats(average, count);
    }

    private JCRNodeWrapper getOrCreateRatingsFolder(JCRSessionWrapper session, String basePath) throws RepositoryException {
        if (session.nodeExists(basePath)) {
            return session.getNode(basePath);
        }

        String[] segments = StringUtils.split(basePath, '/');
        JCRNodeWrapper current = session.getRootNode();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (StringUtils.isBlank(segment)) {
                continue;
            }
            if (current.hasNode(segment)) {
                current = current.getNode(segment);
            } else {
                String nodeType = (i == segments.length - 1) ? "jsblognt:ratingsFolder" : "jnt:contentFolder";
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating node {} of type {} under {}", segment, nodeType, current.getPath());
                }
                current = current.addNode(segment, nodeType);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Created ratings folder structure at {}", current.getPath());
        }
        return current;
    }

    /**
     * Get rating statistics for a blog post
     * @param blogPostId The blog post UUID
     * @return Rating statistics
     * @throws BlogServiceException if retrieval fails
     */
    public RatingStats getRatingStats(String blogPostId) throws BlogServiceException {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<RatingStats>) session -> retrieveRatingStats(session, blogPostId));
        } catch (RepositoryException e) {
            logger.error("Failed to retrieve rating stats for blogPost={}", blogPostId, e);
            throw new BlogServiceException("Unable to retrieve rating stats", e);
        }
    }

    private RatingStats retrieveRatingStats(JCRSessionWrapper session, String blogPostId) throws RepositoryException {
        JCRNodeWrapper blogPost = session.getNodeByUUID(blogPostId);
        JCRSiteNode site = blogPost.getResolveSite();

        String basePath = String.format("/sites/%s/contents/ugc/blogs/%s/ratings", site.getName(), blogPostId);

        if (!session.nodeExists(basePath)) {
            logger.debug("No ratings folder exists for blogPost={}", blogPostId);
            return new RatingStats(0.0, 0);
        }

        JCRNodeWrapper ratingsFolder = session.getNode(basePath);
        return calculateRatingStats(ratingsFolder);
    }

    /**
     * Data transfer object for rating statistics
     */
    public static class RatingStats {
        private final double averageRating;
        private final int ratingCount;

        public RatingStats(double averageRating, int ratingCount) {
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public int getRatingCount() {
            return ratingCount;
        }
    }
}
