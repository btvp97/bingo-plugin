package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.BoardStateResponse;
import com.misclickers.bingo.api.dto.TileState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Side-of-screen list of every tile the team has unfinished, nonzero
 * progress on, e.g. "Cut sapphire: 6/10". Complements the sidebar panel
 * (which shows the whole board) by surfacing live progress without needing
 * that panel open. A standard RuneLite OverlayPanel — draggable and
 * resizable by the player, position/size persisted like any other overlay.
 */
public class BingoProgressOverlay extends OverlayPanel {
    private static final int MAX_TITLE_CHARS = 22;

    private final BingoPlugin plugin;

    @Inject
    private BingoProgressOverlay(BingoPlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(200, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        BoardStateResponse state = plugin.getCurrentState();
        if (state == null) {
            return null;
        }

        boolean any = false;
        for (TileState tile : state.tiles) {
            if (tile.completed) {
                continue;
            }
            String progress = TileProgressFormatter.format(tile);
            if (progress.startsWith("0/")) {
                continue; // no progress yet on this tile — nothing worth showing
            }
            if (!any) {
                panelComponent.getChildren().add(TitleComponent.builder()
                        .text("Bingo Progress")
                        .color(Color.ORANGE)
                        .build());
                any = true;
            }
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(truncate(tile.title))
                    .right(progress)
                    .build());
        }

        if (!any) {
            return null;
        }
        return super.render(graphics);
    }

    private static String truncate(String title) {
        return title.length() > MAX_TITLE_CHARS ? title.substring(0, MAX_TITLE_CHARS - 1) + "…" : title;
    }
}
