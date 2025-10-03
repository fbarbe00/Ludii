package app.library;

import app.DesktopApp;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Color;

import manager.api.GameInfo;

/**
 * A UI component that displays a single game in the Game Library. It shows
 * the game's icon and name, and is designed to be displayed in a grid.
 */
public class GameCard extends JPanel {

    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 180;
    private static final int ICON_SIZE = 120;

    private final GameInfo gameInfo;
    private final DesktopApp app;

    public GameCard(GameInfo gameInfo, DesktopApp app) {
        this.gameInfo = gameInfo;
        this.app = app;
        initUI();
    }

    private void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setMaximumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setBackground(Color.WHITE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Icon
        java.net.URL imgURL = getClass().getResource(gameInfo.getIconPath());
        if (imgURL == null) {
            imgURL = getClass().getResource("/ludii-logo-100x100.png");
        }
        ImageIcon icon = new ImageIcon(imgURL);
        Image image = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(image));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(iconLabel);

        // Name
        JLabel nameLabel = new JLabel(gameInfo.getName());
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setFont(nameLabel.getFont().deriveFont(16f));
        add(nameLabel);

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                app.showGameDetails(gameInfo);
            }

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLUE, 2),
                        BorderFactory.createEmptyBorder(9, 9, 9, 9)
                ));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY, 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }
        });
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }
}