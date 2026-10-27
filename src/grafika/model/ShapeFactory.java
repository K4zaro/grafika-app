package grafika.model;

import java.awt.geom.Point2D;
import java.util.Optional;

public final class ShapeFactory {

    private ShapeFactory() {
    }

    public static Optional<GShape> fromPoints(Tool tool, Point2D a, Point2D b) {
        switch (tool) {
            case LINE:
                return Optional.of(new Line(a.getX(), a.getY(), b.getX(), b.getY()));
            case RECT:
                return Optional.of(new Rect(a.getX(), a.getY(), b.getX(), b.getY()));
            case CIRCLE:
                double r = a.distance(b);
                return r < 1 ? Optional.empty() : Optional.of(new Circle(a.getX(), a.getY(), r));
            default:
                return Optional.empty();
        }
    }

    public static GShape fromParams(Tool tool, double[] p) {
        if (p.length != tool.getParamCount()) {
            throw new IllegalArgumentException("Nieprawidłowa liczba parametrów.");
        }
        switch (tool) {
            case LINE:
                return new Line(p[0], p[1], p[2], p[3]);
            case RECT:
                return new Rect(p[0], p[1], p[0] + p[2], p[1] + p[3]);
            case CIRCLE:
                if (p[2] <= 0) {
                    throw new IllegalArgumentException("Promień musi być większy od zera.");
                }
                return new Circle(p[0], p[1], p[2]);
            default:
                throw new IllegalArgumentException("To narzędzie nie tworzy figur.");
        }
    }
}
