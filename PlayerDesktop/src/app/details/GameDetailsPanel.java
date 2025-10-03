package app.details;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;

import app.DesktopApp;
import manager.api.GameInfo;

/**
 * A panel that displays detailed information about a single game, including
 * its description, rules, and a button to start playing.
 */
public class GameDetailsPanel extends JPanel {

    private final GameInfo gameInfo;
    private final DesktopApp app;

    public GameDetailsPanel(GameInfo gameInfo, DesktopApp app) {
        this.gameInfo = gameInfo;
        this.app = app;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top section with banner and title
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));

        // Banner Image
        ImageIcon icon = new ImageIcon(gameInfo.getIconPath());
        Image image = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
        JLabel bannerLabel = new JLabel(new ImageIcon(image));
        topPanel.add(bannerLabel, BorderLayout.WEST);

        // Title and player count
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(gameInfo.getName());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titlePanel.add(titleLabel);

        JLabel playersLabel = new JLabel(gameInfo.getMinPlayers() + "-" + gameInfo.getMaxPlayers() + " players");
        playersLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        titlePanel.add(playersLabel);

        topPanel.add(titlePanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Center section with description/rules
        JTextArea descriptionArea = new JTextArea("This is where a longer description of the game and its rules would go.\n\n" + gameInfo.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Arial", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom section with Play button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton backButton = new JButton("Back to Library");
        backButton.setFont(new Font("Arial", Font.PLAIN, 16));
        backButton.addActionListener(e -> app.showGameLibrary());

        JButton playButton = new JButton("Play vs. AI / Local");
        playButton.setFont(new Font("Arial", Font.BOLD, 16));
        playButton.addActionListener(e -> {
            new app.display.dialogs.GameSetupDialog(app.frame(), gameInfo, app).setVisible(true);
        });

        JButton onlineButton = new JButton("Create Online Game");
        onlineButton.setFont(new Font("Arial", Font.BOLD, 16));
        onlineButton.addActionListener(e -> {
            // Placeholder for online game creation
            javax.swing.JOptionPane.showMessageDialog(app.frame(), "Online play is not yet implemented.");
        });

        bottomPanel.add(backButton);
        bottomPanel.add(playButton);
        bottomPanel.add(onlineButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}