package com.voxloud.provisioning.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {
    private final String errorDescription;

    public TechnicalException(String errorDescription) {
        super(errorDescription);
        this.errorDescription = errorDescription;
    }
}