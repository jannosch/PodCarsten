package podCarsten.frontend;

import aj.frame.components.Button;
import aj.frame.components.Clickable;
import aj.frame.components.Component;
import aj.vector.BindableVector;
import podCarsten.backend.Audible;
import podCarsten.backend.AudioEffect;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static podCarsten.frontend.Main.*;

public class AudioTrackComponent extends Component implements Clickable {

    static final float DB_DRAW_MIN = -30;
    static final int BUFFER_SIZE = 2048;

    int analyserAccuracy = 1000;

    Audible audible = null;
    double seconds;
    int sampleLength = 0;
    float sampleRate;
    float[] amounts;
    AudioFormat format;
    Button playButton;
    boolean bypassed = false;

    boolean playing;
    int samplePosition = 0;
    SourceDataLine line;
    AudioPlayerThread thread;
    AudioEffect[] audioEffects = new AudioEffect[7];
    CompletableFuture<Boolean> amountCalculator;


    public AudioTrackComponent(BindableVector position, BindableVector size, Button playButton) {
        super(position, size);
        this.playButton = playButton;
    }

    public AudioTrackComponent loadAudible(Audible audible, double seconds, float sampleRate, AudioFormat format) {
        this.audible = audible;
        this.seconds = seconds;
        this.sampleRate = sampleRate;
        this.format = format;

        sampleLength = (int) (seconds * sampleRate);

        analyserAccuracy = (int)size.getX();
        amounts = new float[analyserAccuracy];

        amountCalculator = CompletableFuture.supplyAsync(() -> {
            IntStream.range(0, amounts.length).parallel().forEach(i -> {
                int startPos = (int) (i / (analyserAccuracy + 1.0) * sampleLength);
                amounts[i] = (float) -((audible.splice(startPos, (int) (sampleLength / (analyserAccuracy + 1.0)), null).getPeakInDBFS() - DB_DRAW_MIN) / DB_DRAW_MIN);
            });
            return true;
        });
        drawOnNextCall();

        // Layout AudioFile Visualisation
        /*amounts = DoubleStream.iterate(0, s -> s + (double) sampleLength / analyserAccuracy).limit(analyserAccuracy)
                .map(s -> audible.splice((int) s, (int) ((double) sampleLength / analyserAccuracy), null).getPeakInDBFS()).toArray();*/

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            // Handle the error.
        }
        // Obtain and open the line.
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, BUFFER_SIZE * format.getFrameSize());
        } catch (LineUnavailableException ex) {
            // Handle the error.
            //...
        }

        drawOnNextCall();
        return this;
    }

    public boolean togglePlay() {
        return playing = playing ? stop() : play();
    }

    public boolean play() {
        if (audible != null) {
            if (thread != null) thread.pause();
            thread = new AudioPlayerThread(line, audible, sampleLength, format);
            thread.audible = thread.audible.setEffects(audioEffects);
            if (samplePosition + BUFFER_SIZE >= sampleLength) samplePosition = 0;
            drawOnNextCall();

            playButton.setColor(COLOR_PRIMARY);
            playButton.setIcon(SHAPE_PAUSE);
            return playing = thread.play(samplePosition);
        } else {
            return playing = false;
        }
    }

    public boolean stop() {
        if (thread != null && playing) {
            samplePosition = thread.pause();
            thread = null;
        }

        playButton.setColor(COLOR_ELEMENTS);
        playButton.setIcon(SHAPE_PLAY);

        return playing = false;
    }

    public void jumpAmount(double seconds) {
        jumpToSample((int) (Math.min(samplePosition + seconds * sampleRate, Integer.MAX_VALUE)));
    }

    private void jumpToSample(int sampleNr) {
        if (audible != null) {

            samplePosition = sampleNr;

            if (samplePosition >= sampleLength) {
                stop();
                samplePosition = sampleLength - 1;

            } else {
                if (samplePosition < 0)
                    samplePosition = 0;
                if (playing)
                    thread.samplePosition = samplePosition;
            }

            drawOnNextCall();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        clickOnPosition(event.getX());
    }

    @Override
    public void mouseDragged(MouseEvent event, float pMouseX, float pMouseY) {
        clickOnPosition(event.getX());
    }

    private void clickOnPosition(float mouseX) {
        jumpToSample((int) ((mouseX / size.getX()) * sampleLength));
    }

    @Override
    public void mouseReleased(MouseEvent event) {

    }

    @Override
    public void mouseClicked(MouseEvent event) {

    }

    @Override
    public void mouseDoubleClicked(MouseEvent event) {

    }

    public void setEffect(int index, AudioEffect audioEffect) {
        audioEffects[index] = audioEffect;
        if (!bypassed && Arrays.stream(audioEffects).noneMatch(Objects::isNull))
            audible = audible.setEffects(audioEffects);
        if (thread != null) {
            thread.audible = audible;
        }
    }

    public void bypass(boolean bypass) {
        bypassed = bypass;
        if (audible != null) {
            if (bypass) {
                audible = audible.setEffects();
            } else if (Arrays.stream(audioEffects).noneMatch(Objects::isNull)) {
                audible = audible.setEffects(audioEffects);
            }
            if (thread != null) {
                thread.audible = audible;
            }
        }
    }

    @Override
    protected void drawComponent(PGraphics g) {
        if (amountCalculator != null && !amountCalculator.isDone()) {
            drawOnNextCall();
        }

        // Background
        g.fill(Main.COLOR_BACKGROUND);
        g.rect(0, 0, size.getX(), size.getY());

        if (audible != null) {
            // QUERY FROM THREAD
            if (thread == null || thread.playing != playing) {
                stop();
            } if (playing && thread != null) {
                samplePosition = thread.samplePosition;
            }

            // Audio File
            float xOffset = size.getX() / amounts.length;
            g.rectMode(PConstants.CENTER);
            g.fill(COLOR_PRIMARY);
            g.noStroke();
            for (int i = 0; i < amounts.length; i++) {
                g.rect((i + 0.5f) * xOffset, 0.5f * size.getY(), xOffset+1f, Math.max(0.8f * amounts[i] * size.getY(), xOffset));
            }

            // Position Marker
            g.rectMode(PConstants.CORNER);
            g.fill(0xFFFFFFFF);
            g.rect(Math.min(size.getX() * ((float) samplePosition / sampleLength), size.getX() - 2), 0, 2, size.getY());

            if (playing) drawOnNextCall();
        }
    }

}
