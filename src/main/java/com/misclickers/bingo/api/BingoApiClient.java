package com.misclickers.bingo.api;

import com.google.gson.Gson;
import com.misclickers.bingo.api.dto.BoardStateResponse;
import com.misclickers.bingo.api.dto.CompletionResponse;
import com.misclickers.bingo.api.dto.JoinResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Thin wrapper around the bingo backend's REST API. Every method here does a
 * blocking network call — always invoke from a background thread, never from
 * the RuneLite client thread (that would freeze the game client).
 */
public class BingoApiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;

    private String baseUrl;
    private String token;

    @Inject
    public BingoApiClient(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String getToken() {
        return token;
    }

    /** POST /teams/join — exchanges a join code + RSN for a session token. */
    public JoinResponse join(String joinCode, String rsn) throws IOException {
        String body = gson.toJson(new JoinRequestBody(joinCode, rsn));
        Request request = new Request.Builder()
                .url(baseUrl + "/teams/join")
                .post(RequestBody.create(JSON, body))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Join failed: HTTP " + response.code());
            }
            JoinResponse joinResponse = gson.fromJson(response.body().string(), JoinResponse.class);
            this.token = joinResponse.token;
            return joinResponse;
        }
    }

    /** GET /boards/:boardId/state — requires join() to have run first. */
    public BoardStateResponse fetchBoardState(String boardId) throws IOException {
        if (token == null) {
            throw new IllegalStateException("Not joined yet — call join() first");
        }
        Request request = new Request.Builder()
                .url(baseUrl + "/boards/" + boardId + "/state")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Fetching board state failed: HTTP " + response.code());
            }
            return gson.fromJson(response.body().string(), BoardStateResponse.class);
        }
    }

    /**
     * POST /completions — reports a detected game event toward a specific
     * tile. The server re-validates it against that tile's criteria
     * independently; this call being accepted doesn't guarantee the tile
     * actually completed (see the response body if you need to know).
     */
    public CompletionResponse reportCompletion(String tileId, String metric, String source, int amount, String rawEvidence) throws IOException {
        if (token == null) {
            throw new IllegalStateException("Not joined yet — call join() first");
        }
        String body = gson.toJson(new CompletionRequestBody(tileId, metric, source, amount, rawEvidence));
        Request request = new Request.Builder()
                .url(baseUrl + "/completions")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(JSON, body))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Reporting completion failed: HTTP " + response.code());
            }
            return gson.fromJson(response.body().string(), CompletionResponse.class);
        }
    }

    private static final class JoinRequestBody {
        final String joinCode;
        final String rsn;

        JoinRequestBody(String joinCode, String rsn) {
            this.joinCode = joinCode;
            this.rsn = rsn;
        }
    }

    private static final class CompletionRequestBody {
        final String tileId;
        final String metric;
        final String source;
        final int amount;
        final String rawEvidence;

        CompletionRequestBody(String tileId, String metric, String source, int amount, String rawEvidence) {
            this.tileId = tileId;
            this.metric = metric;
            this.source = source;
            this.amount = amount;
            this.rawEvidence = rawEvidence;
        }
    }
}
