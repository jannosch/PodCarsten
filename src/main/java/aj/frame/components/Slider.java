package aj.frame.components;

import aj.vector.BindableVector;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class Slider extends Component implements Clickable {

    float amount;
    float sliderHeight;
    String text;
    int backgroundColor = 0xff14151a;
    int color = 0xFF2FB961;
    boolean grabbed = false;
    boolean midCentered = true;
    UnaryOperator<Float> mapping = (x) -> x;
    List<Consumer<Float>> listeners = new ArrayList<>();
    PShape shape;

    public Slider(BindableVector position, BindableVector size, String text) {
        super(position, size);
        this.text = text;
        sliderHeight = 0.5f * size.getY() - 70;
    }

    public Slider(BindableVector position, BindableVector size, String text, int backgroundColor, int color) {
        this(position, size, text);
        this.backgroundColor = backgroundColor;
        this.color = color;
    }

    public Slider(BindableVector position, BindableVector size, String text, int backgroundColor, int color, boolean midCentered, UnaryOperator<Float> mapping, PShape shape) {
        this(position, size, text, backgroundColor, color);
        this.midCentered = midCentered;
        this.mapping = mapping;
        shape.disableStyle();
        this.shape = shape;

        amount = midCentered ? 0 : -1;
    }

    public float getAmount() {
        return mapping.apply(amount);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        grabbed = true;
    }

    @Override
    public void mouseDragged(MouseEvent event, float pMouseX, float pMouseY) {
        // Move döngel
        amount -= (event.getY() - pMouseY) / sliderHeight * (event.isShiftDown() ? 0.15f : 1f);

        // Reset to bounds
        if (amount > 1) amount = 1;
        else if (amount < -1) amount = -1;

        listeners.forEach(l -> l.accept(getAmount()));
        drawParentOnNextCall();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        grabbed = false;

        drawParentOnNextCall();
    }

    @Override
    public void mouseClicked(MouseEvent event) {

    }

    @Override
    public void mouseDoubleClicked(MouseEvent event) {
        if (midCentered) amount = 0;
        else amount = -1;

        listeners.forEach(l -> l.accept(getAmount()));
        drawParentOnNextCall();
    }

    public Slider addListener(Consumer<Float> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    protected void drawComponent(PGraphics g) {
        sliderHeight = 0.5f * size.getY() - 70;
        // Background
        /*
        g.rectMode(PConstants.CORNER);
        g.fill(backgroundColor);
        g.rect(0, 0, size.getX(), size.getY());
         */

        // Text
        g.fill(230, 235, 240);
        g.textAlign(PConstants.CENTER);
        if (grabbed) {
            g.text(String.format("%.0f" ,getAmount() * 100) + "%", 0.5f * size.getX(), size.getY() - 10);
        } else {
            g.text(text, 0.5f * size.getX(), size.getY() - 10);
        }

        // Shape
        if (shape != null) {
            g.shapeMode(PConstants.CENTER);
            g.fill(color);
            g.shape(shape, 0.5f * size.getX(),  15, size.getX() * 0.35f, size.getX() * 0.35f);
        }

        // Background Slider
        g.fill(0xff536270);
        g.rectMode(PConstants.CENTER);
        g.rect(0.5f * size.getX(), 0.5f * size.getY(), 18, 2 * sliderHeight);
        g.circle(0.5f * size.getX(), 0.5f * size.getY() + sliderHeight, 18);
        g.circle(0.5f * size.getX(), 0.5f * size.getY() - sliderHeight, 18);

        // Colored Part of Slider
        g.fill(color);
        if (midCentered) {
            g.rect(0.5f * size.getX(), 0.5f * size.getY() - 0.5f * sliderHeight * amount, 18, -sliderHeight * amount);
        } else {
            g.circle(0.5f * size.getX(), 0.5f * size.getY() + sliderHeight, 18);
            g.rect(0.5f * size.getX(), 0.5f * size.getY() + sliderHeight - 0.5f * sliderHeight * (amount + 1), 18, -sliderHeight * (amount + 1));
        }

        // Circle
        g.fill(0xffffffff);
        g.circle(0.5f * size.getX(), 0.5f * size.getY() - sliderHeight * amount, 38);

        // Small Circle
        if (!grabbed) {
            g.fill(0xff536270);
            g.circle(0.5f * size.getX(), 0.5f * size.getY() - sliderHeight * amount, 8);
        }
    }
}
