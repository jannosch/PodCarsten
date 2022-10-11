package aj.frame.components;

import aj.vector.BindableVector;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class Switch extends Component implements Clickable {

    int state = 0;
    int numStates;
    int[] stateColorOutline;
    int[] stateColorInside;
    List<BiConsumer<PGraphics, Integer>> drawer = new ArrayList<>();
    List<IntConsumer> listeners = new ArrayList<>();

    public Switch(BindableVector position, BindableVector size, int[] stateColorOutline, int[] stateColorInside) {
        super(position, size);
        assert stateColorOutline.length == stateColorInside.length;
        this.numStates = stateColorOutline.length;
        this.stateColorOutline = stateColorOutline;
        this.stateColorInside = stateColorInside;
    }

    @Override
    public void mousePressed(MouseEvent event) {

    }

    @Override
    public void mouseDragged(MouseEvent event, float pMouseX, float pMouseY) {

    }

    @Override
    public void mouseReleased(MouseEvent event) {

    }

    @Override
    public void mouseClicked(MouseEvent event) {
        toggle();

    }

    @Override
    public void mouseDoubleClicked(MouseEvent event) {

    }

    public Switch toggle() {
        setState((state + 1) % numStates);
        toggled(state);
        return this;
    }

    public Switch setState(int state) {
        assert 0 <= state && state < numStates;
        this.state = state;
        drawParentOnNextCall();
        stateSet(state);
        listeners.forEach(l -> l.accept(state));
        return this;
    }

    /**
     * Called when toggled
     * @param state
     */
    protected void toggled(int state) {}

    /**
     * Called when state is set
     * @param state new State
     */
    protected void stateSet(int state) {}

    public Switch addDrawer(BiConsumer<PGraphics, Integer> drawer) {
        this.drawer.add(drawer);
        return this;
    }

    public int getState() {
        return state;
    }

    public Switch addListener(IntConsumer listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    protected void drawComponent(PGraphics g) {
        g.rectMode(PConstants.CENTER);

        // Outline
        g.fill(stateColorOutline[state]);
        g.rect(size.getX() * 0.5f, size.getY() * 0.5f, 83-48, 48);
        g.circle(size.getX() * 0.5f - 17.5f, size.getY() * 0.5f, 48);
        g.circle(size.getX() * 0.5f + 17.5f, size.getY() * 0.5f, 48);

        // Inside
        g.fill(stateColorInside[state]);
        g.rect(size.getX() * 0.5f, size.getY() * 0.5f, 73-38, 38);
        g.circle(size.getX() * 0.5f - 17.5f, size.getY() * 0.5f, 38);
        g.circle(size.getX() * 0.5f + 17.5f, size.getY() * 0.5f, 38);

        // Ball
        // Circle
        g.fill(0xffffffff);
        g.circle(0.5f * size.getX() + 35f/(numStates-1) * (state) - 17.5f, 0.5f * size.getY(), 38);

        // Small Circle
        g.fill(0xff536270);
        g.circle(0.5f * size.getX() + 35f/(numStates-1) * (state) - 17.5f, 0.5f * size.getY(), 8);

        g.rectMode(PConstants.CORNER);

        drawer.forEach(l -> l.accept(g, state));
    }
}
