package com.petmatch.community.exception;

public class SupportRequestNotFoundException extends RuntimeException {

    public SupportRequestNotFoundException(Long requestId) {
        super("Support request not found: " + requestId);
    }
}
