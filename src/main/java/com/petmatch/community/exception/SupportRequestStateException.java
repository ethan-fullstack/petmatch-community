package com.petmatch.community.exception;

public class SupportRequestStateException extends RuntimeException {

    public SupportRequestStateException(Long requestId) {
        super("Support request cannot be modified in its current state: " + requestId);
    }
}
