package aj.frame.components;

import aj.vector.BindableVector;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class Button extends Component implements Clickable {

    protected String text = null;
    protected PShape icon = null;
    protected int color = 0xFF536270;
    protected int colorActive = 0xFF14151A;
    protected int contentColor = 0xFFFFFFFF;
    protected float[] roundedCorners = null;
    protected boolean active = false;
    protected List<Runnable> listeners = new ArrayList<>();

    public Button(BindableVector position, BindableVector size) {
        super(position, size);
    }

    public Button(BindableVector position, BindableVector size, String text, int color, int contentColor, float[] roundedCorners) {
        this(position, size);
        this.text = text;
        this.color = color;
        this.contentColor = contentColor;
        this.roundedCorners = roundedCorners;
    }

    public Button(BindableVector position, BindableVector size, String text, int color, int contentColor, float roundedCorners) {
        this(position, size, text, color, contentColor, new float[]{roundedCorners, roundedCorners, roundedCorners, roundedCorners});
    }

    public Button(BindableVector position, BindableVector size, PShape icon, int color, int contentColor, float[] roundedCorners) {
        this(position, size);
        this.icon = icon;
        icon.disableStyle();
        this.color = color;
        this.contentColor = contentColor;
        this.roundedCorners = roundedCorners;
    }

    public Button(BindableVector position, BindableVector size, PShape icon, int color, int contentColor, float roundedCorners) {
        this(position, size, icon, color, contentColor, new float[]{roundedCorners, roundedCorners, roundedCorners, roundedCorners});
    }

    @Override
    public void mousePressed(MouseEvent event) {
        active = true;
        drawOnNextCall();
    }

    @Override
    public void mouseDragged(MouseEvent event, float pMouseX, float pMouseY) {

    }

    @Override
    public void mouseReleased(MouseEvent event) {
        active = false;
        drawOnNextCall();
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        listeners.forEach(Runnable::run);
    }

    @Override
    public void mouseDoubleClicked(MouseEvent event) {

    }

    public Button addListener(Runnable listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    protected void drawComponent(PGraphics g) {
        if (active) g.fill(colorActive);
        else g.fill(color);
        if (roundedCorners == null) {
            g.rect(0, 0, size.getX(), size.getY());
        } else {
            g.rect(0, 0, size.getX(), size.getY(), roundedCorners[0], roundedCorners[1], roundedCorners[2], roundedCorners[3]);
        }

        if (text != null) {
            g.textAlign(PConstants.CENTER);
            g.fill(contentColor);
            g.text(text, size.getX() / 2, size.getY() / 2 + 10);
        } else if (icon != null) {
            g.fill(contentColor);
            g.shapeMode(PConstants.CENTER);
            g.shape(icon, size.getX() / 2, size.getY() / 2);
        }
    }


    public void setIcon(PShape icon) {
        this.text = null;
        icon.disableStyle();
        this.icon = icon;
        drawOnNextCall();
    }

    public void setColor(int color) {
        this.color = color;
        drawOnNextCall();
    }
}
