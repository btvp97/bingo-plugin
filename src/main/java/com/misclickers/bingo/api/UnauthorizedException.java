package com.misclickers.bingo.api;

import java.io.IOException;

/**
 * Thrown by BingoApiClient instead of a plain IOException when the server
 * responds 401 — i.e. the current token is missing, invalid, or expired.
 * Kept as its own type so callers can specifically react by re-joining
 * (getting a fresh token) rather than just surfacing a generic error, which
 * previously required the player to manually restart the plugin once a
 * long-lived session's token expired. See BingoPlugin.refreshBoard().
 */
public class UnauthorizedException extends IOException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
