package grafika.model;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

public class Circle extends GShape {
    private static final long serialVersionUID = 1L;

    private double cx;
    private double cy;
    private double r;

    public Circle(double cx, double cy, double r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
    }

    @Override
    public Tool getTool() {
        return Tool.CIRCLE;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
    }

    @Override
    public boolean hit(double x, double y, double tolerance) {
        return Math.abs(Point2D.distance(cx, cy, x, y) - r) <= tolerance;
    }

    @Override
    public void move(double dx, double dy) {
        cx += dx;
        cy += dy;
    }

    @Override
    public Point2D.Double[] getHandles() {
        return new Point2D.Double[]{
                new Point2D.Double(cx + r, cy), new Point2D.Double(cx, cy + r),
                new Point2D.Double(cx - r, cy), new Point2D.Double(cx, cy - r)};
    }

    @Override
    public void dragHandle(int index, double x, double y) {
        r = Math.max(1, Point2D.distance(cx, cy, x, y));
    }

    @Override
    public double[] getParams() {
        return new double[]{cx, cy, r};
    }

    @Override
    public void setParams(double[] p) {
        if (p[2] <= 0) {
            throw new IllegalArgumentException("Promień musi być większy od zera.");
        }
        cx = p[0];
        cy = p[1];
        r = p[2];
    }
}
