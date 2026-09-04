package com.petmatch.community.exception;

public class SupportApplicationNotFoundException extends RuntimeException {

    public SupportApplicationNotFoundException(Long applicationId) {
        super("Support application not found: " + applicationId);
    }
}
