package app.display.dialogs;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import app.DesktopApp;
import manager.api.GameInfo;

/**
 * A dialog for setting up a new game, allowing the user to configure
 * players (Human or AI with different difficulty levels).
 */
public class GameSetupDialog extends JDialog {

    private final GameInfo gameInfo;
    private final DesktopApp app;
    private final List<JComboBox<String>> playerOptions = new ArrayList<>();

    public GameSetupDialog(JFrame owner, GameInfo gameInfo, DesktopApp app) {
        super(owner, "Setup Game: " + gameInfo.getName(), true);
        this.gameInfo = gameInfo;
        this.app = app;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));

        JPanel playersPanel = new JPanel(new GridLayout(gameInfo.getMaxPlayers(), 2, 10, 10));
        String[] options = {"Human", "AI (Easy)", "AI (Medium)", "AI (Hard)"};

        for (int i = 1; i <= gameInfo.getMaxPlayers(); i++) {
            playersPanel.add(new JLabel("Player " + i + ":"));
            JComboBox<String> comboBox = new JComboBox<>(options);
            playerOptions.add(comboBox);
            playersPanel.add(comboBox);
        }

        JPanel buttonsPanel = new JPanel();
        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(e -> {
            List<org.json.JSONObject> playerConfigs = new ArrayList<>();
            for (JComboBox<String> dropdown : playerOptions) {
                playerConfigs.add(getAIConfigForSelection((String) dropdown.getSelectedItem()));
            }
            app.showGameView(gameInfo, playerConfigs);
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonsPanel.add(cancelButton);
        buttonsPanel.add(startButton);

        add(playersPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private org.json.JSONObject getAIConfigForSelection(String selection) {
        org.json.JSONObject config = new org.json.JSONObject();
        org.json.JSONObject aiDetails = new org.json.JSONObject();

        switch (selection) {
            case "Human":
                aiDetails.put("algorithm", "Human");
                break;
            case "AI (Easy)":
                aiDetails.put("algorithm", "Random");
                break;
            case "AI (Medium)":
                aiDetails.put("algorithm", "UCT");
                aiDetails.put("Thinking Time", "1s");
                break;
            case "AI (Hard)":
                aiDetails.put("algorithm", "UCT");
                aiDetails.put("Thinking Time", "5s");
                break;
        }

        config.put("AI", aiDetails);
        return config;
    }
}