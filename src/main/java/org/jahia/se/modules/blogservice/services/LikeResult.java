package org.jahia.se.modules.blogservice.services;

public class LikeResult {

    public static final String CODE_OK = "OK";
    public static final String CODE_ALREADY_LIKED = "ALREADY_LIKED";

    private final boolean success;
    private final String code;

    public LikeResult(boolean success, String code) {
        this.success = success;
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }
}
