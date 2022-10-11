package aj.frame.components;

import aj.frame.AJFrame;
import aj.vector.BindableVector;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;

public abstract class Component {

    protected BindableVector position; // Position of Component relative to Holder, if null the position is determined by Layout
    protected BindableVector size; // Sets size of Component cannot be null

    protected Holder parent; // null if directly in AJFrame

    protected boolean drawOnNextCall = true; // Determines if should be drawn on next call

    // Constructor which automatically binds position and size to its size
    public Component(Holder parent) {
        position = new BindableVector();
        size = new BindableVector();
        parent.add(this);
        if (!parent.overridesSize()) size = size.bind(parent.getSize());
    }

    // Constructor with all necessary Information
    public Component(BindableVector position, BindableVector size) {
        this.position = position;
        this.size = size;
    }

    // Tells the Designable to render and do additional Component specific stuff
    public void draw(PGraphics g, boolean forceRender) {
        if (drawOnNextCall || forceRender) {
            // Tidy up
            drawOnNextCall = false;

            // Draw Component
            g.rectMode(PConstants.CORNER);
            g.noStroke();
            g.translate(position.getX(), position.getY());
            drawComponent(g);
            g.translate(-position.getX(), -position.getY());
        }
    }

    public Component addTo(Holder holder) {
        holder.add(this);
        return this;
    }

    public Component addTo(AJFrame ajFrame) {
        ajFrame.add(this);
        return this;
    }

    /**
     * PLS Override
     * Draw Component in there using PGraphics-Object g
     * g is translated that the Anchor-Point of the object is (0, 0)
     */
    protected abstract void drawComponent(PGraphics g);


    // Return true if on positon relative to its anchor point
    public boolean isOnPosition(PVector relativePos) {
        return  relativePos.x >= 0 &&
                relativePos.y >= 0 &&
                relativePos.x < size.getX() &&
                relativePos.y < size.getY();
    }

    // Returns absolute Position on AJFrame
    public PVector getAbsolutePosition() {
        if (parent == null) return position.copy();
        return parent.getAbsolutePosition().add(position);
    }


    public void drawOnNextCall() {
        drawOnNextCall = true;
    }

    public void drawParentOnNextCall() {
        if (parent != null) parent.drawOnNextCall();
        else drawOnNextCall();
    }

    // Getters 'n Setters

    protected Component setParent(Holder parent) {
        this.parent = parent;
        return this;
    }

    public Component setX(float x) {
        drawParentOnNextCall();
        position.x = x;
        return this;
    }

    public Component setX(float x, boolean resetXBindings) {
        setX(x);
        if (resetXBindings) {
            position.getBindings().forEach(b -> b.getScaling().x = 0);
            position.cleanBindings();
        }
        return this;
    }

    /**
     * Resets old x-Values and binding and binds the x coordinate to
     * @param xVector
     */
    public Component setX(BindableVector xVector) {
        setX(0, true);
        position.bind(xVector, new PVector(1, 0));
        return this;
    }

    public Component setY(float y) {
        drawParentOnNextCall();
        position.y = y;
        return this;
    }

    public Component setY(float y, boolean resetYBindings) {
        setY(y);
        if (resetYBindings) {
            position.getBindings().forEach(b -> b.getScaling().y = 0);
            position.cleanBindings();
        }
        return this;
    }

    /**
     * Resets old y-Values and binding and binds the y coordinate to
     * @param yVector
     */
    public Component setY(BindableVector yVector) {
        setY(0, true);
        position.bind(yVector, new PVector(0, 1));
        return this;
    }


    public Component setWidth(float width) {
        drawParentOnNextCall();
        size.x = width;
        return this;
    }


    public Component setWidth(float width, boolean resetWidthBindings) {
        setWidth(width);
        if (resetWidthBindings) {
            size.getBindings().forEach(b -> b.getScaling().x = 0);
            size.cleanBindings();
        }
        return this;
    }


    /**
     * Resets width and replaces it with the y component of
     * @param widthVector
     */
    public Component setWidth(BindableVector widthVector, float scaling) {
        setWidth(0, true);
        size.bind(widthVector, new PVector(scaling, 0));
        return this;
    }

    /**
     * Resets width and replaces it with the y component of
     * @param widthVector
     */
    public Component setWidth(BindableVector widthVector) {
        return setWidth(widthVector, 1);
    }


    public Component setHeight(float height) {
        drawParentOnNextCall();
        size.y = height;
        return this;
    }


    public Component setHeight(float height, boolean resetHeightBindings) {
        setHeight(height);
        if (resetHeightBindings) {
            size.getBindings().forEach(b -> b.getScaling().y = 0);
            size.cleanBindings();
        }
        return this;
    }


    /**
     * Resets height and replaces it with the y component of
     * @param heightVector
     */
    public Component setHeight(BindableVector heightVector, float scaling) {
        setHeight(0, true);
        size.bind(heightVector, new PVector(0, scaling));
        return this;
    }


    /**
     * Resets height and replaces it with the y component of
     * @param heightVector
     */
    public Component setHeight(BindableVector heightVector) {
        return setHeight(heightVector, 1);
    }


    public BindableVector getSize() {
        return size;
    }

    public BindableVector getPosition() {
        return position;
    }
}
