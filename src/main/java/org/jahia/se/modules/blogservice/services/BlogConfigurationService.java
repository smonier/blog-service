package org.jahia.se.modules.blogservice.services;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Configuration service exposed to the OSGi runtime that carries the secret used to hash
 * user-provided identifiers when persisting blog comments and likes. The configuration is mandatory
 * in order to keep the stored data pseudonymous.
 */
@Component(service = BlogConfigurationService.class, immediate = true)
@Designate(ocd = BlogConfigurationService.Configuration.class)
public class BlogConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(BlogConfigurationService.class);

    private volatile String serverSecret;
    private volatile String clientIdCookieName;
    private volatile boolean enableIpHash;
    private volatile boolean requireModeration;

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        this.serverSecret = StringUtils.trimToNull(configuration.serverSecret());
        this.clientIdCookieName = StringUtils.defaultIfBlank(configuration.clientIdCookieName(), "jahia-client-id");
        this.enableIpHash = configuration.enableIpHash();
        this.requireModeration = configuration.requireModeration();

        if (logger.isInfoEnabled()) {
            logger.info("Blog configuration updated: serverSecretConfigured={} clientIdCookieName={} enableIpHash={} requireModeration={}",
                    this.serverSecret != null, this.clientIdCookieName, this.enableIpHash, this.requireModeration);
        }

        if (this.serverSecret == null) {
            logger.warn("Blog configuration is missing a server secret. Default value will be used.");
        }
    }

    public Optional<String> getServerSecret() {
        return Optional.ofNullable(serverSecret);
    }

    public String getClientIdCookieName() {
        return clientIdCookieName;
    }

    public boolean isEnableIpHash() {
        return enableIpHash;
    }

    public boolean isRequireModeration() {
        return requireModeration;
    }

    @ObjectClassDefinition(
            pid = "org.jahia.se.modules.blogservice",
            name = "Blog service configuration"
    )
    public @interface Configuration {

        @AttributeDefinition(
                name = "Server secret",
                description = "Secret used to hash the client identifier and IP when persisting blog interactions. Keep it private and rotate periodically.")
        String serverSecret() default "blog-service-default-secret-change-me-in-production-2025";

        @AttributeDefinition(
                name = "Client identifier cookie",
                description = "Name of the cookie containing the client identifier. Defaults to jahia-client-id.")
        String clientIdCookieName() default "jahia-client-id";

        @AttributeDefinition(
                name = "Enable IP hash",
                description = "If enabled, blog interactions will also record an anonymised hash of the caller IP address.")
        boolean enableIpHash() default true;

        @AttributeDefinition(
                name = "Require comment moderation",
                description = "If enabled, all comments will require approval before being visible.")
        boolean requireModeration() default true;
    }
}
