package podCarsten.frontend;

import podCarsten.backend.Audible;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import java.util.function.UnaryOperator;

public class AudioPlayerThread extends Thread {

    SourceDataLine line;
    Audible audible;
    Audible playingAudibleClean;
    Audible playingAudibleWet;
    AudioFormat format;
    boolean playing = false;
    int samplePosition;
    int sampleLength;

    public AudioPlayerThread(SourceDataLine line, Audible audible, int sampleLength, AudioFormat format) {
        this.line = line;
        this.audible = audible;
        this.format = format;
        this.sampleLength = sampleLength;
    }

    public boolean play(int startSample) {
        if (startSample + AudioTrackComponent.BUFFER_SIZE < sampleLength)
        samplePosition = startSample;
        playing = true;
        start();

        return true;
    }

    // Returns position to stop
    public int pause() {
        playing = false;
        return samplePosition + Math.min(AudioTrackComponent.BUFFER_SIZE, (sampleLength - samplePosition));
    }

    @Override
    public void run() {
        super.run();

        // Only run if not too close to the end
        if (samplePosition + AudioTrackComponent.BUFFER_SIZE < sampleLength) {

            processBuffer(AudioTrackComponent.BUFFER_SIZE, a -> a.fadeIn((AudioTrackComponent.BUFFER_SIZE - 1) / format.getSampleRate()));
            line.start();

            while (playing && samplePosition + AudioTrackComponent.BUFFER_SIZE < sampleLength) {
                processBuffer(AudioTrackComponent.BUFFER_SIZE, a -> a);
            }

            if (sampleLength - samplePosition > 0)
                processBuffer(Math.min(AudioTrackComponent.BUFFER_SIZE, (sampleLength - samplePosition)),
                        a -> a.fadeOut((Math.min(AudioTrackComponent.BUFFER_SIZE, (sampleLength - samplePosition)) - 1) / format.getSampleRate()));

            line.stop();
            playingAudibleClean = null;
            playing = false;
        }
    }

    private void processBuffer(int bufferSize, UnaryOperator<Audible> customization) {
        playingAudibleClean = audible.splice(samplePosition, bufferSize, playingAudibleWet);
        playingAudibleWet = playingAudibleClean.processEffects();

        byte[] bytes = customization.apply(playingAudibleWet).getOutput(format);
        line.write(bytes, 0, bufferSize * format.getFrameSize());

        samplePosition += bufferSize;
    }


}
