package com.xlaoy.wcgateway.exception;

import org.springframework.security.authentication.AccountStatusException;

public class UserChangeException extends AccountStatusException {

    public UserChangeException(String msg) {
        super(msg);
    }

    public UserChangeException(String msg, Throwable t) {
        super(msg, t);
    }
}

