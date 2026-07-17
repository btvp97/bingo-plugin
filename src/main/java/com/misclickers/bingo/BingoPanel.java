package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.BoardStateResponse;
import com.misclickers.bingo.api.dto.TileState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class BingoPanel extends PluginPanel {
    // Small margin so the grid doesn't touch the panel's edges.
    private static final int GRID_HORIZONTAL_MARGIN = 14;

    private final JLabel teamLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Not connected");
    private final JLabel scoreLabel = new JLabel(" ");
    private final JPanel gridPanel = new JPanel();
    // gridPanel sits inside this FlowLayout wrapper rather than being added
    // to the outer BorderLayout directly. FlowLayout is what stops the grid
    // from being stretched to fill the sidebar's full height — it always
    // respects its child's own preferred size instead of expanding it,
    // which is what let the tiles get stretched into tall rectangles before.
    private final JPanel gridWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    private Runnable onRefresh;

    public BingoPanel() {
        super(false);
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel(new GridLayout(4, 1, 0, 4));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);

        teamLabel.setForeground(Color.WHITE);
        statusLabel.setForeground(Color.WHITE);
        scoreLabel.setForeground(Color.WHITE);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });

        header.add(teamLabel);
        header.add(statusLabel);
        header.add(scoreLabel);
        header.add(refreshButton);
        add(header, BorderLayout.NORTH);

        gridPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        gridWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        gridWrapper.add(gridPanel);
        add(gridWrapper, BorderLayout.CENTER);
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    /** Set once after a successful join — see BingoPlugin.attemptJoin(). */
    public void setTeamName(String teamName) {
        teamLabel.setText("Team: " + teamName);
    }

    public void showConnecting() {
        statusLabel.setText("Connecting...");
    }

    public void showError(String message) {
        statusLabel.setText("Error: " + message);
    }

    public void render(BoardStateResponse state) {
        statusLabel.setText(state.board.name);
        scoreLabel.setText("Score: " + state.score.total
                + " (" + state.score.completedCount + "/" + state.tiles.size() + " tiles)");

        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(state.board.rows, state.board.cols, 2, 2));

        // Force square cells: GridLayout sizes every cell to the same
        // width/height, taken from its largest child's preferred size — so
        // giving each cell an explicit square preferredSize here, combined
        // with gridWrapper's FlowLayout (which never stretches gridPanel
        // beyond its own preferred size), is what keeps the whole board
        // square instead of stretching to fill the sidebar's height.
        int usableWidth = PANEL_WIDTH - GRID_HORIZONTAL_MARGIN;
        int cellSize = Math.max(24, usableWidth / Math.max(1, state.board.cols));

        // Tiles come back from the API already ordered by row then column.
        for (TileState tile : state.tiles) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setPreferredSize(new Dimension(cellSize, cellSize));
            cell.setBackground(tile.completed ? new Color(46, 125, 50) : ColorScheme.DARKER_GRAY_COLOR);

            String progressText = TileProgressFormatter.format(tile);
            cell.setToolTipText(tile.title + " (" + tile.points + " pts) — " + progressText);

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

            JLabel progressLabel = new JLabel(progressText);
            progressLabel.setForeground(new Color(200, 200, 200));
            progressLabel.setFont(progressLabel.getFont().deriveFont(9f));
            progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cell.add(progressLabel, BorderLayout.SOUTH);

            gridPanel.add(cell);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
        gridWrapper.revalidate();
        gridWrapper.repaint();
        revalidate();
        repaint();
    }
}
