package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.Condition;
import com.misclickers.bingo.api.dto.TileGroup;
import com.misclickers.bingo.api.dto.TileState;

/**
 * Shared "x/y" progress text formatting, used by both the side panel and the
 * on-screen progress overlay so they can never disagree with each other.
 */
public final class TileProgressFormatter {
    private TileProgressFormatter() {
    }

    /**
     * For "each" mode tiles (full-set tiles, e.g. "obtain all 4 axe pieces")
     * this is how many of the required sources have hit their own target so
     * far. For "and_or" mode tiles it's how many of the tile's OR-groups
     * already have a satisfied condition (out of the total group count). For
     * "sum" mode tiles it's raw progress against the target (e.g. kill count
     * so far / kills needed).
     */
    public static String format(TileState tile) {
        if ("EACH".equals(tile.mode) && tile.sources != null && !tile.sources.isEmpty()) {
            int satisfied = 0;
            for (String source : tile.sources) {
                Integer progress = tile.eachProgress != null ? tile.eachProgress.get(source) : null;
                if (progress != null && progress >= targetFor(tile, source)) {
                    satisfied++;
                }
            }
            return satisfied + "/" + tile.sources.size();
        }
        if ("AND_OR".equals(tile.mode) && tile.groups != null && !tile.groups.isEmpty()) {
            int satisfiedGroups = 0;
            for (TileGroup group : tile.groups) {
                if (groupSatisfied(tile, group)) {
                    satisfiedGroups++;
                }
            }
            return satisfiedGroups + "/" + tile.groups.size();
        }
        return Math.min(tile.sumProgress, tile.target) + "/" + tile.target;
    }

    /** An AND_OR group is satisfied once any one of its own conditions is met. */
    private static boolean groupSatisfied(TileState tile, TileGroup group) {
        if (group.conditions == null) {
            return false;
        }
        for (Condition condition : group.conditions) {
            Integer progress = tile.eachProgress != null ? tile.eachProgress.get(condition.source) : null;
            if (progress != null && progress >= condition.target) {
                return true;
            }
        }
        return false;
    }

    /**
     * The target a given EACH-mode source needs to hit: its own override
     * from tile.sourceTargets if present, otherwise the tile's shared
     * target — mirrors the same fallback the backend applies in
     * completionLogic.ts, so display never disagrees with what actually
     * completes the tile.
     */
    private static int targetFor(TileState tile, String source) {
        if (tile.sourceTargets != null && tile.sourceTargets.containsKey(source)) {
            return tile.sourceTargets.get(source);
        }
        return tile.target;
    }
}
