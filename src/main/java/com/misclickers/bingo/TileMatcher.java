package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.TileState;

import java.util.ArrayList;
import java.util.List;

/**
 * Given the currently cached board tiles and a detected (metric, source)
 * event, finds which tiles it's relevant to. This mirrors the matching rule
 * the backend applies in src/lib/completionLogic.ts — it's a client-side
 * pre-filter so we don't spam the API with obviously irrelevant events, not
 * the authority on whether a completion actually counts (the server always
 * re-checks and is free to reject).
 */
public final class TileMatcher {
    private TileMatcher() {
    }

    public static List<TileState> findMatches(List<TileState> tiles, String metric, String source) {
        List<TileState> matches = new ArrayList<>();
        if (tiles == null) {
            return matches;
        }
        for (TileState tile : tiles) {
            if (!metric.equals(tile.metric)) {
                continue;
            }
            // Once a non-repeatable tile is done, nothing more to report.
            // Repeatable tiles (e.g. the pet tile) keep accepting events for
            // bonus credit even after their first completion.
            if (tile.completed && !tile.repeatable) {
                continue;
            }
            if (containsIgnoreCase(tile.sources, source)) {
                matches.add(tile);
            }
        }
        return matches;
    }

    private static boolean containsIgnoreCase(List<String> sources, String source) {
        if (sources == null) {
            return false;
        }
        for (String s : sources) {
            if (s.equalsIgnoreCase(source)) {
                return true;
            }
        }
        return false;
    }
}
