package grafika.ui;

import grafika.image.ImageLoader;
import grafika.image.JpegWriter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ImageViewerPanel extends JPanel {
    private final ImageViewPanel view = new ImageViewPanel();
    private final JLabel status = new JLabel(" Wczytaj plik PPM (P3/P6) lub JPEG.");
    private final JFileChooser chooser = new JFileChooser();
    private String fileInfo = "";
    private String baseName = "obraz";

    public ImageViewerPanel() {
        setLayout(new BorderLayout());
        chooser.setFileFilter(new FileNameExtensionFilter("Obrazy (PPM, JPG)", "ppm", "jpg", "jpeg"));

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(button("Wczytaj obraz…", this::open));
        bar.add(button("Zapisz jako JPEG…", this::saveJpeg));
        bar.addSeparator();
        bar.add(button("Dopasuj", view::fitToWindow));
        bar.add(button("100%", () -> view.setZoomCentered(1)));
        bar.add(button("+", () -> view.zoomCentered(1.5)));
        bar.add(button("−", () -> view.zoomCentered(1 / 1.5)));

        view.setInfoListener(text -> status.setText(
                " " + (fileInfo.isEmpty() ? "" : fileInfo + "   |   ") + text));

        add(bar, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void open() {
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        status.setText(" Wczytywanie: " + file.getName() + "…");

        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return ImageLoader.load(file);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    BufferedImage img = get();
                    fileInfo = file.getName() + " (" + img.getWidth() + " × " + img.getHeight() + ")";
                    int dot = file.getName().lastIndexOf('.');
                    baseName = dot > 0 ? file.getName().substring(0, dot) : file.getName();
                    view.setImage(img);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    status.setText(" Nie udało się wczytać pliku.");
                    showLoadError(file, ex.getCause());
                }
            }
        }.execute();
    }

    private void showLoadError(File file, Throwable cause) {
        String msg;
        if (cause instanceof OutOfMemoryError) {
            msg = "Za mało pamięci, aby wczytać ten obraz.";
        } else if (cause instanceof IOException) {
            msg = cause.getMessage();
        } else {
            msg = "Nieoczekiwany błąd: " + cause;
        }
        JOptionPane.showMessageDialog(this, file.getName() + ":\n" + msg,
                "Błąd wczytywania", JOptionPane.ERROR_MESSAGE);
    }

    private void saveJpeg() {
        BufferedImage img = view.getImage();
        if (img == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wczytaj obraz.",
                    "Brak obrazu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), baseName + ".jpg"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        String lower = file.getName().toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
            file = new File(file.getParentFile(), file.getName() + ".jpg");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(this,
                "Plik " + file.getName() + " już istnieje. Nadpisać?",
                "Zapis JPEG", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        Integer quality = askQuality();
        if (quality == null) {
            return;
        }
        try {
            JpegWriter.save(img, file, quality);
            status.setText(String.format(" Zapisano %s (jakość %d, %.1f KB)",
                    file.getName(), quality, file.length() / 1024.0));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu: " + ex.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer askQuality() {
        JSlider slider = new JSlider(0, 100, 75);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        JLabel label = new JLabel("Jakość JPEG: 75  (100 = najlepsza jakość, najmniejsza kompresja)");
        slider.addChangeListener(e -> label.setText("Jakość JPEG: " + slider.getValue()
                + "  (100 = najlepsza jakość, najmniejsza kompresja)"));
        int result = JOptionPane.showConfirmDialog(this, new Object[]{label, slider},
                "Stopień kompresji JPEG", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return result == JOptionPane.OK_OPTION ? slider.getValue() : null;
    }
}