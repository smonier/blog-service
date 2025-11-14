package org.jahia.se.modules.blogservice.services;

public class BlogServiceException extends Exception {

    public BlogServiceException(String message) {
        super(message);
    }

    public BlogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
