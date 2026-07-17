package com.misclickers.bingo;

import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Fades a small banner onto the top-center of the screen when a tile is
 * completed, styled after OSRS's own "New item added to your collection
 * log" pop-up — dark semi-transparent box, gold/bronze border, orange
 * header line, RuneScape UI font. Not a pixel-perfect recreation (the
 * game's actual sprite assets aren't available to a plugin), but matches
 * the silhouette and colour palette closely.
 *
 * queueCompletion() is safe to call from any thread (completions are
 * reported from background "bingo-report" threads); the queue is drained on
 * the client thread inside render(), one announcement at a time.
 */
public class TileCompletionOverlay extends Overlay {
    private static final int FADE_IN_MS = 250;
    private static final int HOLD_MS = 3000;
    private static final int FADE_OUT_MS = 500;
    private static final int TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private static final Color BACKGROUND = new Color(0, 0, 0, 215);
    private static final Color BORDER = new Color(178, 140, 62); // OSRS interface gold/bronze
    private static final Color HEADER = new Color(255, 152, 31); // OSRS orange
    private static final Color POINTS_COLOR = new Color(255, 255, 0);
    private static final Color TITLE_COLOR = Color.WHITE;

    private final Font headerFont = FontManager.getRunescapeBoldFont();
    private final Font bodyFont = FontManager.getRunescapeFont();

    private final Queue<Announcement> queue = new ConcurrentLinkedQueue<>();
    private Announcement current;
    private long shownAtMillis;

    @Inject
    private TileCompletionOverlay() {
        setPosition(OverlayPosition.TOP_CENTER);
    }

    public void queueCompletion(String tileTitle, int points) {
        queue.add(new Announcement(tileTitle, points));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (current == null) {
            current = queue.poll();
            if (current == null) {
                return null;
            }
            shownAtMillis = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - shownAtMillis;
        if (elapsed >= TOTAL_MS) {
            current = null;
            return null;
        }

        float alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = elapsed / (float) FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 1f;
        } else {
            alpha = 1f - (elapsed - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics headerMetrics = graphics.getFontMetrics(headerFont);
        FontMetrics bodyMetrics = graphics.getFontMetrics(bodyFont);

        String headerText = "Bingo tile completed!";
        String titleText = current.title;
        String pointsText = "+" + current.points + (current.points == 1 ? " pt" : " pts");

        int contentWidth = Math.max(headerMetrics.stringWidth(headerText),
                Math.max(bodyMetrics.stringWidth(titleText), bodyMetrics.stringWidth(pointsText)));
        int width = contentWidth + 32;
        int height = headerMetrics.getHeight() + bodyMetrics.getHeight() * 2 + 20;

        Composite originalComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        graphics.setColor(BACKGROUND);
        graphics.fill(new RoundRectangle2D.Float(0, 0, width, height, 10, 10));
        graphics.setColor(BORDER);
        graphics.setStroke(new BasicStroke(2f));
        graphics.draw(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, 10, 10));

        int y = 6 + headerMetrics.getAscent();
        graphics.setFont(headerFont);
        graphics.setColor(HEADER);
        drawCentered(graphics, headerText, width, y, headerMetrics);

        y += bodyMetrics.getHeight() + 2;
        graphics.setFont(bodyFont);
        graphics.setColor(TITLE_COLOR);
        drawCentered(graphics, titleText, width, y, bodyMetrics);

        y += bodyMetrics.getHeight();
        graphics.setColor(POINTS_COLOR);
        drawCentered(graphics, pointsText, width, y, bodyMetrics);

        graphics.setComposite(originalComposite);

        return new Dimension(width, height);
    }

    private static void drawCentered(Graphics2D graphics, String text, int width, int baselineY, FontMetrics metrics) {
        int x = (width - metrics.stringWidth(text)) / 2;
        graphics.drawString(text, x, baselineY);
    }

    private static final class Announcement {
        final String title;
        final int points;

        Announcement(String title, int points) {
            this.title = title;
            this.points = points;
        }
    }
}
