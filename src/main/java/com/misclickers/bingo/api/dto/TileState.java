package com.misclickers.bingo.api.dto;

import java.util.Map;

/**
 * One tile as returned by GET /boards/:boardId/state — see backend
 * routes/boards.ts. eachProgress is only populated for "each" mode tiles.
 */
public class TileState {
    public String id;
    public String title;
    public int points;
    public int row;
    public int col;
    public boolean repeatable;
    public int target;
    // Detection criteria — what game events this tile is watching for.
    public String metric;
    public String mode;
    public java.util.List<String> sources;
    // Optional OSRS item ID to render (via ItemManager) instead of the tile's
    // title text — independent of metric/sources, see backend schema.prisma.
    // Gson leaves this null when the field is absent from the JSON response.
    public Integer iconItemId;
    public boolean completed;
    public int repeatCount;
    public int sumProgress;
    public Map<String, Integer> eachProgress;
}
