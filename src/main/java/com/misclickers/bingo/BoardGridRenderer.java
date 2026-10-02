package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.BoardStateResponse;
import com.misclickers.bingo.api.dto.TileState;
import net.runelite.client.game.ItemManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Builds the tile-grid Swing tree for a BoardStateResponse. Extracted out of
 * BingoPanel so both the "my board" panel and the leaderboard's spectate
 * view can share one implementation — this class is stateless, it just
 * populates whatever gridPanel it's handed.
 */
final class BoardGridRenderer {
    private static final int GRID_HORIZONTAL_MARGIN = 14;
    private static final int TILE_CORNER_RADIUS = 8;

    // Same colors BingoPanel used inline before — kept here now that this is
    // the only place that paints tile cells.
    private static final Color TILE_INCOMPLETE_COLOR = new Color(24, 24, 24);
    private static final Color TILE_COMPLETE_COLOR = new Color(32, 63, 33);

    private BoardGridRenderer() {}

    /**
     * Clears gridPanel and rebuilds it from state. panelWidth is the
     * caller's own PANEL_WIDTH (PluginPanel.PANEL_WIDTH), passed in rather
     * than read from a constant here since this class isn't itself a panel.
     */
    static void render(JPanel gridPanel, BoardStateResponse state, ItemManager itemManager, int panelWidth) {
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(state.board.rows, state.board.cols, 2, 2));

        int usableWidth = panelWidth - GRID_HORIZONTAL_MARGIN;
        int cellSize = Math.max(24, usableWidth / Math.max(1, state.board.cols));

        // Tiles come back from the API already ordered by row then column.
        for (TileState tile : state.tiles) {
            gridPanel.add(buildCell(tile, itemManager, cellSize));
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private static JPanel buildCell(TileState tile, ItemManager itemManager, int cellSize) {
        RoundedPanel cell = new RoundedPanel(new BorderLayout(), TILE_CORNER_RADIUS);
        cell.setPreferredSize(new Dimension(cellSize, cellSize));
        cell.setBackground(tile.completed ? TILE_COMPLETE_COLOR : TILE_INCOMPLETE_COLOR);
        // A little breathing room on every side so the icon/title isn't
        // flush against the rounded corners and the progress row isn't
        // glued to the very bottom edge.
        cell.setBorder(new EmptyBorder(4, 3, 3, 3));

        String progressText = TileProgressFormatter.format(tile);
        // Tooltip stays title + progress regardless of whether the tile
        // shows an icon or text — the icon replaces the title label
        // visually, but doesn't change what identifies the tile on hover.
        cell.setToolTipText(tile.title + " (" + tile.points + " pts) — " + progressText);

        if (tile.iconItemId != null) {
            // itemManager.getImage() returns an AsyncBufferedImage that
            // starts blank and fills in once the client renders the real
            // sprite — addTo() wires up the repaint-on-load callback so the
            // label doesn't need any manual refresh logic here.
            // BorderLayout.CENTER stretches this label to fill all leftover
            // space above the SOUTH progress row, and the label's own
            // horizontal/vertical alignment centers the icon within that
            // space. Note OSRS item sprites aren't always visually centered
            // within their own canvas (some reserve padding for a
            // stack-count number in a corner), so a slightly off-center look
            // for certain items is the sprite itself, not this layout.
            JLabel iconLabel = new JLabel();
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setVerticalAlignment(SwingConstants.CENTER);
            itemManager.getImage(tile.iconItemId).addTo(iconLabel);
            cell.add(iconLabel, BorderLayout.CENTER);
        } else {
            // width:Npx forces Swing's HTML renderer to word-wrap the title
            // instead of overflowing past the cell edge; text-align:center
            // centers each wrapped line rather than leaving them ragged-left.
            int labelWidth = Math.max(20, cellSize - 8);
            JLabel titleLabel = new JLabel("<html><body style='width:" + labelWidth
                    + "px; text-align:center'>" + tile.title + "</body></html>");
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(titleLabel.getFont().deriveFont(10f));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            titleLabel.setVerticalAlignment(SwingConstants.CENTER);
            cell.add(titleLabel, BorderLayout.CENTER);
        }

        JLabel progressLabel = new JLabel(progressText);
        progressLabel.setForeground(new Color(200, 200, 200));
        // Bumped up from 9f now that icon tiles don't also need room for
        // wrapped title text above it — 12f reads much better and still has
        // shrinkFontToFit below as a safety net on narrow cells.
        progressLabel.setFont(progressLabel.getFont().deriveFont(12f));
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Shrinks the font until "10/10"-style text actually fits the cell
        // width instead of getting clipped at the right edge — small cells
        // (wide boards) otherwise had no fallback once the label's natural
        // text width exceeded the cell width.
        shrinkFontToFit(progressLabel, Math.max(14, cellSize - 6));
        cell.add(progressLabel, BorderLayout.SOUTH);

        return cell;
    }

    /** Reduces label's font size (down to a 7pt floor) until its current text fits maxWidth. */
    private static void shrinkFontToFit(JLabel label, int maxWidth) {
        Font font = label.getFont();
        FontMetrics metrics = label.getFontMetrics(font);
        while (font.getSize() > 7 && metrics.stringWidth(label.getText()) > maxWidth) {
            font = font.deriveFont((float) font.getSize() - 1f);
            metrics = label.getFontMetrics(font);
        }
        label.setFont(font);
    }

    /**
     * A JPanel that paints its background as a rounded rectangle instead of
     * a hard-edged square. Swing has no built-in support for rounded-corner
     * panels short of custom painting, so this fills the shape itself in
     * paintComponent and relies on setOpaque(false) to stop the normal
     * square background fill from painting underneath/around it.
     */
    static final class RoundedPanel extends JPanel {
        private final int cornerRadius;

        RoundedPanel(LayoutManager layout, int cornerRadius) {
            super(layout);
            this.cornerRadius = cornerRadius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
