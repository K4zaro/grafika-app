package grafika.model;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class Line extends GShape {
    private static final long serialVersionUID = 1L;

    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public Tool getTool() {
        return Tool.LINE;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    @Override
    public boolean hit(double x, double y, double tolerance) {
        return Line2D.ptSegDist(x1, y1, x2, y2, x, y) <= tolerance;
    }

    @Override
    public void move(double dx, double dy) {
        x1 += dx;
        y1 += dy;
        x2 += dx;
        y2 += dy;
    }

    @Override
    public Point2D.Double[] getHandles() {
        return new Point2D.Double[]{new Point2D.Double(x1, y1), new Point2D.Double(x2, y2)};
    }

    @Override
    public void dragHandle(int index, double x, double y) {
        if (index == 0) {
            x1 = x;
            y1 = y;
        } else {
            x2 = x;
            y2 = y;
        }
    }

    @Override
    public double[] getParams() {
        return new double[]{x1, y1, x2, y2};
    }

    @Override
    public void setParams(double[] p) {
        x1 = p[0];
        y1 = p[1];
        x2 = p[2];
        y2 = p[3];
    }
}
