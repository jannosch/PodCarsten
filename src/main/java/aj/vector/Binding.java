package aj.vector;

import processing.core.PVector;

public class Binding {

    private BindableVector vector;
    private PVector scaling = new PVector(1, 1);

    /*
    Bind a BindableVector to another Vector.
     */
    public Binding(BindableVector vector) {
        this.vector = vector;
    }

    public Binding(BindableVector vector, PVector scaling) {
        this.vector = vector;
        this.scaling = scaling;
    }

    // Calculates the influence of this Binding
    public float getX() {
        return vector.getX() * scaling.x;
    }

    // Calculates the influence of this Binding
    public float getY() {
        return vector.getY() * scaling.y;
    }

    public PVector getScaling() {
        return scaling;
    }

    // Calculates the influence of this Binding
    public Vector getVector() {
        return new Vector(getX(), getY());
    }

}
