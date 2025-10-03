package app.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.swing.JLabel;
import javax.swing.JPanel;
import manager.api.GameObserver;
import manager.api.GameState;
import app.PlayerApp;

/**
 * A View component that displays a graphical evaluation bar.
 */
public class EvaluationBarView extends View implements GameObserver {

    private double evaluation = 0.5; // Normalized between 0.0 (loss) and 1.0 (win)
    private final JLabel evalLabel;
    private final JPanel barPanel;

    public EvaluationBarView(PlayerApp app) {
        super(app);
        this.barPanel = new JPanel(new BorderLayout());
        this.evalLabel = new JLabel("+0.00", JLabel.CENTER);
        this.evalLabel.setForeground(Color.BLACK);
        this.barPanel.add(evalLabel, BorderLayout.CENTER);
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(placement.x, placement.y, placement.width, placement.height);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(placement.x, placement.y, (int) (placement.width * evaluation), placement.height);

        // We need to draw the label manually as we are not a real JPanel
        g2d.setColor(evalLabel.getForeground());
        g2d.setFont(evalLabel.getFont());
        String text = evalLabel.getText();
        int stringWidth = g2d.getFontMetrics().stringWidth(text);
        int stringHeight = g2d.getFontMetrics().getAscent();
        g2d.drawString(text, placement.x + (placement.width - stringWidth) / 2, placement.y + (placement.height + stringHeight) / 2);
    }

    @Override
    public void onGameStateUpdate(GameState newState) {
        this.evaluation = (newState.getEvaluation() + 1.0) / 2.0;
        String evalString = String.format("%+.2f", newState.getEvaluation());
        evalLabel.setText(evalString);
        app.repaint(placement);
    }

    @Override
    public void onError(String errorMessage) {
        // Ignore errors
    }
}