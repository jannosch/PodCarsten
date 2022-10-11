package aj.frame.components;

import processing.core.PApplet;
import processing.event.MouseEvent;

public interface Clickable {

    void mousePressed(MouseEvent event);
    void mouseDragged(MouseEvent event, float pMouseX, float pMouseY);
    void mouseReleased(MouseEvent event);
    void mouseClicked(MouseEvent event);
    void mouseDoubleClicked(MouseEvent event);

    default int getCursor() {
        return PApplet.HAND;
    }

}
