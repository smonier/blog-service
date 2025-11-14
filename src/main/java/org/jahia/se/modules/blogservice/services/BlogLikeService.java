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
 * Handles the JCR operations required to persist blog likes under the LIVE workspace as UGC.
 */
@Component(service = BlogLikeService.class, immediate = true)
public class BlogLikeService {

    private static final Logger logger = LoggerFactory.getLogger(BlogLikeService.class);

    public LikeResult submit(LikeRequest request) throws BlogServiceException {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Submitting like for blogPost={} clientHashPresent={} ipHashPresent={}",
                        request.getBlogPostId(),
                        StringUtils.isNotBlank(request.getClientHash()), StringUtils.isNotBlank(request.getIpHash()));
            }
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE,
                    null, (JCRCallback<LikeResult>) session -> execute(session, request));
        } catch (RepositoryException e) {
            logger.error("Like persistence failed for blogPost={}", request.getBlogPostId(), e);
            throw new BlogServiceException("Unable to execute like persistence", e);
        }
    }

    private LikeResult execute(JCRSessionWrapper session, LikeRequest request) throws RepositoryException {
        JCRNodeWrapper blogPost = session.getNodeByUUID(request.getBlogPostId());

        JCRSiteNode site = blogPost.getResolveSite();
        if (site == null) {
            throw new RepositoryException("Unable to resolve site for blog post " + request.getBlogPostId());
        }

        String basePath = "/sites/" + site.getName() + "/contents/ugc/blogs/" + request.getBlogPostId() + "/likes";
        if (logger.isDebugEnabled()) {
            logger.debug("Using likes base path {}", basePath);
        }
        JCRNodeWrapper likesFolder = getOrCreateLikesFolder(session, basePath);
        if (logger.isDebugEnabled()) {
            logger.debug("Likes folder primary type={} mixins={}", likesFolder.getPrimaryNodeTypeName(),
                    Arrays.toString(likesFolder.getMixinNodeTypes()));
        }

        if (isDuplicateLike(likesFolder, request)) {
            logger.info("Rejected duplicate like for blogPost={}", request.getBlogPostId());
            return new LikeResult(false, LikeResult.CODE_ALREADY_LIKED);
        }

        String likeNodeName = "l-" + UUID.randomUUID();
        JCRNodeWrapper likeNode = likesFolder.addNode(likeNodeName, "jsblognt:like");

        likeNode.setProperty("blogPostId", request.getBlogPostId());

        if (StringUtils.isNotBlank(request.getClientHash())) {
            likeNode.setProperty("clientHash", request.getClientHash());
        }

        if (StringUtils.isNotBlank(request.getIpHash())) {
            likeNode.setProperty("ipHash", request.getIpHash());
        }

        if (StringUtils.isNotBlank(request.getUserAgent())) {
            likeNode.setProperty("ua", request.getUserAgent());
        }

        Calendar timestamp = request.getTimestamp();
        if (timestamp != null) {
            likeNode.setProperty("ts", timestamp);
        }

        session.save();
        
        if (logger.isInfoEnabled()) {
            logger.info("Like persisted for blogPost={} node={}", request.getBlogPostId(), likeNode.getPath());
        }
        
        return new LikeResult(true, LikeResult.CODE_OK);
    }

    private boolean isDuplicateLike(JCRNodeWrapper likesFolder, LikeRequest request) throws RepositoryException {
        if (!likesFolder.hasNodes()) {
            return false;
        }

        NodeIterator nodes = likesFolder.getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper like = (JCRNodeWrapper) nodes.nextNode();
            if (StringUtils.isNotBlank(request.getClientHash()) && like.hasProperty("clientHash")
                    && request.getClientHash().equals(like.getProperty("clientHash").getString())) {
                return true;
            }
            if (StringUtils.isNotBlank(request.getIpHash()) && like.hasProperty("ipHash")
                    && request.getIpHash().equals(like.getProperty("ipHash").getString())) {
                return true;
            }
        }
        return false;
    }

    private JCRNodeWrapper getOrCreateLikesFolder(JCRSessionWrapper session, String basePath) throws RepositoryException {
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
                String nodeType = (i == segments.length - 1) ? "jsblognt:likesFolder" : "jnt:contentFolder";
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating node {} of type {} under {}", segment, nodeType, current.getPath());
                }
                current = current.addNode(segment, nodeType);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Created likes folder structure at {}", current.getPath());
        }
        return current;
    }
}
