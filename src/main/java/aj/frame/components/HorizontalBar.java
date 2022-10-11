package aj.frame.components;

import aj.vector.BindableVector;
import processing.core.PVector;

public class HorizontalBar extends Holder {

    public HorizontalBar(Holder parent) {
        super(parent);
    }

    /**
     * @param position
     * @param size
     */
    public HorizontalBar(BindableVector position, BindableVector size) {
        super(position, size);
    }

    @Override
    public void add(Component component) {
        component.setHeight(size);
        component.setY(0, true);

        if (components.size() == 0) {
            component.setX(0, true);
        } else {
            component.setX(components.get(components.size() - 1).getPosition());
            component.getPosition().bind(components.get(components.size() - 1).getSize(), new PVector(1, 0));
        }

        super.add(component);
    }

    @Override
    protected boolean overridesSize() {
        return true;
    }
}
