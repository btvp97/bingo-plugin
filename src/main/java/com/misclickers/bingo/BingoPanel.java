package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.BoardStateResponse;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

public class BingoPanel extends PluginPanel {
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

    private final ItemManager itemManager;
    private Runnable onRefresh;

    public BingoPanel(ItemManager itemManager) {
        super(false);
        this.itemManager = itemManager;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Header styled as its own card (DARKER_GRAY_COLOR fill, padding,
        // thin divider underneath) rather than sitting flush against the
        // sidebar background — same pattern the Wise Old Man plugin uses to
        // separate its header/search area from the rest of the panel.
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        teamLabel.setForeground(Color.WHITE);
        teamLabel.setFont(FontManager.getRunescapeBoldFont());
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        scoreLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        scoreLabel.setFont(FontManager.getRunescapeSmallFont());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshButton.addActionListener(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });

        teamLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(teamLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(statusLabel);
        header.add(scoreLabel);
        header.add(Box.createVerticalStrut(6));
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

        // Grid cell construction (icons, progress labels, rounded tiles,
        // square-cell sizing) lives in BoardGridRenderer now — shared with
        // LeaderboardPanel's read-only spectate view.
        BoardGridRenderer.render(gridPanel, state, itemManager, PANEL_WIDTH);

        gridWrapper.revalidate();
        gridWrapper.repaint();
        revalidate();
        repaint();
    }
}
