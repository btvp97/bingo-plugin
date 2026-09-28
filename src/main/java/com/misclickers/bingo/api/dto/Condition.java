package com.misclickers.bingo.api.dto;

/**
 * One leaf of an AND_OR tile's group structure: "obtain/kill/complete
 * `source` at least `target` times satisfies this condition." See
 * TileGroup and the backend's completionLogic.ts for the full semantics.
 */
public class Condition {
    public String source;
    public int target;
}
