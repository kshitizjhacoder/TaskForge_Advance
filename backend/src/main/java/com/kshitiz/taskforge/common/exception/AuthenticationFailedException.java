package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when credentials or a token are wrong, for example a bad password at login or an
 * invalid refresh token. Mapped to HTTP 401.
 *
 * <p>Messages must not reveal which part was wrong (e.g. "unknown email" vs "wrong
 * password"), so attackers cannot discover which emails are registered.
 */
public class AuthenticationFailedException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
