package com.misclickers.bingo.api.dto;

import java.util.List;

/** Response shape from GET /boards/:boardId/state. */
public class BoardStateResponse {
    public BoardInfo board;
    public List<TileState> tiles;
    public ScoreBreakdown score;
}
