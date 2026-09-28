package com.misclickers.bingo.api.dto;

import java.util.List;

/**
 * One OR-group of an AND_OR tile: satisfied when any one of its conditions
 * has been met. A tile completes once every one of its groups is satisfied
 * — see TileState.groups and the backend's completionLogic.ts.
 */
public class TileGroup {
    public List<Condition> conditions;
}
