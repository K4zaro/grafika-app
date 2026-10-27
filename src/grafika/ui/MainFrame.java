package grafika.ui;

import grafika.model.Drawing;
import grafika.model.GShape;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;

public class MainFrame extends JFrame {

    private final Drawing drawing = new Drawing();
    private final DrawingPanel canvas = new DrawingPanel(drawing);
    private final JLabel status = new JLabel(" ");

    public MainFrame() {
        super("Grafika Komputerowa – edytor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(new ControlPanel(drawing, canvas), BorderLayout.WEST);
        add(canvas, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        drawing.addChangeListener(this::updateStatus);
        updateStatus();

        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    private void updateStatus() {
        GShape selected = drawing.getSelected();
        status.setText(" Obiektów: " + drawing.getShapes().size()
                + (selected != null ? "   |   zaznaczono: " + selected.getTool().getLabel() : ""));
    }
}
