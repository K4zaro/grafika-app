package grafika.ui;

import grafika.model.Drawing;
import grafika.model.GShape;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

public class MainFrame extends JFrame {

    private final Drawing drawing = new Drawing();
    private final DrawingPanel canvas = new DrawingPanel(drawing);
    private final JLabel status = new JLabel(" ");

    public MainFrame() {
        super("Grafika Komputerowa");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel vectorTab = new JPanel(new BorderLayout());
        vectorTab.add(new ControlPanel(drawing, canvas), BorderLayout.WEST);
        vectorTab.add(canvas, BorderLayout.CENTER);
        vectorTab.add(status, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Edytor wektorowy", vectorTab);
        tabs.addTab("Przeglądarka obrazów", new ImageViewerPanel());
        add(tabs, BorderLayout.CENTER);

        drawing.addChangeListener(this::updateStatus);
        updateStatus();

        setSize(1100, 750);
        setLocationRelativeTo(null);
    }

    private void updateStatus() {
        GShape selected = drawing.getSelected();
        status.setText(" Obiektów: " + drawing.getShapes().size()
                + (selected != null ? "   |   zaznaczono: " + selected.getTool().getLabel() : ""));
    }
}