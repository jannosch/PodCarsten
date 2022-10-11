package podCarsten;

import podCarsten.backend.*;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class LoggingSzenario {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
        // Loading the file
        File file = new File("obsolete.wav");
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioStream.getFormat();


        // Use Audible Interface
        Audible audible = Audible.of(audioStream.readAllBytes(), format)
                .setEffects(AudioEffect.highPass(format.getSampleRate(), 200, 0.71),
                        AudioEffect.bell(format.getSampleRate(), 1000, 0.5, -6),
                        AudioEffect.bell(format.getSampleRate(), 4000, 0.5, -3),
                        AudioEffect.rmsCompressor(3, -20, 0.1, 1.2, format.getSampleRate()))
                .processEffects();

        byte[] bytes = audible.getOutput(format);


        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        AudioInputStream audioInputStream =  new AudioInputStream(inputStream, format, bytes.length / format.getFrameSize());


        // Writing output file
        File outputFile = new File("obsolete_out.wav");
        outputFile.delete();
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
        audioInputStream.close();

    }
}