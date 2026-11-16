package grafika.ui;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class ImageViewPanel extends JPanel {
    private static final double MIN_ZOOM = 0.02;
    private static final double MAX_ZOOM = 200;
    /** Od tego powiększenia (px ekranu na 1 px obrazu) wyświetlane są wartości R, G, B. */
    private static final double VALUES_ZOOM = 48;

    private BufferedImage image;
    private double zoom = 1;
    private double offX;
    private double offY;
    private boolean fitPending;
    private Point lastDrag;
    private Point lastMouse;
    private Consumer<String> infoListener = s -> {
    };

    public ImageViewPanel() {
        setBackground(new Color(60, 60, 60));
        setPreferredSize(new Dimension(640, 480));

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDrag = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (image == null || lastDrag == null) {
                    return;
                }
                offX += e.getX() - lastDrag.x;
                offY += e.getY() - lastDrag.y;
                lastDrag = e.getPoint();
                lastMouse = e.getPoint();
                repaint();
                report();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastDrag = null;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouse = e.getPoint();
                report();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lastMouse = null;
                report();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAt(e.getPoint(), Math.pow(1.2, -e.getPreciseWheelRotation()));
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addMouseWheelListener(adapter);
    }

    public void setInfoListener(Consumer<String> listener) {
        this.infoListener = listener;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        this.fitPending = true;
        repaint();
    }

    public void fitToWindow() {
        fit();
        repaint();
        report();
    }

    public void setZoomCentered(double newZoom) {
        zoomAt(center(), newZoom / zoom);
    }

    public void zoomCentered(double factor) {
        zoomAt(center(), factor);
    }

    private Point center() {
        return new Point(getWidth() / 2, getHeight() / 2);
    }

    private void zoomAt(Point p, double factor) {
        if (image == null) {
            return;
        }
        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        double ix = (p.x - offX) / zoom;
        double iy = (p.y - offY) / zoom;
        zoom = newZoom;
        offX = p.x - ix * zoom;
        offY = p.y - iy * zoom;
        repaint();
        report();
    }

    private void fit() {
        if (image == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double fit = Math.min(getWidth() / (double) image.getWidth(), getHeight() / (double) image.getHeight());
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, fit));
        offX = (getWidth() - image.getWidth() * zoom) / 2;
        offY = (getHeight() - image.getHeight() * zoom) / 2;
    }

    private void report() {
        String text = String.format("Powiększenie: %.0f%%", zoom * 100);
        if (image != null && lastMouse != null) {
            int x = (int) Math.floor((lastMouse.x - offX) / zoom);
            int y = (int) Math.floor((lastMouse.y - offY) / zoom);
            if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                int rgb = image.getRGB(x, y);
                text += String.format("   |   piksel (%d, %d):  R=%d  G=%d  B=%d",
                        x, y, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            }
        }
        infoListener.accept(text);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (image == null) {
            g.setColor(Color.LIGHT_GRAY);
            String hint = "Brak obrazu – wczytaj plik PPM (P3/P6) lub JPEG";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(hint, (getWidth() - fm.stringWidth(hint)) / 2, getHeight() / 2);
            g.dispose();
            return;
        }

        if (fitPending && getWidth() > 0 && getHeight() > 0) {
            fitPending = false;
            fit();
            SwingUtilities.invokeLater(this::report);
        }

        int w = image.getWidth();
        int h = image.getHeight();
        int sx0 = Math.max(0, (int) Math.floor(-offX / zoom));
        int sy0 = Math.max(0, (int) Math.floor(-offY / zoom));
        int sx1 = Math.min(w, (int) Math.ceil((getWidth() - offX) / zoom));
        int sy1 = Math.min(h, (int) Math.ceil((getHeight() - offY) / zoom));
        if (sx0 >= sx1 || sy0 >= sy1) {
            g.dispose();
            return;
        }

        int dx0 = (int) Math.round(offX + sx0 * zoom);
        int dy0 = (int) Math.round(offY + sy0 * zoom);
        int dx1 = (int) Math.round(offX + sx1 * zoom);
        int dy1 = (int) Math.round(offY + sy1 * zoom);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, zoom >= 1
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);

        if (zoom >= VALUES_ZOOM) {
            drawPixelValues(g, sx0, sy0, sx1, sy1, dx0, dy0, dx1, dy1);
        }
        g.dispose();
    }

    private void drawPixelValues(Graphics2D g, int sx0, int sy0, int sx1, int sy1,
                                 int dx0, int dy0, int dx1, int dy1) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(128, 128, 128, 120));
        for (int x = sx0; x <= sx1; x++) {
            int px = (int) Math.round(offX + x * zoom);
            g.drawLine(px, dy0, px, dy1);
        }
        for (int y = sy0; y <= sy1; y++) {
            int py = (int) Math.round(offY + y * zoom);
            g.drawLine(dx0, py, dx1, py);
        }

        float fontSize = (float) Math.max(9, Math.min(16, zoom / 5));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();

        for (int y = sy0; y < sy1; y++) {
            for (int x = sx0; x < sx1; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                double luminance = 0.299 * red + 0.587 * green + 0.114 * blue;
                g.setColor(luminance > 140 ? Color.BLACK : Color.WHITE);

                String[] lines = {"R:" + red, "G:" + green, "B:" + blue};
                double cx = offX + (x + 0.5) * zoom;
                double cy = offY + (y + 0.5) * zoom;
                double top = cy - lines.length * lineHeight / 2.0 + fm.getAscent();
                for (int i = 0; i < lines.length; i++) {
                    g.drawString(lines[i],
                            (float) (cx - fm.stringWidth(lines[i]) / 2.0),
                            (float) (top + i * lineHeight));
                }
            }
        }
    }
}