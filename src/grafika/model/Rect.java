package grafika.model;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Rect extends GShape {
    private static final long serialVersionUID = 1L;

    private double ax;
    private double ay;
    private double bx;
    private double by;

    public Rect(double ax, double ay, double bx, double by) {
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
    }

    private double minX() {
        return Math.min(ax, bx);
    }

    private double minY() {
        return Math.min(ay, by);
    }

    private double maxX() {
        return Math.max(ax, bx);
    }

    private double maxY() {
        return Math.max(ay, by);
    }

    @Override
    public Tool getTool() {
        return Tool.RECT;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(new Rectangle2D.Double(minX(), minY(), maxX() - minX(), maxY() - minY()));
    }

    @Override
    public boolean hit(double x, double y, double t) {
        boolean outer = x >= minX() - t && x <= maxX() + t && y >= minY() - t && y <= maxY() + t;
        boolean inner = x > minX() + t && x < maxX() - t && y > minY() + t && y < maxY() - t;
        return outer && !inner;
    }

    @Override
    public void move(double dx, double dy) {
        ax += dx;
        ay += dy;
        bx += dx;
        by += dy;
    }

    @Override
    public Point2D.Double[] getHandles() {
        return new Point2D.Double[]{
                new Point2D.Double(ax, ay), new Point2D.Double(bx, ay),
                new Point2D.Double(bx, by), new Point2D.Double(ax, by)};
    }

    @Override
    public void dragHandle(int index, double x, double y) {
        switch (index) {
            case 0:
                ax = x;
                ay = y;
                break;
            case 1:
                bx = x;
                ay = y;
                break;
            case 2:
                bx = x;
                by = y;
                break;
            default:
                ax = x;
                by = y;
                break;
        }
    }

    @Override
    public double[] getParams() {
        return new double[]{minX(), minY(), maxX() - minX(), maxY() - minY()};
    }

    @Override
    public void setParams(double[] p) {
        ax = p[0];
        ay = p[1];
        bx = p[0] + p[2];
        by = p[1] + p[3];
    }
}
