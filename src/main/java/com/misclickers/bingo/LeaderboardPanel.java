package com.misclickers.bingo;

import com.misclickers.bingo.api.dto.BoardStateResponse;
import com.misclickers.bingo.api.dto.TeamsListResponse;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;

/**
 * Read-only "spectate another team's board" view. Sits alongside BingoPanel
 * as a second tab (see BingoPlugin) — same grid rendering via
 * BoardGridRenderer, but no refresh-triggers-report plumbing, since this is
 * never the local player's own progress.
 */
public class LeaderboardPanel extends JPanel {
    private final JComboBox<TeamOption> teamSelector = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Pick a team");
    private final JLabel scoreLabel = new JLabel(" ");
    private final JPanel gridPanel = new JPanel();
    // Same FlowLayout-wrapper trick BingoPanel uses — keeps the grid square
    // instead of letting it stretch to fill the tab's height.
    private final JPanel gridWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    private final ItemManager itemManager;
    private Consumer<String> onTeamSelected;
    // Guards against the selection listener firing (and re-fetching) while
    // setTeams() is repopulating the combo box itself.
    private boolean populatingTeams;

    public LeaderboardPanel(ItemManager itemManager) {
        this.itemManager = itemManager;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(10, 10, 10, 10)));

        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        scoreLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        scoreLabel.setFont(FontManager.getRunescapeSmallFont());

        teamSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        teamSelector.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, teamSelector.getPreferredSize().height));
        teamSelector.addActionListener(e -> {
            if (populatingTeams) {
                return;
            }
            TeamOption selected = (TeamOption) teamSelector.getSelectedItem();
            if (selected != null && onTeamSelected != null) {
                onTeamSelected.accept(selected.teamId);
            }
        });

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(teamSelector);
        header.add(Box.createVerticalStrut(6));
        header.add(statusLabel);
        header.add(scoreLabel);
        add(header, BorderLayout.NORTH);

        gridPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        gridWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
        gridWrapper.add(gridPanel);
        add(gridWrapper, BorderLayout.CENTER);
    }

    /** Called once the teams list has been fetched. Fires onTeamSelected for whichever entry ends up selected. */
    public void setTeams(List<TeamsListResponse.TeamSummary> teams) {
        populatingTeams = true;
        Object previouslySelected = teamSelector.getSelectedItem();
        String previousTeamId = previouslySelected instanceof TeamOption ? ((TeamOption) previouslySelected).teamId : null;

        teamSelector.removeAllItems();
        for (TeamsListResponse.TeamSummary team : teams) {
            teamSelector.addItem(new TeamOption(team.id, team.name));
        }

        // Keep the same team selected across a refresh if it's still on the
        // board; otherwise fall back to whatever the combo box defaults to
        // (its first entry). Stays under populatingTeams so none of this
        // triggers the action listener — the explicit call below is the
        // single source of truth for firing onTeamSelected, since
        // setSelectedIndex() is a silent no-op whenever the index it's
        // given is already the current selection (notably true for the
        // very first population, where addItem already auto-selected
        // index 0).
        boolean restored = false;
        if (previousTeamId != null) {
            for (int i = 0; i < teamSelector.getItemCount(); i++) {
                if (teamSelector.getItemAt(i).teamId.equals(previousTeamId)) {
                    teamSelector.setSelectedIndex(i);
                    restored = true;
                    break;
                }
            }
        }
        if (!restored && teamSelector.getItemCount() > 0) {
            teamSelector.setSelectedIndex(0);
        }
        populatingTeams = false;

        TeamOption selected = (TeamOption) teamSelector.getSelectedItem();
        if (selected != null && onTeamSelected != null) {
            onTeamSelected.accept(selected.teamId);
        }
    }

    public void setOnTeamSelected(Consumer<String> onTeamSelected) {
        this.onTeamSelected = onTeamSelected;
    }

    public void showError(String message) {
        statusLabel.setText("Error: " + message);
    }

    public void render(BoardStateResponse state) {
        statusLabel.setText(state.board.name);
        scoreLabel.setText("Score: " + state.score.total
                + " (" + state.score.completedCount + "/" + state.tiles.size() + " tiles)");

        BoardGridRenderer.render(gridPanel, state, itemManager, net.runelite.client.ui.PluginPanel.PANEL_WIDTH);

        gridWrapper.revalidate();
        gridWrapper.repaint();
        revalidate();
        repaint();
    }

    private static final class TeamOption {
        final String teamId;
        final String teamName;

        TeamOption(String teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName;
        }

        @Override
        public String toString() {
            return teamName;
        }
    }
}
