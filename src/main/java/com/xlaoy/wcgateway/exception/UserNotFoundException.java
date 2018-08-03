package com.xlaoy.wcgateway.exception;

import org.springframework.security.authentication.AccountStatusException;

public class UserNotFoundException extends AccountStatusException {

    public UserNotFoundException(String msg) {
        super(msg);
    }

    public UserNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }
}

