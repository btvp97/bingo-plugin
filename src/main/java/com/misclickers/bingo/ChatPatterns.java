package com.misclickers.bingo;

import java.util.regex.Pattern;

/**
 * Chat message regexes used to detect tile-completing events. Where noted,
 * these are copied directly from RuneLite's own official ChatCommandsPlugin
 * source (runelite-client/.../chatcommands/ChatCommandsPlugin.java) — high
 * confidence they're exactly right, since that's the same code the KC/CA
 * hiscore tracking in the live client uses. The rest are best-effort based
 * on well-known message text and should be double-checked in-game before
 * relying on them for anything that matters.
 */
public final class ChatPatterns {
    private ChatPatterns() {
    }

    // Verified against RuneLite source. Matches the "Your X kill count is: N"
    // family of messages: kill/harvest/lap/completion/success/Total Ticket
    // counts. Group 2 (added) is which of those words matched, so
    // ChatEventDetector can tell "kill" apart from the rest — combat kills
    // are now detected via BingoPlugin.onServerNpcLoot() instead (reliable
    // for every NPC, not just the handful with a native kill-count message),
    // so this pattern only drives the non-combat count types now. Kept
    // matching "kill" too (rather than dropping it from the alternation) so
    // this stays a complete, accurate description of the real message
    // format even though that branch is intentionally unused.
    public static final Pattern KILL_COUNT = Pattern.compile(
            "Your (?:completion count for |subdued |completed )?(?:<col=[0-9a-f]{6}>)?(.+?)(?:</col>)?"
                    + " (?:(kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?is:"
                    + " ?<col=[0-9a-f]{6}>([0-9,]+)</col>"
    );

    // Verified against user's own in-game testing: burying bones is actually
    // two separate chat lines — "You dig a hole in the ground." then, as its
    // own message, "You bury the bones." Only the second line carries the
    // bone type, so that's the one matched here; the "dig a hole" line is
    // generic (doesn't even mention bones) and ignored. Group 1 captures the
    // bone type (e.g. "bones", "big bones") so other bone tiers work too
    // without needing their own pattern.
    public static final Pattern BONES_BURIED = Pattern.compile(
            "You bury the (.+)\\.",
            Pattern.CASE_INSENSITIVE
    );

    // Verified against RuneLite source, but NOT currently wired up for
    // ITEM_OBTAINED detection — see BingoPlugin.onNpcLootReceived(). This
    // message only fires the first time you ever get a given item, so it
    // can't track tiles needing more than one (obtain 3 cudgels, obtain 5000
    // Pieces of Eight), and firing it alongside loot-based detection would
    // double-count the very first drop of everything. Kept here since it's
    // still the only detectable signal for non-NPC-kill sources (raid reward
    // chests, minigame rewards) — not yet wired up, see backend-data-model.md
    // follow-ups.
    public static final Pattern COLLECTION_LOG_ITEM = Pattern.compile(
            "New item added to your collection log: (.*)"
    );

    // Verified against RuneLite source. No boss/item name to capture — every
    // match is worth exactly one point of progress toward the rumour tile.
    public static final Pattern HUNTER_RUMOUR = Pattern.compile(
            "You have completed <col=[0-9a-f]{6}>([0-9,]+)</col> rumours? for the Hunter Guild\\."
    );

    // Well-known, stable message text (not from RuneLite source, but this
    // exact wording has been stable for years). Two variants depending on
    // whether you have a follower out already.
    public static final Pattern PET_RECEIVED = Pattern.compile(
            "You have a funny feeling like you're being followed"
                    + "|You feel something weird sneaking into your backpack"
    );

    // Verified against RuneLite source (embedded in their raid personal-best
    // patterns) — this substring reliably means a raid was just completed,
    // but doesn't tell us which of CoX/ToB/ToA. See BingoPlugin's note on
    // the "raid completion" source label.
    public static final Pattern RAID_COMPLETE = Pattern.compile(
            "Congratulations - your raid is complete!"
    );

    // Confirmed exact text directly from in-game testing. Deliberately easy
    // to trigger repeatedly (a few seconds at a gem rock) — good for
    // validating the whole detection -> report -> panel-update pipeline
    // without needing a boss kill or rare drop.
    public static final Pattern CUT_SAPPHIRE = Pattern.compile("You cut the sapphire\\.");

    // Verified against RuneLite's own LootTrackerPlugin source
    // (CLUE_SCROLL_PATTERN). Group 1 is the tier (e.g. "medium"), lowercase.
    public static final Pattern CLUE_COMPLETE = Pattern.compile(
            "You have completed [0-9]+ ([a-z]+) Treasure Trails?\\.",
            Pattern.CASE_INSENSITIVE
    );

    // Verified against RuneLite's own LootTrackerPlugin source
    // (LARRAN_LOOTED_PATTERN). Fires the instant the chest is opened, before
    // the loot lands in the inventory — used purely as a trigger to snapshot
    // inventory contents just ahead of the drop; see BingoPlugin's
    // onChatMessage/onItemContainerChanged for the diffing logic, since
    // chests (unlike NPC kills) don't fire a dedicated loot event.
    public static final Pattern LARRAN_CHEST_OPENED = Pattern.compile(
            "You have opened Larran's (big|small) chest .*"
    );

    // NOT VERIFIED — placeholder based on general knowledge, not source or
    // search-confirmed exact text. Don't rely on this until confirmed
    // in-game; likely candidate for the next round of fixes.
    public static final Pattern LMS_WIN = Pattern.compile(
            "Congratulations, you have won the game!"
    );
}
