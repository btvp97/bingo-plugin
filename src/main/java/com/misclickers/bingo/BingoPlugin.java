package com.misclickers.bingo;

import com.google.inject.Provides;
import com.misclickers.bingo.ChatEventDetector.DetectedEvent;
import com.misclickers.bingo.api.BingoApiClient;
import com.misclickers.bingo.api.dto.BoardStateResponse;
import com.misclickers.bingo.api.dto.CompletionResponse;
import com.misclickers.bingo.api.dto.JoinResponse;
import com.misclickers.bingo.api.dto.TileState;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Slf4j
@PluginDescriptor(
        name = "Clan Bingo",
        description = "Coordinate clan bingo boards with live team sync",
        tags = {"bingo", "pvm", "minigame", "clan"}
)
public class BingoPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private BingoConfig config;

    @Inject
    private BingoApiClient apiClient;

    @Inject
    private ItemManager itemManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TileCompletionOverlay completionOverlay;

    @Inject
    private BingoProgressOverlay progressOverlay;

    @Inject
    private AudioPlayer audioPlayer;

    // Not Guice-injected — it's a plain wrapper with no RuneLite
    // dependencies of its own, just a live socket connection scoped to
    // whichever team we're currently joined to. See attemptJoin()/shutDown().
    private final BingoSocketClient socketClient = new BingoSocketClient();

    // Classpath-resource path to the tile-completion sound, bundled at
    // src/main/resources/tile_complete.wav. This is a synthesized placeholder
    // chime, not the real "Task Mastered (Leagues)" jingle — that's Jagex's
    // copyrighted game audio, so it isn't something to source and bundle
    // here. To use the real jingle instead, personally export the clip from
    // your own client and replace that file at the same path; nothing else
    // needs to change.
    private static final String COMPLETION_SOUND_RESOURCE = "/tile_complete.wav";

    private BingoPanel panel;
    private NavigationButton navButton;
    private String boardId;
    private boolean joined;

    // Last board state fetched from the server — this is what chat-detected
    // events get matched against locally before reporting. Kept up to date
    // by refreshBoard(); until task 5's live sync exists, it can go briefly
    // stale between refreshes, which just means a just-completed tile might
    // get one redundant (harmless) completion report before the next fetch.
    private volatile BoardStateResponse currentState;

    // Set when a "You have opened Larran's chest" chat message is seen;
    // holds an item-id -> quantity snapshot of the inventory taken at that
    // instant, so the next inventory-changed event can be diffed against it
    // to work out what the chest actually gave. Cleared after that one diff,
    // win or lose — chests don't fire NpcLootReceived, so this is the only
    // way to detect their contents. Only ever touched from the client
    // thread (chat + inventory events are both delivered there), so no
    // extra synchronization is needed.
    private Map<Integer, Integer> pendingChestSnapshot;

    @Provides
    BingoConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BingoConfig.class);
    }

    @Override
    protected void startUp() {
        panel = new BingoPanel();
        panel.setOnRefresh(() -> new Thread(this::connectOrRefresh, "bingo-refresh").start());

        navButton = NavigationButton.builder()
                .tooltip("Clan Bingo")
                .icon(createBingoIcon())
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        overlayManager.add(completionOverlay);
        overlayManager.add(progressOverlay);

        joined = false;
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
        overlayManager.remove(completionOverlay);
        overlayManager.remove(progressOverlay);
        socketClient.disconnect();
        joined = false;
        boardId = null;
    }

    /** Read by BingoProgressOverlay to render live per-tile progress. */
    public BoardStateResponse getCurrentState() {
        return currentState;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN || joined) {
            return;
        }
        new Thread(this::connectOrRefresh, "bingo-join").start();
    }

    /**
     * Shared entry point for both the login trigger and the panel's Refresh
     * button: joins if we haven't yet (e.g. the join code was set after
     * login, so the login trigger fired too early to see it), otherwise just
     * re-fetches current board state. Runs blocking network calls — always
     * called from a background thread, never the client thread.
     */
    private void connectOrRefresh() {
        if (!joined) {
            attemptJoin();
        } else {
            refreshBoard();
        }
    }

    private void attemptJoin() {
        if (config.joinCode() == null || config.joinCode().isEmpty()) {
            SwingUtilities.invokeLater(() -> panel.showError("Set a join code in the plugin's settings first"));
            return;
        }
        String rsn = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
        if (rsn == null) {
            SwingUtilities.invokeLater(() -> panel.showError("Not logged in yet"));
            return;
        }

        SwingUtilities.invokeLater(panel::showConnecting);
        apiClient.setBaseUrl(config.baseUrl()); // re-read in case it changed since startUp()

        try {
            JoinResponse joinResponse = apiClient.join(config.joinCode(), rsn);
            this.boardId = joinResponse.boardId;
            this.joined = true;
            SwingUtilities.invokeLater(() -> panel.setTeamName(joinResponse.teamName));
            // Live sync: teammates' completions push a "tile_update" over
            // this socket the instant they're reported, so refreshBoard()
            // runs for everyone on the team, not just whoever detected the
            // event. Backend side of this (broadcasting to the team's room)
            // already existed — this was the missing half.
            socketClient.connect(config.baseUrl(), joinResponse.token, this::refreshBoard);
            refreshBoard();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> panel.showError(e.getMessage()));
            this.joined = false;
        }
    }

    private void refreshBoard() {
        if (boardId == null) {
            return;
        }
        try {
            BoardStateResponse state = apiClient.fetchBoardState(boardId);
            this.currentState = state;
            SwingUtilities.invokeLater(() -> panel.render(state));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> panel.showError(e.getMessage()));
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) {
            return;
        }

        // Larran's chest doesn't tell us what it gave in the message itself
        // — snapshot inventory now so the next onItemContainerChanged can
        // diff against it. Handled separately from ChatEventDetector.detect()
        // below since it needs Client/inventory access that pure function
        // deliberately doesn't have.
        Matcher larranChest = ChatPatterns.LARRAN_CHEST_OPENED.matcher(event.getMessage());
        if (larranChest.find()) {
            pendingChestSnapshot = snapshotInventory();
        }

        DetectedEvent detected = ChatEventDetector.detect(event.getMessage());
        if (detected != null) {
            dispatch(detected);
        }
    }

    private Map<Integer, Integer> snapshotInventory() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null) {
            return new HashMap<>();
        }
        return aggregateItems(inventory.getItems());
    }

    private static Map<Integer, Integer> aggregateItems(Item[] items) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Item item : items) {
            if (item.getId() <= 0) {
                continue;
            }
            counts.merge(item.getId(), item.getQuantity(), Integer::sum);
        }
        return counts;
    }

    // Fires whenever any item container changes — filtered down to the
    // player's inventory, and only acted on if a Larran's chest open was
    // just seen in chat (pendingChestSnapshot != null). Diffs current
    // inventory against that snapshot to find what the chest gave, since
    // chests have no dedicated loot event the way NPC kills do.
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        Map<Integer, Integer> snapshot = pendingChestSnapshot;
        if (snapshot == null || event.getContainerId() != InventoryID.INV) {
            return;
        }
        pendingChestSnapshot = null; // one-shot: only diff against the very next change

        Map<Integer, Integer> current = aggregateItems(event.getItemContainer().getItems());
        for (Map.Entry<Integer, Integer> entry : current.entrySet()) {
            int gained = entry.getValue() - snapshot.getOrDefault(entry.getKey(), 0);
            if (gained <= 0) {
                continue;
            }
            ItemComposition composition = itemManager.getItemComposition(entry.getKey());
            String rawEvidence = "Larran's chest gave " + gained + "x " + composition.getName();
            dispatch(new DetectedEvent("ITEM_OBTAINED", composition.getName(), gained, rawEvidence));
        }
    }

    // Item drops are tracked here rather than via chat, since this fires on
    // every kill with the real quantity obtained — the collection log chat
    // message only ever fires once, the very first time you get a given
    // item, which can't track tiles that need more than one (e.g. "obtain 3
    // Sarachnis cudgels"). See ChatPatterns.COLLECTION_LOG_ITEM for the full
    // reasoning. Larran's chest (a non-NPC-kill source) is handled separately
    // via inventory diffing — see onChatMessage/onItemContainerChanged.
    // Other chest/minigame reward sources aren't covered yet — known gap.
    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        for (ItemStack itemStack : event.getItems()) {
            ItemComposition composition = itemManager.getItemComposition(itemStack.getId());
            String rawEvidence = event.getNpc().getName() + " dropped " + itemStack.getQuantity() + "x " + composition.getName();
            dispatch(new DetectedEvent("ITEM_OBTAINED", composition.getName(), itemStack.getQuantity(), rawEvidence));
        }
    }

    // General-purpose "you killed an NPC" signal, sourced from the game's
    // own native loot-tracking system — the same data that powers RuneLite's
    // built-in Loot Tracker panel and the in-game "View loot" summary. Unlike
    // NpcLootReceived (ground-item based; only fires when there's something
    // to pick up off the ground) or the "Your X kill count is: N" chat
    // message (only sent for a handful of tracked bosses/minigames), this
    // fires for essentially every NPC kill — regular monsters like Goblins
    // included — since it's driven by the client script that feeds the
    // native loot tracker, not chat text. See the KILL_COUNT branch in
    // ChatEventDetector for why the chat message is deliberately skipped for
    // "kill"-type counts now: firing both here and there for the same kill
    // would double-count it.
    @Subscribe
    public void onServerNpcLoot(ServerNpcLoot event) {
        String npcName = Text.removeTags(event.getComposition().getName());
        if (npcName == null || npcName.isEmpty() || "null".equals(npcName)) {
            return;
        }
        dispatch(new DetectedEvent("KILL_COUNT", npcName, 1, npcName + " kill (loot-tracker signal)"));
    }

    /** Shared by both detection sources: find matching tiles, report each on a background thread. */
    private void dispatch(DetectedEvent detected) {
        if (!joined || currentState == null) {
            return;
        }
        List<TileState> matches = TileMatcher.findMatches(currentState.tiles, detected.metric, detected.source);
        for (TileState tile : matches) {
            new Thread(() -> reportCompletion(tile, detected), "bingo-report").start();
        }
    }

    private void reportCompletion(TileState tile, DetectedEvent detected) {
        try {
            CompletionResponse response = apiClient.reportCompletion(
                    tile.id, detected.metric, detected.source, detected.amount, detected.rawEvidence);
            // justCompleted is only true on the exact report that pushed this
            // tile from incomplete -> completed — that's what triggers the
            // pop-up, not every accepted report (which would fire on every
            // single kill/drop toward a multi-step tile).
            if (response != null && response.accepted && response.justCompleted) {
                completionOverlay.queueCompletion(tile.title, tile.points);
                playCompletionSound();
            }
            // Re-fetch so the panel and progress overlay reflect the new
            // state. This is a stopgap until task 5 adds a WebSocket push —
            // for now, only the player whose client detected the event sees
            // it update immediately; teammates need to click Refresh to see
            // it.
            refreshBoard();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> panel.showError(e.getMessage()));
        }
    }

    private void playCompletionSound() {
        try {
            audioPlayer.play(BingoPlugin.class, COMPLETION_SOUND_RESOURCE, 0f);
        } catch (Exception e) {
            // Non-critical — a missing/corrupt sound file shouldn't break
            // completion reporting or the pop-up.
            log.warn("Failed to play tile completion sound", e);
        }
    }

    // Toolbar icon: a small light-blue 3x3 bingo grid, drawn in code rather
    // than a bundled image resource so there's no separate asset to keep in
    // sync — swap for a real icon.png before publishing if a hand-drawn one
    // is ever wanted instead.
    private static BufferedImage createBingoIcon() {
        int cells = 3;
        int gap = 1;
        int cellSize = 5;
        int size = gap + cells * (cellSize + gap);

        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(100, 200, 255));
        for (int row = 0; row < cells; row++) {
            for (int col = 0; col < cells; col++) {
                int x = gap + col * (cellSize + gap);
                int y = gap + row * (cellSize + gap);
                g.fillRoundRect(x, y, cellSize, cellSize, 1, 1);
            }
        }
        g.dispose();
        return icon;
    }
}
