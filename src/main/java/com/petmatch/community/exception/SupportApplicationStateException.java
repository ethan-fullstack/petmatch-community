package com.petmatch.community.exception;

public class SupportApplicationStateException extends RuntimeException {

    public SupportApplicationStateException(Long applicationId) {
        super("Support application state does not allow this operation: " + applicationId);
    }
}
