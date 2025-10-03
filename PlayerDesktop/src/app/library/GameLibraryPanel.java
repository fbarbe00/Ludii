package app.library;

import app.DesktopApp;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import manager.api.GameInfo;
import manager.api.LudiiGameService;

/**
 * The main panel for the Game Library screen. It displays a searchable and
 * filterable grid of available games.
 */
public class GameLibraryPanel extends JPanel {

    private final LudiiGameService gameService;
    private final JPanel gameGrid;

    private final DesktopApp app;

    public GameLibraryPanel(LudiiGameService gameService, DesktopApp app) {
        this.gameService = gameService;
        this.app = app;
        this.gameGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        initUI();
        loadGames();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel with search and filters
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        topPanel.add(new JTextField(), BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Game grid
        JScrollPane scrollPane = new JScrollPane(gameGrid);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadGames() {
        List<GameInfo> games = gameService.listAvailableGames();
        for (GameInfo game : games) {
            GameCard card = new GameCard(game, app);
            gameGrid.add(card);
        }
        revalidate();
        repaint();
    }
}