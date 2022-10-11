package aj.frame;

import aj.frame.components.Clickable;
import aj.frame.components.Component;
import aj.frame.components.Holder;
import aj.vector.BindableVector;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import processing.event.MouseEvent;

import java.io.File;
import java.util.function.Consumer;

public class AJFrame extends PApplet {

    public static PFont FONT_H1_THICC;
    public static PFont FONT_H1;
    public static PFont FONT_TEXT;
    public static PFont FONT_SMALL;

    Holder holder;
    Component pressedComponent = null;
    Consumer<File> fileInputListener = null;
    Consumer<File> fileOutputListener = null;

    long doubleClickMs = 350;
    long lastClickMs;
    Clickable lastClickedComponent = null;

    public AJFrame() {
        width = 800;
        height = 450;
        PApplet.runSketch(new String[]{""}, this);
    }

    public AJFrame(int width, int height) {
        this.width = width;
        this.height = height;
        PApplet.runSketch(new String[]{""}, this);
    }

    public void settings() {
        size(width, height);
        holder = new Holder(new BindableVector(), new BindableVector(width, height));
    }

    public void setup() {
        FONT_H1_THICC = createFont("Roboto-Bold", 38, true);
        FONT_H1 = createFont("Roboto-Medium", 38, true);
        FONT_TEXT = createFont("Roboto-Medium", 28, true);
        FONT_SMALL = createFont("Roboto-Medium", 18, true);

        textFont(FONT_TEXT);
        textLeading(28);

        frameRate(30f);
        windowTitle("PodCarsten");
        windowResizable(true);
        noStroke();
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        Component hoveredComponent = holder.getComponentOnPosition(new PVector(event.getX(), event.getY()));

        if (hoveredComponent instanceof Clickable c) {
            cursor(c.getCursor());
        } else {
            cursor(PApplet.ARROW);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        pressedComponent = holder.getComponentOnPosition(new PVector(event.getX(), event.getY()));

        if (pressedComponent instanceof Clickable c) c.mousePressed(event);

        if (lastClickedComponent == null) lastClickMs = System.currentTimeMillis();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (pressedComponent instanceof Clickable c) c.mouseDragged(event, pmouseX, pmouseY);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        Component releasedComponent = holder.getComponentOnPosition(new PVector(event.getX(), event.getY()));

        if (releasedComponent instanceof Clickable c) {
            c.mouseReleased(event);
            if (releasedComponent == pressedComponent) {
                c.mouseClicked(event);

                if (lastClickedComponent == c && System.currentTimeMillis() - lastClickMs <= doubleClickMs) {
                    c.mouseDoubleClicked(event);
                    lastClickedComponent = null;
                } else if (System.currentTimeMillis() - lastClickMs <= doubleClickMs){
                    lastClickMs = System.currentTimeMillis();
                    lastClickedComponent = c;
                } else {
                    lastClickedComponent = null;
                }
            } else {
                lastClickedComponent = null;
            }
        }

        if (pressedComponent instanceof Clickable c) {
            c.mouseReleased(event);
        }

        pressedComponent = null;
    }

    @Override
    public void windowResized() {
        holder.setWidth(width);
        holder.setHeight(height);
        holder.draw(super.g, true);
    }

    public AJFrame selectInputFile(Consumer<File> fileListener) {
        this.fileInputListener = fileListener;
        selectInput("Wähle eine Datei zum Laden aus:", "inputFileSelected");
        return this;
    }

    public AJFrame selectOutputFile(Consumer<File> fileListener) {
        this.fileOutputListener = fileListener;
        selectOutput("Wähle eine Datei zum Speichern aus:", "outputFileSelected");
        return this;
    }

    public void inputFileSelected(File file) {
        if (fileInputListener != null && file != null) fileInputListener.accept(file);
    }

    public void outputFileSelected(File file) {
        if (fileOutputListener != null && file != null) fileOutputListener.accept(file);
    }

    public void draw() {
        holder.draw(super.g, false);
    }

    // NON-PROCESSING METHODS
    public void add(Component component) {
        holder.add(component);
    }

    // GETTERS N SETTERS
    public BindableVector getSize() {
        return holder.getSize();
    }

    public Holder getHolder() {
        return holder;
    }
}
