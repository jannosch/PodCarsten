package aj.frame.components;

import aj.vector.BindableVector;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Holder extends Component {

    List<Component> components = new ArrayList<>();

    public Holder(Holder parent) {
        super(parent);
    }

    public Holder(BindableVector position, BindableVector size) {
        super(position, size);
    }

    // Adds component to Holder
    public void add(Component component) {
        components.add(component);
        component.setParent(this);
        drawOnNextCall = true;
    }

    /**
     *
     * @param relativePos
     * @return Component on Mouse pos
     */
    public Component getComponentOnPosition(PVector relativePos) {
        return IntStream.range(0, components.size()).mapToObj(i -> components.get(components.size() - 1 - i))
                .filter(c -> c.isOnPosition(PVector.sub(relativePos, c.position.getCurrentVector())))
                .findFirst()
                .map(c -> c instanceof Holder ? ((Holder) c).getComponentOnPosition(PVector.sub(relativePos, c.position.getCurrentVector())) : c)
                .orElse(this);
    }

    // If holder is drawn, all child-components

    @Override
    public void draw(PGraphics g, boolean forceRender) {

        g.translate(position.getX(), position.getY());

        if (drawOnNextCall || forceRender) {

            // Draw Component
            g.rectMode(PConstants.CORNER);
            g.noStroke();
            drawComponent(g);
        }

        // Draw Child-Components
        components.forEach(c -> c.draw(g, drawOnNextCall || forceRender));

        // Tidy up
        g.translate(-position.getX(), -position.getY());
        drawOnNextCall = false;
    }

    /**
     * Draws itself with a dark Background
     * @param g
     */
    @Override
    protected void drawComponent(PGraphics g) {
        g.fill(20, 21, 26);
        g.rect(0, 0, size.getX(), size.getY());
    }

    protected boolean overridesSize() {
        return false;
    }


}
