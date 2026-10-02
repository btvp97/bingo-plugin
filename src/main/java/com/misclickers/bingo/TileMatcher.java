package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.Condition;
import com.misclickers.bingo.api.dto.TileGroup;
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
            // SUM/EACH tiles keep their match criteria in the flat
            // `sources` list. AND_OR and OR_AND tiles instead nest it inside
            // `groups`/`sets` — each a list of {source, target} conditions
            // — and leave `sources` empty/unused (see TileState and the
            // backend schema), so those have to be checked separately or
            // every event for a groups/sets tile gets silently dropped here
            // before it's ever reported.
            if (containsIgnoreCase(tile.sources, source)
                    || anyConditionMatches(tile.groups, source)
                    || anyConditionMatches(tile.sets, source)) {
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

    /** True if any condition in any group/set names this source — used for both AND_OR groups and OR_AND sets. */
    private static boolean anyConditionMatches(List<TileGroup> groupsOrSets, String source) {
        if (groupsOrSets == null) {
            return false;
        }
        for (TileGroup group : groupsOrSets) {
            if (group.conditions == null) {
                continue;
            }
            for (Condition condition : group.conditions) {
                if (condition.source.equalsIgnoreCase(source)) {
                    return true;
                }
            }
        }
        return false;
    }
}
