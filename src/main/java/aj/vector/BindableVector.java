package aj.vector;

import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

public class BindableVector extends Vector {

    private List<Binding> bindings = new ArrayList<>();

    public BindableVector() {
    }

    public BindableVector(float x, float y) {
        super(x, y);
    }

    public BindableVector(ArrayList<Binding> bindings) {
        this.bindings = bindings;
    }

    public BindableVector(float x, float y, ArrayList<Binding> bindings) {
        super(x, y);
        this.bindings = bindings;
    }


    /** Binds this BindableVector to another Vector */
    public BindableVector bind(BindableVector vector) {
        bindings.add(new Binding(vector));
        return this;
    }

    /** Binds this BindableVector to another Vector.
     * Scales the values of Vector before applying
     */
    public BindableVector bind(BindableVector vector, PVector scaling) {
        bindings.add(new Binding(vector, scaling));
        return this;
    }

    /** Binds this BindableVector to another Vector.
     * Scales the values of Vector before applying
     */
    public BindableVector bind(BindableVector vector, float xScaling, float yScaling) {
        bindings.add(new Binding(vector, new PVector(xScaling, yScaling)));
        return this;
    }

    /**
     * Cleans all unnecessary bindings
     */
    public void cleanBindings() {
        bindings.removeAll(bindings.stream().filter(b -> b.getScaling().equals(new PVector(0, 0))).toList());
    }


    // Getters 'n Setters
    public List<Binding> getBindings() {
        return bindings;
    }

    @Override
    public float getX() {
        float xValue = super.getX();

        for (Binding binding : bindings) {
            xValue += binding.getX();
        }

        return xValue;
    }

    @Override
    public float getY() {
        float yValue = super.getY();

        for (Binding binding : bindings) {
            yValue += binding.getY();
        }

        return yValue;
    }

    public Vector getCurrentVector() {
        Vector vector = this.copy();

        for (Binding b : bindings) {
            vector.add(b.getVector());
        }

        return vector;
    }
}
