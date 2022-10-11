package podCarsten.backend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class AudibleTest {
    private static final Logger LOGGER = LogManager.getLogger("monitoring");

    @Test
    void dBToFactor() {
        LOGGER.info("Starte den Test für normalize()...\n");

        // Überprüfe die Formel
        assertEquals(1, Audible.dBToFactor(0));
        LOGGER.info("Für 0 korrekt gerechnet.");

        assertEquals(2, Audible.dBToFactor(6));
        assertEquals(4, Audible.dBToFactor(12));
        assertEquals(8, Audible.dBToFactor(18));
        assertEquals(16, Audible.dBToFactor(24));
        assertEquals(32, Audible.dBToFactor(30));
        LOGGER.info("Für positive Zahlen korrekt errechnet.");

        assertEquals(1.0 / 2, Audible.dBToFactor(-6));
        assertEquals(1.0 / 4, Audible.dBToFactor(-12));
        assertEquals(1.0 / 8, Audible.dBToFactor(-18));
        assertEquals(1.0 / 16, Audible.dBToFactor(-24));
        assertEquals(1.0 / 32, Audible.dBToFactor(-30));
        LOGGER.info("Für negative Zahlen korrekt errechnet.");

        LOGGER.info("Die Methode getPeakInDBFS() funktioniert!\n\n\n");
    }

    @Test
    void getPeakInDBFS() {
        LOGGER.info("Starte den Test für normalize()...\n");

        // Tests durch erwartete Ergebnisse
        assertEquals(-20, getTestAudible("testNeedle-20dB16Bits.wav").getPeakInDBFS(), 1e-1);
        assertEquals(-12, getTestAudible("testSweep-12dB16Bits.wav").getPeakInDBFS(), 1e-1);
        assertEquals(-1, getTestAudible("testSine1kHz-1dB.wav").getPeakInDBFS(), 1e-1);

        LOGGER.info("Die Methode getPeakInDBFS() funktioniert!\n\n\n");
    }

    @Test
    void normalize() {
        LOGGER.info("Starte den Test für normalize(). Nutze bereits getestete Methode getPeakInDBFS()...\n");

        // Tests durch erwartete Ergebnisse einer daraufhin aufgerufenen Methode
        Audible audible = getTestAudible("testNeedle-20dB16Bits.wav");
        assertTrue(audible.getPeakInDBFS() != -1);
        assertTrue(audible.normalize().getPeakInDBFS() != -1);

        LOGGER.info("Die Methode normalize() standardisiert die Lautstärke eines Audiosignals mit zuvor anderer Lautstärke!");
        LOGGER.info("Die Methode normalize() funktioniert!\n\n\n");
    }

    @Test
    void getFFTdBFS() {
        LOGGER.info("Starte den Test für getFFTdBFS()...\n");

        // Tests durch vorbereitetes Szenarien bei dem im Detail mehrere
        // Frequenzen auf einen bestimmten Zielwert überprüft werden
        Audible audible = getTestAudible("testSine1kHz-1dB.wav");
        int numOfTests = 64;
        double fromFrequency = 62.5;
        double toFrequency = 16000;
        IntStream.rangeClosed(0, numOfTests)
                .mapToDouble(i -> fromFrequency * Math.pow(toFrequency / fromFrequency, (double) i / numOfTests))
                .parallel()
                .forEach(freq -> {
                    double fftValue = audible.getFFTdBFS(freq);
                    if (freq > 995 && freq < 1005) {
                        assertTrue(fftValue > -2, freq + " Hz wurde mit UNTER -2 (" + fftValue + ") dBFS FEHLERHAFT gemessen");
                        LOGGER.info(freq + " Hz wurde mit über -2 ({}) dBFS korrekt gemessen", fftValue);
                    } else {
                        assertTrue(fftValue < -30, freq + " Hz wurde mit ÜBER -80 (" + fftValue + ") dBFS FEHLERHAFT gemessen");
                        // LOGGER.info(freq + " Hz wurde mit unter -80 ({}) dBFS korrekt gemessen", fftValue);
                    }
                });
        LOGGER.info("Die restlichen Frequenzen wurden mit unter -80 dBFS korrekt gemessen!");
        LOGGER.info("Die Methode getFFTdBFS() funktioniert!\n\n\n");
    }

    @Test
    void getEffects() {
        LOGGER.info("Starte den Test für getEffects(). Nutze bereits getestete Methode setEffects()...\n");

        Audible audible = getTestAudible("testSine1kHz-1dB.wav");

        AudioEffect exampleEffect1 = AudioEffect.bell(44100, 200, 1, -9);
        AudioEffect exampleEffect2 = AudioEffect.rmsCompressor(3, -20, 0.01, 0.06, 44100);
        AudioEffect[] exampleEffectArray = new AudioEffect[]{exampleEffect1, exampleEffect2};

        audible = audible.setEffects(exampleEffectArray);

        AudioEffect[][] testEffectArray = audible.getAudioEffects();

        assertAll(
                // Assert correct length
                () -> {
                    assertEquals(2, testEffectArray.length);
                    LOGGER.info("Das Array hat die richtige Länge.");
                },

                // Assert copies
                () -> {
                    for (AudioEffect[] audioEffects : testEffectArray) {
                        assertArrayEquals(exampleEffectArray, audioEffects);
                    }
                    LOGGER.info("Die inneren Arrays wurden richtig kopiert.");
                }
        );

        LOGGER.info("Die Methode getEffects() funktioniert!\n\n\n");
    }



    AudioProcessor getTestAudible(String filename) {
        File file = new File(filename);
        AudioInputStream audioStream = null;
        try {
            audioStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException ignored) {
            LOGGER.info("Die Testdatei konnte nicht gelesen werden");
            fail();
        }

        AudioFormat format = audioStream.getFormat();

        try {
            return (AudioProcessor) Audible.of(audioStream.readAllBytes(), format);
        } catch (IOException ignored) {
            LOGGER.info("Die Testdatei konnte nicht gelesen werden");
            fail();
        }
        return null;
    }
}

