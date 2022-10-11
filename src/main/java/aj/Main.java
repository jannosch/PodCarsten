package aj;

import aj.frame.AJFrame;
import aj.frame.components.*;
import aj.vector.BindableVector;
import aj.vector.Vector;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.event.MouseEvent;

public class Main {

    public static void main(String[] args) {
	    AJFrame frame = new AJFrame();


        HorizontalBar bar = (HorizontalBar) new HorizontalBar(new BindableVector(0, 0), new BindableVector(0, 200).bind(frame.getSize(), new PVector(1, 0.2f))).addTo(frame);

        Component component = new Component(new BindableVector(100, 100), new BindableVector(0, 200).bind(frame.getSize(), new Vector(0.5f, 0.1f))) {
            @Override
            protected void drawComponent(PGraphics g) {
                g.fill(27, 185, 187);
                g.rect(0, 0, size.getX(), size.getY());
            }
        }.addTo(bar);

        Button button = (Button) new Button(new BindableVector(200, 200), new BindableVector(100, 33)) {
            @Override
            public void mouseClicked(MouseEvent event) {
                component.setX((float) Math.random() * 200);
            }
        }.addTo(bar);

        Slider slider = (Slider) new Slider(new BindableVector(42, 42), new BindableVector(100, 42), "Slider").addTo(bar);


        Switch mySwitch = (Switch) new Switch(new BindableVector(42, 42), new BindableVector(100, 42), new int[] { 0xff536270, 0xff31B961 }, new int[] { 0xff111319, 0xff235530 }).addTo(bar);

        Switch my3Switch = (Switch) new Switch(new BindableVector(42, 42), new BindableVector(100, 42), new int[] { 0xff1BB9BB, 0xff536270, 0xffFF4D5B }, new int[] { 0xff235155, 0xff111319, 0xff552325 }).addTo(bar);

    }
}
