package com.misclickers.bingo;

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
     * far. For "sum" mode tiles it's raw progress against the target (e.g.
     * kill count so far / kills needed).
     */
    public static String format(TileState tile) {
        if ("EACH".equals(tile.mode) && tile.sources != null && !tile.sources.isEmpty()) {
            int satisfied = 0;
            for (String source : tile.sources) {
                Integer progress = tile.eachProgress != null ? tile.eachProgress.get(source) : null;
                if (progress != null && progress >= tile.target) {
                    satisfied++;
                }
            }
            return satisfied + "/" + tile.sources.size();
        }
        return Math.min(tile.sumProgress, tile.target) + "/" + tile.target;
    }
}
