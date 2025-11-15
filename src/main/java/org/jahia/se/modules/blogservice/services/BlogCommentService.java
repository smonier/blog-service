package org.jahia.se.modules.blogservice.services;

import org.apache.commons.lang3.StringUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Handles the JCR operations required to persist blog comments under the LIVE workspace as UGC.
 */
@Component(service = BlogCommentService.class, immediate = true)
public class BlogCommentService {

    private static final Logger logger = LoggerFactory.getLogger(BlogCommentService.class);

    @Reference
    private BlogConfigurationService configurationService;

    public CommentResult submit(CommentRequest request) throws BlogServiceException {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Submitting comment for blogPost={} author={} clientHashPresent={} ipHashPresent={}",
                        request.getBlogPostId(), request.getAuthor(),
                        StringUtils.isNotBlank(request.getClientHash()), StringUtils.isNotBlank(request.getIpHash()));
            }
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<CommentResult>) session -> execute(session, request));
        } catch (RepositoryException e) {
            logger.error("Comment persistence failed for blogPost={}", request.getBlogPostId(), e);
            throw new BlogServiceException("Unable to execute comment persistence", e);
        }
    }

    private CommentResult execute(JCRSessionWrapper session, CommentRequest request) throws RepositoryException {
        try {
            JCRNodeWrapper blogPost = session.getNodeByUUID(request.getBlogPostId());

            JCRSiteNode site = blogPost.getResolveSite();
            if (site == null) {
                logger.error("Unable to resolve site for blog post {}", request.getBlogPostId());
                throw new RepositoryException("Unable to resolve site for blog post " + request.getBlogPostId());
            }

            String basePath = "/sites/" + site.getName() + "/contents/ugc/blogs/" + request.getBlogPostId() + "/comments";
            if (logger.isDebugEnabled()) {
                logger.debug("Using comments base path {}", basePath);
            }
        } catch (RepositoryException e) {
            logger.error("Failed to resolve blog post or site for UUID={}", request.getBlogPostId(), e);
            throw e;
        }
        
        JCRNodeWrapper blogPost = session.getNodeByUUID(request.getBlogPostId());
        JCRSiteNode site = blogPost.getResolveSite();
        
        String basePath = "/sites/" + site.getName() + "/contents/ugc/blogs/" + request.getBlogPostId() + "/comments";
        if (logger.isDebugEnabled()) {
            logger.debug("Using comments base path {}", basePath);
        }
        JCRNodeWrapper commentsFolder = getOrCreateCommentsFolder(session, basePath);
        if (logger.isDebugEnabled()) {
            logger.debug("Comments folder primary type={} mixins={}", commentsFolder.getPrimaryNodeTypeName(),
                    Arrays.toString(commentsFolder.getMixinNodeTypes()));
        }

        if (isDuplicateComment(commentsFolder, request)) {
            logger.info("Rejected duplicate comment for blogPost={}", request.getBlogPostId());
            return new CommentResult(false, CommentResult.CODE_DUPLICATE);
        }

        String commentNodeName = "c-" + UUID.randomUUID();
        JCRNodeWrapper commentNode = commentsFolder.addNode(commentNodeName, "jsblognt:comment");

        commentNode.setProperty("blogPostId", request.getBlogPostId());
        commentNode.setProperty("comment", request.getComment());

        if (StringUtils.isNotBlank(request.getAuthor())) {
            commentNode.setProperty("author", request.getAuthor());
        }

        if (StringUtils.isNotBlank(request.getAuthorEmail())) {
            commentNode.setProperty("authorEmail", request.getAuthorEmail());
        }

        if (StringUtils.isNotBlank(request.getClientHash())) {
            commentNode.setProperty("clientHash", request.getClientHash());
        }

        if (StringUtils.isNotBlank(request.getIpHash())) {
            commentNode.setProperty("ipHash", request.getIpHash());
        }

        if (StringUtils.isNotBlank(request.getUserAgent())) {
            commentNode.setProperty("ua", request.getUserAgent());
        }

        Calendar timestamp = request.getTimestamp();
        if (timestamp != null) {
            commentNode.setProperty("ts", timestamp);
        }

        // Comments require approval based on configuration
        boolean requiresModeration = configurationService.isRequireModeration();
        logger.info("DEBUG: requiresModeration value from config: {}", requiresModeration);
        logger.info("DEBUG: Setting approved property to: {}", !requiresModeration);
        logger.info("DEBUG: Setting status property to: {}", requiresModeration ? "pending" : "approved");
        
        commentNode.setProperty("approved", !requiresModeration);
        commentNode.setProperty("status", requiresModeration ? "pending" : "approved");

        session.save();
        
        if (logger.isInfoEnabled()) {
            logger.info("Comment persisted for blogPost={} node={} status={} approved={} requiresModeration={}", 
                    request.getBlogPostId(), commentNode.getPath(), 
                    commentNode.getProperty("status").getString(),
                    commentNode.getProperty("approved").getBoolean(),
                    configurationService.isRequireModeration());
        }
        
        String resultCode = configurationService.isRequireModeration() ? 
                CommentResult.CODE_MODERATION : CommentResult.CODE_OK;
        return new CommentResult(true, resultCode, commentNode.getIdentifier());
    }

    private boolean isDuplicateComment(JCRNodeWrapper commentsFolder, CommentRequest request) throws RepositoryException {
        if (!commentsFolder.hasNodes()) {
            return false;
        }

        NodeIterator nodes = commentsFolder.getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper comment = (JCRNodeWrapper) nodes.nextNode();
            
            // Check for duplicate by client hash
            if (StringUtils.isNotBlank(request.getClientHash()) && comment.hasProperty("clientHash")
                    && request.getClientHash().equals(comment.getProperty("clientHash").getString())) {
                // Check if same comment content (prevent duplicate submission)
                if (comment.hasProperty("comment") && 
                    request.getComment().equals(comment.getProperty("comment").getString())) {
                    return true;
                }
            }
            
            // Check for recent duplicate from same IP
            if (StringUtils.isNotBlank(request.getIpHash()) && comment.hasProperty("ipHash")
                    && request.getIpHash().equals(comment.getProperty("ipHash").getString())) {
                if (comment.hasProperty("comment") && 
                    request.getComment().equals(comment.getProperty("comment").getString())) {
                    // Check if within last minute
                    if (comment.hasProperty("ts")) {
                        Calendar commentTime = comment.getProperty("ts").getDate();
                        Calendar oneMinuteAgo = Calendar.getInstance();
                        oneMinuteAgo.add(Calendar.MINUTE, -1);
                        if (commentTime.after(oneMinuteAgo)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private JCRNodeWrapper getOrCreateCommentsFolder(JCRSessionWrapper session, String basePath) throws RepositoryException {
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
                String nodeType = (i == segments.length - 1) ? "jsblognt:commentsFolder" : "jnt:contentFolder";
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating node {} of type {} under {}", segment, nodeType, current.getPath());
                }
                current = current.addNode(segment, nodeType);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Created comments folder structure at {}", current.getPath());
        }
        return current;
    }

    /**
     * Retrieves all comments for a blog post
     * @param blogPostId The UUID of the blog post
     * @return List of comment nodes with their properties
     * @throws BlogServiceException if retrieval fails
     */
    public List<CommentData> getComments(String blogPostId) throws BlogServiceException {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<List<CommentData>>) session -> retrieveComments(session, blogPostId));
        } catch (RepositoryException e) {
            logger.error("Failed to retrieve comments for blogPost={}", blogPostId, e);
            throw new BlogServiceException("Unable to retrieve comments", e);
        }
    }

    private List<CommentData> retrieveComments(JCRSessionWrapper session, String blogPostId) throws RepositoryException {
        List<CommentData> comments = new ArrayList<>();
        
        JCRNodeWrapper blogPost = session.getNodeByUUID(blogPostId);
        JCRSiteNode site = blogPost.getResolveSite();
        
        String basePath = String.format("/sites/%s/contents/ugc/blogs/%s/comments", site.getName(), blogPostId);
        
        if (!session.nodeExists(basePath)) {
            logger.debug("No comments folder exists for blogPost={}", blogPostId);
            return comments;
        }
        
        JCRNodeWrapper commentsFolder = session.getNode(basePath);
        NodeIterator iterator = commentsFolder.getNodes();
        
        while (iterator.hasNext()) {
            JCRNodeWrapper commentNode = (JCRNodeWrapper) iterator.nextNode();
            if (commentNode.isNodeType("jsblognt:comment")) {
                // Check status property first, fall back to approved boolean for backwards compatibility
                String status;
                if (commentNode.hasProperty("status")) {
                    status = commentNode.getProperty("status").getString();
                } else {
                    boolean approved = commentNode.hasProperty("approved") && commentNode.getProperty("approved").getBoolean();
                    status = approved ? "approved" : "pending";
                }
                
                // Only return approved comments for public display
                if (!"approved".equals(status)) {
                    continue;
                }
                
                String uuid = commentNode.getIdentifier();
                String authorName = commentNode.hasProperty("author") ? commentNode.getProperty("author").getString() : "Anonymous";
                String body = commentNode.getProperty("comment").getString();
                String created = commentNode.getProperty("ts").getDate().toInstant().toString();
                
                comments.add(new CommentData(uuid, authorName, body, created, status));
            }
        }
        
        logger.debug("Retrieved {} approved comments for blogPost={}", comments.size(), blogPostId);
        return comments;
    }

    /**
     * Update the status of a comment
     * @param commentId The UUID of the comment
     * @param status The new status (approved, rejected, pending)
     * @return true if successful
     * @throws BlogServiceException if update fails
     */
    public boolean updateCommentStatus(String commentId, String status) throws BlogServiceException {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<Boolean>) session -> {
                        try {
                            JCRNodeWrapper commentNode = session.getNodeByIdentifier(commentId);
                            commentNode.setProperty("status", status);
                            // Sync the approved boolean for backwards compatibility
                            commentNode.setProperty("approved", "approved".equals(status));
                            session.save();
                            logger.info("Updated comment {} status to {}", commentId, status);
                            return true;
                        } catch (PathNotFoundException e) {
                            logger.error("Comment not found: {}", commentId, e);
                            return false;
                        }
                    });
        } catch (RepositoryException e) {
            logger.error("Failed to update comment status for commentId={}", commentId, e);
            throw new BlogServiceException("Failed to update comment status", e);
        }
    }

    /**
     * Delete a comment
     * @param commentId The UUID of the comment
     * @return true if successful
     * @throws BlogServiceException if deletion fails
     */
    public boolean deleteComment(String commentId) throws BlogServiceException {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<Boolean>) session -> {
                        try {
                            JCRNodeWrapper commentNode = session.getNodeByIdentifier(commentId);
                            String path = commentNode.getPath();
                            commentNode.remove();
                            session.save();
                            logger.info("Deleted comment {} at path {}", commentId, path);
                            return true;
                        } catch (PathNotFoundException e) {
                            logger.error("Comment not found: {}", commentId, e);
                            return false;
                        }
                    });
        } catch (RepositoryException e) {
            logger.error("Failed to delete comment commentId={}", commentId, e);
            throw new BlogServiceException("Failed to delete comment", e);
        }
    }

    /**
     * Data transfer object for comment data
     */
    public static class CommentData {
        private final String uuid;
        private final String authorName;
        private final String body;
        private final String created;
        private final String status;

        public CommentData(String uuid, String authorName, String body, String created, String status) {
            this.uuid = uuid;
            this.authorName = authorName;
            this.body = body;
            this.created = created;
            this.status = status;
        }

        public String getUuid() { return uuid; }
        public String getAuthorName() { return authorName; }
        public String getBody() { return body; }
        public String getCreated() { return created; }
        public String getStatus() { return status; }
    }
}
