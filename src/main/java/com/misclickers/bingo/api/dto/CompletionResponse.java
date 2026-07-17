package com.misclickers.bingo.api.dto;

/**
 * Response body from POST /completions — see backend routes/completions.ts.
 * Only the fields the plugin actually reads are declared here; Gson just
 * ignores the rest of the JSON (score, repeatCredited, reason, etc).
 */
public class CompletionResponse {
    public boolean accepted;
    // True only on the exact report that pushed the tile from
    // incomplete -> completed. Used to trigger the completion pop-up without
    // needing to diff board state ourselves.
    public boolean justCompleted;
}
