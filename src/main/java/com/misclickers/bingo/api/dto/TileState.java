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
    // EACH mode only — per-source override of `target` (e.g. {"Goblin": 2,
    // "Chicken": 3}). Null, or missing a given source, means that source
    // uses `target` instead — see TileProgressFormatter.
    public Map<String, Integer> sourceTargets;
    // AND_OR mode only — list of OR-groups; the tile completes once every
    // group has at least one satisfied condition. See TileProgressFormatter.
    public java.util.List<TileGroup> groups;
    // OR_AND mode only — list of AND-sets; the tile completes once any one
    // set has every one of its own conditions satisfied. Reuses the same
    // TileGroup shape as `groups` (just a list of conditions) since the
    // structure is identical — only the completion rule differs, and that
    // lives in TileProgressFormatter, not the DTO. See Mode.OR_AND.
    public java.util.List<TileGroup> sets;
    // Optional OSRS item ID to render (via ItemManager) instead of the tile's
    // title text — independent of metric/sources, see backend schema.prisma.
    // Gson leaves this null when the field is absent from the JSON response.
    public Integer iconItemId;
    public boolean completed;
    public int repeatCount;
    public int sumProgress;
    public Map<String, Integer> eachProgress;
}
