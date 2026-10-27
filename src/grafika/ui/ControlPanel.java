package grafika.ui;

import grafika.io.DrawingStorage;
import grafika.model.Drawing;
import grafika.model.GShape;
import grafika.model.ShapeFactory;
import grafika.model.Tool;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class ControlPanel extends JPanel {
    private static final int MAX_PARAMS = 4;

    private final Drawing drawing;
    private final DrawingPanel canvas;
    private final JLabel[] labels = new JLabel[MAX_PARAMS];
    private final JTextField[] fields = new JTextField[MAX_PARAMS];

    public ControlPanel(Drawing drawing, DrawingPanel canvas) {
        this.drawing = drawing;
        this.canvas = canvas;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(createToolPanel());
        add(Box.createVerticalStrut(8));
        add(createParamsPanel());
        add(Box.createVerticalStrut(6));
        addButton("Rysuj z parametrów", this::drawFromFields);
        addButton("Zastosuj do zaznaczonego", this::applyToSelected);
        addButton("Usuń zaznaczony (Del)", drawing::deleteSelected);
        addButton("Wyczyść wszystko", drawing::clear);
        addButton("Zapisz do pliku…", this::save);
        addButton("Wczytaj z pliku…", this::load);
        add(Box.createVerticalGlue());
        add(createHelp());

        drawing.addChangeListener(this::refreshFields);
        refreshFields();
    }

    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 3));
        panel.setBorder(BorderFactory.createTitledBorder("Narzędzie"));
        ButtonGroup group = new ButtonGroup();
        for (Tool tool : Tool.values()) {
            JToggleButton button = new JToggleButton(tool.getLabel(), tool == Tool.SELECT);
            button.addActionListener(e -> {
                for (JTextField f : fields) {
                    f.setText("");
                }
                canvas.setTool(tool);
            });
            group.add(button);
            panel.add(button);
        }
        return panel;
    }

    private JPanel createParamsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Parametry (piksele)"));
        for (int i = 0; i < MAX_PARAMS; i++) {
            labels[i] = new JLabel();
            fields[i] = new JTextField(6);
            panel.add(labels[i]);
            panel.add(fields[i]);
        }
        return panel;
    }

    private JTextArea createHelp() {
        JTextArea help = new JTextArea(
                "Rysowanie myszą: klik = 1. punkt, klik = 2. punkt\n"
                        + "(linia: końce, prostokąt: przeciwległe rogi,\n"
                        + "okrąg: środek i punkt na okręgu). Esc anuluje.\n\n"
                        + "Zaznacz: klik w krawędź figury; przeciągnij,\n"
                        + "by przesunąć, lub złap biały uchwyt, by zmienić\n"
                        + "kształt. Parametry edytuj w polach i zatwierdź.");
        help.setEditable(false);
        help.setOpaque(false);
        help.setFont(help.getFont().deriveFont(11f));
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        return help;
    }

    private void addButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        add(button);
        add(Box.createVerticalStrut(4));
    }

    private void refreshFields() {
        GShape selected = drawing.getSelected();
        Tool tool = selected != null ? selected.getTool() : canvas.getTool();
        String[] names = tool.getParamLabels();
        double[] values = selected != null ? selected.getParams() : null;
        for (int i = 0; i < MAX_PARAMS; i++) {
            boolean visible = i < names.length;
            labels[i].setVisible(visible);
            fields[i].setVisible(visible);
            if (visible) {
                labels[i].setText(names[i]);
                if (values != null) {
                    fields[i].setText(format(values[i]));
                }
            }
        }
    }

    private static String format(double v) {
        return String.valueOf(Math.round(v * 100) / 100.0);
    }

    private Optional<double[]> readParams(Tool tool) {
        double[] p = new double[tool.getParamCount()];
        try {
            for (int i = 0; i < p.length; i++) {
                p[i] = Double.parseDouble(fields[i].getText().trim().replace(',', '.'));
            }
        } catch (NumberFormatException ex) {
            warn("Wpisz poprawne liczby we wszystkich polach.");
            return Optional.empty();
        }
        return Optional.of(p);
    }

    private void drawFromFields() {
        Tool tool = canvas.getTool();
        if (!tool.isDrawingTool()) {
            warn("Wybierz narzędzie: linia, prostokąt lub okrąg.");
            return;
        }
        readParams(tool).ifPresent(p -> {
            try {
                drawing.add(ShapeFactory.fromParams(tool, p));
            } catch (IllegalArgumentException ex) {
                warn(ex.getMessage());
            }
        });
    }

    private void applyToSelected() {
        GShape selected = drawing.getSelected();
        if (selected == null) {
            warn("Najpierw zaznacz obiekt (narzędzie „Zaznacz”).");
            return;
        }
        readParams(selected.getTool()).ifPresent(p -> {
            try {
                selected.setParams(p);
                drawing.fireChanged();
            } catch (IllegalArgumentException ex) {
                warn(ex.getMessage());
            }
        });
    }

    private void save() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            DrawingStorage.save(chooser.getSelectedFile(), new ArrayList<>(drawing.getShapes()));
        } catch (IOException ex) {
            error("Błąd zapisu: " + ex.getMessage());
        }
    }

    private void load() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            drawing.replaceAll(DrawingStorage.load(chooser.getSelectedFile()));
        } catch (IOException ex) {
            error("Błąd odczytu: " + ex.getMessage());
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Błędne parametry", JOptionPane.WARNING_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }
}
