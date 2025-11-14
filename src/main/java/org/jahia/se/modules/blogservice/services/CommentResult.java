package org.jahia.se.modules.blogservice.services;

public class CommentResult {

    public static final String CODE_OK = "OK";
    public static final String CODE_DUPLICATE = "DUPLICATE_COMMENT";
    public static final String CODE_MODERATION = "AWAITING_MODERATION";

    private final boolean success;
    private final String code;
    private final String commentId;

    public CommentResult(boolean success, String code) {
        this(success, code, null);
    }

    public CommentResult(boolean success, String code, String commentId) {
        this.success = success;
        this.code = code;
        this.commentId = commentId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getCommentId() {
        return commentId;
    }
}
