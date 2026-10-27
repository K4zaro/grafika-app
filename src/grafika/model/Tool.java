package grafika.model;

public enum Tool {
    SELECT("Zaznacz / edytuj"),
    LINE("Linia", "x1", "y1", "x2", "y2"),
    RECT("Prostokąt", "x", "y", "szerokość", "wysokość"),
    CIRCLE("Okrąg", "środek x", "środek y", "promień");

    private final String label;
    private final String[] paramLabels;

    Tool(String label, String... paramLabels) {
        this.label = label;
        this.paramLabels = paramLabels;
    }

    public String getLabel() {
        return label;
    }

    public String[] getParamLabels() {
        return paramLabels.clone();
    }

    public int getParamCount() {
        return paramLabels.length;
    }

    public boolean isDrawingTool() {
        return this != SELECT;
    }
}
