package grafika.model;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

public abstract class GShape implements Serializable {
    private static final long serialVersionUID = 1L;

    public abstract Tool getTool();

    public abstract void draw(Graphics2D g);

    public abstract boolean hit(double x, double y, double tolerance);

    public abstract void move(double dx, double dy);

    public abstract Point2D.Double[] getHandles();

    public abstract void dragHandle(int index, double x, double y);

    public abstract double[] getParams();

    public abstract void setParams(double[] params);
}
