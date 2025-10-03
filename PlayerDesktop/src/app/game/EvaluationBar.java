package app.game;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import manager.api.GameObserver;
import manager.api.GameState;

/**
 * A component that displays a graphical evaluation bar, providing a visual
 * representation of the game's state from the perspective of the current player.
 */
public class EvaluationBar extends JPanel implements GameObserver {

    private double evaluation = 0.5; // Normalized between 0.0 (loss) and 1.0 (win)
    private final JLabel evalLabel;

    public EvaluationBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(100, 25));
        evalLabel = new JLabel("+0.00", JLabel.CENTER);
        evalLabel.setForeground(Color.BLACK);
        add(evalLabel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int width = getWidth();
        int height = getHeight();

        // Draw background
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);

        // Draw evaluation bar
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, (int) (width * evaluation), height);
    }

    @Override
    public void onGameStateUpdate(GameState newState) {
        // The evaluation from the GameState is assumed to be between -1 and 1.
        // We need to normalize it to be between 0 and 1 for the bar.
        this.evaluation = (newState.getEvaluation() + 1.0) / 2.0;

        // Update the label with a more traditional chess-style evaluation
        String evalString = String.format("%+.2f", newState.getEvaluation());
        evalLabel.setText(evalString);

        repaint();
    }

    @Override
    public void onError(String errorMessage) {
        // We can ignore errors for the evaluation bar
    }
}