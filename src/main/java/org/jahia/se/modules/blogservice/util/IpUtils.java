package org.jahia.se.modules.blogservice.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class IpUtils {

    private static final List<String> CLIENT_IP_HEADERS = Arrays.asList(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
    );

    private IpUtils() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        for (String header : CLIENT_IP_HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public static String truncateForHash(String ip) {
        if (StringUtils.isBlank(ip)) {
            return null;
        }

        String normalised = ip.trim().toLowerCase(Locale.ROOT);
        if (normalised.contains(":")) {
            String[] parts = normalised.split(":");
            int length = Math.min(parts.length, 4);
            return String.join(":", Arrays.copyOf(parts, length));
        }

        String[] parts = normalised.split("\\.");
        if (parts.length >= 3) {
            return String.join(".", parts[0], parts[1], parts[2]);
        }

        return normalised;
    }
}
