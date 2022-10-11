package podCarsten.frontend;

import aj.frame.AJFrame;
import aj.frame.components.Component;
import aj.vector.BindableVector;
import podCarsten.backend.Audible;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.Arrays;
import java.util.stream.IntStream;

public class FFTComponent extends Component {

    final static double START_FREQUENCY = 80;
    final static double END_FREQUENCY = 16000;
    final static float MIN_DB = -60;
    final static double DECAY_PER_FRAME = 10 / 30.0;
    final static int[] displayFrequency = new int[]{100, 225, 500, 1000, 2250, 5000, 10000};
    final static int BORDER_DISTANCE = 32;
    final static float WIDTH = 2.5f;

    AudioTrackComponent audioTrack;
    double[] wetAmount = new double[150];
    double[] dryAmount = new double[wetAmount.length];
    final double factor = Math.pow(END_FREQUENCY / START_FREQUENCY, 1.0 / (wetAmount.length - 1));
    final double difference = (END_FREQUENCY - START_FREQUENCY) / wetAmount.length;

    float boxHeight;

    public FFTComponent(BindableVector position, BindableVector size, AudioTrackComponent audioTrack) {
        super(position, size);
        this.audioTrack = audioTrack;
        Arrays.fill(wetAmount, MIN_DB);
        Arrays.fill(dryAmount, MIN_DB);
    }

    @Override
    protected void drawComponent(PGraphics g) {
        // BackBackground
        g.fill(Main.COLOR_MIDDLEGROUND);
        g.rect(0, 0, size.getX(), size.getY());


        // Background
        boxHeight = size.getY() * 0.825f;
        g.fill(Main.COLOR_BACKGROUND);
        g.rect(BORDER_DISTANCE, BORDER_DISTANCE, size.getX() - 2 * BORDER_DISTANCE, boxHeight - BORDER_DISTANCE);

        // Lines
        g.strokeWeight(2f);
        g.textFont(AJFrame.FONT_SMALL);
        g.stroke(Main.COLOR_MIDDLEGROUND & 0x60ffffff);
        g.fill(Main.COLOR_ELEMENTS);
        for (int i = 0; i < displayFrequency.length; i++) {
            float x = xOfFreq(displayFrequency[i]);
            g.line(x, BORDER_DISTANCE, x, boxHeight);
        }
        for (int i = 0; i < 60; i += 20) {
            float y = yOfDBFS(-i);
            g.line(BORDER_DISTANCE, y, size.getX() - BORDER_DISTANCE, y);
            g.text("-" + i + " dBFS", size.getX() - 75, y + 20);
        }



        IntStream.range(0, wetAmount.length * 2).parallel().forEach(i -> {
            double[] setArray = i < wetAmount.length ? dryAmount : wetAmount;
            int index = i % wetAmount.length;
            if (audioTrack.playing && audioTrack.thread != null) {
                double frequency = freq(index);
                double fftResult = MIN_DB - 1;
                Audible audible = i < wetAmount.length ? audioTrack.thread.playingAudibleClean : audioTrack.thread.playingAudibleWet;

                if (audible != null)
                    fftResult = audible.fade(100 / audioTrack.format.getSampleRate()).getFFTdBFS(frequency);
                setArray[index] = Math.max(fftResult, setArray[index] - DECAY_PER_FRAME);

            } else {
                setArray[index] = setArray[index] - DECAY_PER_FRAME;
            }
        });

        boolean isRunning = audioTrack.playing && audioTrack.thread != null;

        g.strokeWeight(WIDTH);
        g.stroke(Main.COLOR_ELEMENTS);

        isRunning = drawFFT(g, dryAmount, isRunning);

        g.stroke(Main.COLOR_PRIMARY);

        isRunning = drawFFT(g, wetAmount, isRunning);

        if (isRunning) drawOnNextCall();

        g.fill(Main.COLOR_TEXT);
        g.textFont(AJFrame.FONT_TEXT);

        for (int i = 0; i < displayFrequency.length; i++) {
            g.text(freqToString(displayFrequency[i]), xOfFreq(displayFrequency[i]), size.getY() * 0.93f);
        }
    }

    private boolean drawFFT(PGraphics g, double[] amount, boolean isRunning) {
        for (int i = 1; i < amount.length; i++) {
            if (amount[i - 1] > MIN_DB || amount[i] > MIN_DB) {
                float x1 = xOfFreq(freq(i - 1));
                float x2 = xOfFreq(freq(i));
                float y1 = yOfDBFS(amount[i-1]);
                float y2 = yOfDBFS(amount[i]);
                g.line(x1, y1, x2, y2);
                isRunning = true;
            }
        }
        return isRunning;
    }

    double freq(int i) {
        // Fade between logarithmic FFT-scaling and linear scaling
        return 0.15 * (START_FREQUENCY + i * difference) + 0.85 * 80 * Math.pow(16000d/80, (double) i / wetAmount.length);
    }

    float xOfFreq(double freq) {
        return (float) (Math.log(freq / START_FREQUENCY) / Math.log(END_FREQUENCY / START_FREQUENCY)) * (size.getX() - WIDTH - 2 * BORDER_DISTANCE) + WIDTH/2 + BORDER_DISTANCE;
    }


    float yOfDBFS(double dBFS) {
        return (float) Math.max(dBFS, MIN_DB) / MIN_DB * (boxHeight - WIDTH - BORDER_DISTANCE) + WIDTH/2 + BORDER_DISTANCE;
    }

    String freqToString(int freq) {
        if (freq < 1000) {
            return freq + " Hz";
        } else {
            if (freq / 1000 == freq / 1000.0) {
                return (freq / 1000) + " kHz";
            } else {
                return String.format("%.1f kHz", freq * 0.001);
            }
        }
    }

}
