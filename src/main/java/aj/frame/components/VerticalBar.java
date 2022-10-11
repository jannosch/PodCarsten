package aj.frame.components;

import aj.vector.BindableVector;
import processing.core.PVector;

public class VerticalBar extends Holder {

    public VerticalBar(Holder parent) {
        super(parent);
    }

    /**
     * @param position
     * @param size
     */
    public VerticalBar(BindableVector position, BindableVector size) {
        super(position, size);
    }

    @Override
    public void add(Component component) {
        component.setWidth(size);
        component.setX(0, true);

        if (components.size() == 0) {
            component.setY(0, true);
        } else {
            component.setY(components.get(components.size() - 1).getPosition());
            component.getPosition().bind(components.get(components.size() - 1).getSize(), new PVector(0, 1));
        }

        super.add(component);
    }

    @Override
    protected boolean overridesSize() {
        return true;
    }
}
