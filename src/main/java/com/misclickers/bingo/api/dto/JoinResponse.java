package com.misclickers.bingo.api.dto;

/**
 * Response shape from POST /teams/join — see backend routes/teams.ts.
 * Field names match the JSON keys exactly so Gson can map them with no
 * annotations needed.
 */
public class JoinResponse {
    public String token;
    public String teamId;
    public String teamName;
    public String boardId;
}
