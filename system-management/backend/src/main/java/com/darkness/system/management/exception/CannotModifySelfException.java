package com.darkness.system.management.exception;

public class CannotModifySelfException extends RuntimeException {
    public CannotModifySelfException() {
        super("Cannot modify your own account via this endpoint");
    }
}
