package grafika.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Drawing {

    private final List<GShape> shapes = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private GShape selected;

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void fireChanged() {
        for (Runnable l : listeners) {
            l.run();
        }
    }

    public List<GShape> getShapes() {
        return Collections.unmodifiableList(shapes);
    }

    public GShape getSelected() {
        return selected;
    }

    public void select(GShape shape) {
        selected = shape;
        fireChanged();
    }

    public void add(GShape shape) {
        shapes.add(shape);
        fireChanged();
    }

    public void deleteSelected() {
        if (selected != null) {
            shapes.remove(selected);
            selected = null;
            fireChanged();
        }
    }

    public void clear() {
        shapes.clear();
        selected = null;
        fireChanged();
    }

    public void replaceAll(List<GShape> newShapes) {
        shapes.clear();
        shapes.addAll(newShapes);
        selected = null;
        fireChanged();
    }

    public GShape findAt(double x, double y, double tolerance) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).hit(x, y, tolerance)) {
                return shapes.get(i);
            }
        }
        return null;
    }
}
