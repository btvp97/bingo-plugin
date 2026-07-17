package com.misclickers.bingo;

import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Collections;

/**
 * Thin wrapper around the Socket.IO Java client for the backend's live-sync
 * channel — see backend src/socket.ts and routes/completions.ts. The server
 * authenticates the connection from the same JWT returned by POST
 * /teams/join, sent here via the Socket.IO v4 "auth" handshake payload
 * (verified against both the Java client's Socket.onopen(), which packs
 * `auth` into the CONNECT packet, and the server's
 * `socket.handshake.auth?.token` check) — and joins it to a "team:<teamId>"
 * room automatically. This class doesn't need to know about rooms at all,
 * just connect and listen for "tile_update".
 */
@Slf4j
public class BingoSocketClient {
    private Socket socket;

    /**
     * Connects and starts listening for "tile_update" events, invoking
     * onTileUpdate for every one received — including ones this same client
     * caused itself, which is harmless since the handler just triggers a
     * refetch. Runs on a Socket.IO-managed thread, not the RuneLite client
     * thread, so onTileUpdate is free to do blocking I/O (e.g. call
     * BingoPlugin.refreshBoard()) directly.
     */
    public void connect(String baseUrl, String token, Runnable onTileUpdate) {
        disconnect(); // replace any existing connection first

        try {
            IO.Options options = new IO.Options();
            options.auth = Collections.singletonMap("token", token);
            options.reconnection = true;

            socket = IO.socket(baseUrl, options);
            socket.on("tile_update", args -> onTileUpdate.run());
            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    log.warn("Bingo live-sync socket connect error: {}", args.length > 0 ? args[0] : "unknown"));
            socket.connect();
        } catch (URISyntaxException e) {
            log.warn("Bad base URL for live-sync socket: {}", baseUrl, e);
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.off();
            socket.disconnect();
            socket = null;
        }
    }
}
