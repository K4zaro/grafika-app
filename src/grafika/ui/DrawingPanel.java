package grafika.ui;

import grafika.model.Drawing;
import grafika.model.GShape;
import grafika.model.ShapeFactory;
import grafika.model.Tool;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class DrawingPanel extends JPanel {
    private static final double HIT_TOLERANCE = 5;
    private static final double HANDLE_TOLERANCE = 8;
    private static final Color SELECTED_COLOR = new Color(0, 100, 220);

    private final Drawing drawing;
    private final List<Point2D.Double> pending = new ArrayList<>();
    private Tool tool = Tool.SELECT;
    private Point2D.Double mouse;
    private Point2D.Double lastDrag;
    private int dragHandle = -1;
    private boolean moving;

    public DrawingPanel(Drawing drawing) {
        this.drawing = drawing;
        setBackground(Color.WHITE);
        setFocusable(true);
        drawing.addChangeListener(this::repaint);
        installMouse();
        installKeys();
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        pending.clear();
        drawing.select(null);
    }

    private void installMouse() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point2D.Double p = new Point2D.Double(e.getX(), e.getY());
                if (tool == Tool.SELECT) {
                    pressSelect(p);
                } else {
                    pressDraw(p);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragTo(new Point2D.Double(e.getX(), e.getY()));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = new Point2D.Double(e.getX(), e.getY());
                if (!pending.isEmpty()) {
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragHandle = -1;
                moving = false;
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void installKeys() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "delete");
        getActionMap().put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawing.deleteSelected();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pending.clear();
                repaint();
            }
        });
    }

    private void pressSelect(Point2D.Double p) {
        dragHandle = -1;
        moving = false;
        GShape selected = drawing.getSelected();
        if (selected != null) {
            Point2D.Double[] handles = selected.getHandles();
            for (int i = 0; i < handles.length; i++) {
                if (handles[i].distance(p) <= HANDLE_TOLERANCE) {
                    dragHandle = i;
                    return;
                }
            }
        }
        GShape found = drawing.findAt(p.x, p.y, HIT_TOLERANCE);
        if (found != null) {
            moving = true;
            lastDrag = p;
        }
        drawing.select(found);
    }

    private void dragTo(Point2D.Double p) {
        mouse = p;
        GShape selected = drawing.getSelected();
        if (selected == null) {
            return;
        }
        if (dragHandle >= 0) {
            selected.dragHandle(dragHandle, p.x, p.y);
            drawing.fireChanged();
        } else if (moving) {
            selected.move(p.x - lastDrag.x, p.y - lastDrag.y);
            lastDrag = p;
            drawing.fireChanged();
        }
    }

    private void pressDraw(Point2D.Double p) {
        pending.add(p);
        if (pending.size() < 2) {
            return;
        }
        ShapeFactory.fromPoints(tool, pending.get(0), pending.get(1)).ifPresent(drawing::add);
        pending.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setStroke(new BasicStroke(2f));
        GShape selected = drawing.getSelected();
        for (GShape s : drawing.getShapes()) {
            g.setColor(s == selected ? SELECTED_COLOR : Color.BLACK);
            s.draw(g);
        }
        if (selected != null) {
            paintHandles(g, selected);
        }
        if (!pending.isEmpty() && mouse != null) {
            paintPreview(g);
        }
        g.dispose();
    }

    private void paintHandles(Graphics2D g, GShape shape) {
        g.setStroke(new BasicStroke(1f));
        for (Point2D.Double h : shape.getHandles()) {
            Rectangle2D square = new Rectangle2D.Double(h.x - 4, h.y - 4, 8, 8);
            g.setColor(Color.WHITE);
            g.fill(square);
            g.setColor(SELECTED_COLOR);
            g.draw(square);
        }
    }

    private void paintPreview(Graphics2D g) {
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{5f, 5f}, 0f));
        ShapeFactory.fromPoints(tool, pending.get(0), mouse).ifPresent(s -> s.draw(g));
    }
}
