package aj.vector;

import processing.core.PVector;

public class Vector extends PVector {

    // STATIC PART
    // Static operations don't influence the vectors themselves

    public static Vector add(PVector vector1, PVector vector2) {
        return new Vector(vector1.x + vector2.x, vector1.y + vector2.y, vector1.z + vector2.z);
    }

    public static Vector sub(PVector vector1, PVector vector2) {
        return new Vector(vector1.x - vector2.x, vector1.y - vector2.y, vector1.z - vector2.z);
    }

    public static Vector mul(PVector vector1, PVector vector2) {
        return new Vector(vector1.x * vector2.x, vector1.y * vector2.y, vector1.z * vector2.z);
    }

    public static Vector div(PVector vector1, PVector vector2) {
        return new Vector(vector1.x / vector2.x, vector1.y / vector2.y, vector1.z / vector2.z);
    }



    // NON-STATIC PART

    public Vector() {
    }

    public Vector(float x, float y, float z) {
        super(x, y, z);
    }

    public Vector(float x, float y) {
        super(x, y);
    }

    // Returns copy of itself
    public Vector copy() {
        return new Vector(x, y, z);
    }


    /** Getters 'n Setters */
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }



}
