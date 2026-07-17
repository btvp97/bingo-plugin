package com.misclickers.bingo;

import java.util.regex.Matcher;

/**
 * Runs a chat message against every known pattern and returns the first
 * match as a normalized (metric, source) event, or null if nothing matched.
 * Pure function, no RuneLite dependencies — easy to reason about in
 * isolation from the plugin wiring.
 */
public final class ChatEventDetector {
    private ChatEventDetector() {
    }

    public static final class DetectedEvent {
        public final String metric;
        public final String source;
        public final int amount;
        public final String rawEvidence;

        DetectedEvent(String metric, String source, int amount, String rawEvidence) {
            this.metric = metric;
            this.source = source;
            this.amount = amount;
            this.rawEvidence = rawEvidence;
        }
    }

    public static DetectedEvent detect(String message) {
        Matcher killCount = ChatPatterns.KILL_COUNT.matcher(message);
        if (killCount.find()) {
            String countType = killCount.group(2);
            // "kill"-type counts are now detected via
            // BingoPlugin.onServerNpcLoot() instead — that fires for every
            // NPC kill (goblins included), not just the handful of
            // bosses/minigames that happen to print this chat message.
            // Falling through here for "kill" avoids double-counting the
            // same death from both sources. harvest/lap/completion/success/
            // Total Ticket counts have no other signal, so they still go
            // through chat.
            if (!"kill".equalsIgnoreCase(countType)) {
                String subject = killCount.group(1).trim();
                return new DetectedEvent("KILL_COUNT", subject, 1, message);
            }
        }

        // Item drops are detected via NpcLootReceived instead (real
        // quantities, fires on every kill) — see BingoPlugin and the note on
        // ChatPatterns.COLLECTION_LOG_ITEM for why chat detection isn't used
        // for this metric.

        Matcher hunterRumour = ChatPatterns.HUNTER_RUMOUR.matcher(message);
        if (hunterRumour.find()) {
            return new DetectedEvent("ACTIVITY_COMPLETION", "Hunter rumour completion", 1, message);
        }

        Matcher pet = ChatPatterns.PET_RECEIVED.matcher(message);
        if (pet.find()) {
            return new DetectedEvent("ACTIVITY_COMPLETION", "Pet received", 1, message);
        }

        Matcher raid = ChatPatterns.RAID_COMPLETE.matcher(message);
        if (raid.find()) {
            // Can't tell which raid from this message alone — the board's
            // "Complete 50 Raids" tile is set up to accept this one generic
            // label rather than per-raid labels, see fixtures/*.json.
            return new DetectedEvent("ACTIVITY_COMPLETION", "raid completion", 1, message);
        }

        Matcher lms = ChatPatterns.LMS_WIN.matcher(message);
        if (lms.find()) {
            return new DetectedEvent("ACTIVITY_COMPLETION", "Last Man Standing win", 1, message);
        }

        Matcher clue = ChatPatterns.CLUE_COMPLETE.matcher(message);
        if (clue.find()) {
            String tier = clue.group(1).toLowerCase();
            String tierLabel = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
            return new DetectedEvent("ACTIVITY_COMPLETION", tierLabel + " clue casket", 1, message);
        }

        // Larran's chest (Dagon'hai piece source) isn't handled here — unlike
        // every other pattern in this class, matching the "chest opened"
        // message alone doesn't tell us what was obtained. That requires
        // snapshotting inventory state and diffing against the next
        // inventory-changed event, which needs RuneLite's Client/ItemManager
        // and so lives in BingoPlugin.onChatMessage/onItemContainerChanged
        // instead of this pure-function detector.

        Matcher sapphire = ChatPatterns.CUT_SAPPHIRE.matcher(message);
        if (sapphire.find()) {
            return new DetectedEvent("ACTIVITY_COMPLETION", "Cut sapphire", 1, message);
        }

        Matcher buryBones = ChatPatterns.BONES_BURIED.matcher(message);
        if (buryBones.find()) {
            String boneType = buryBones.group(1).trim().toLowerCase();
            return new DetectedEvent("ACTIVITY_COMPLETION", "Bury " + boneType, 1, message);
        }

        return null;
    }
}
