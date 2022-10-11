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
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AudioProcessorTest {
    private static final Logger LOGGER = LogManager.getLogger("monitoring");
    static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    void setEffects() {
        LOGGER.info("Starte den Test für setEffects()...\n");

        AudioProcessor audioProcessor = new AudioProcessor(new int[3][], 44100, new AudioEffect[0][]);

        AudioEffect exampleEffect1 = AudioEffect.bell(44100, 200, 1, -9);
        AudioEffect exampleEffect2 = AudioEffect.rmsCompressor(3, -20, 0.01, 0.06, 44100);
        AudioEffect[] exampleEffectArray = new AudioEffect[]{exampleEffect1, exampleEffect2};

        audioProcessor = (AudioProcessor) audioProcessor.setEffects(exampleEffectArray);

        AudioEffect[][] testAudioEffects = audioProcessor.audioEffects;

        assertAll(
                // Assert correct length
                () -> {
                    assertEquals(3, testAudioEffects.length);
                    LOGGER.info("Das AudioEffects-Array hat die richtige Länge.");
                },

                // Assert copies
                () -> {
                    for (AudioEffect[] audioEffects : testAudioEffects) {
                        assertArrayEquals(exampleEffectArray, audioEffects);
                    }
                    LOGGER.info("Die inneren Arrays wurden richtig kopiert.");
                }
        );

        LOGGER.info("Die Methode setEffects() funktioniert!\n\n\n");
    }

    @Test
    void setInput() {
        LOGGER.info("Starte den Test für setInput()...\n");

        assertAll(
                // Test BigEndian
                () -> {
                    AudioFormat testFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 24, 2, 6, 48000,false);
                    byte[] bytes = new byte[]{ 0x0C, 0x0B, 0x0A, 0x33, 0x22, 0x11, 0x0F, 0x0E, 0x0D, 0x66, 0x55, 0x44};
                    int[][] expected = Arrays.stream(
                                    new int[][]{new int[]{ 0x0A0B0C00, 0x0D0E0F00 }, new int[]{ 0x11223300, 0x44556600}})
                            .map(array -> Arrays.stream(array).map(n -> n / AudioProcessor.HEADROOM_DIVISOR).toArray())
                            .toArray(int[][]::new);

                    AudioProcessor testProcessor = (AudioProcessor) Audible.of(bytes, testFormat);

                    assertEquals(testFormat.getSampleRate(), testProcessor.sampleRate, "Die Samplerate wurde nicht korrekt übernommen!");
                    assertTrue(IntStream.range(0, expected.length).allMatch(i -> {
                        if (Arrays.equals(expected[i], testProcessor.channelSamples[i]))
                            return true;
                        LOGGER.info("Vergleich fehlgeschlagen! Erwartet: " + toHexString(expected[i]) + " | Tatsächlicher Wert: " + toHexString(testProcessor.channelSamples[i]));
                        return false;
                    }));
                    LOGGER.info("Test 1 mit folgenden Werten erfolgreich: Kanäle: {}; Samplegröße in Bits: {}; isBigEndian: {}", testFormat.getChannels(), testFormat.getSampleSizeInBits(), testFormat.isBigEndian());

                },

                // Test SmallEndian
                () -> {
                    AudioFormat testFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100,true);
                    byte[] bytes = new byte[]{ 0x12, 0x34, 0x76, 0x54 };
                    int[][] expected = Arrays.stream(
                                    new int[][]{new int[]{ 0x1234_0000, 0x7654_0000 }})
                            .map(array -> Arrays.stream(array).map(n -> n / AudioProcessor.HEADROOM_DIVISOR).toArray())
                            .toArray(int[][]::new);

                    AudioProcessor testProcessor = (AudioProcessor) Audible.of(bytes, testFormat);

                    assertEquals(testFormat.getSampleRate(), testProcessor.sampleRate, "Die Samplerate wurde nicht korrekt übernommen!");
                    assertTrue(IntStream.range(0, expected.length).allMatch(i -> {
                        if (Arrays.equals(expected[i], testProcessor.channelSamples[i]))
                            return true;
                        LOGGER.info("Vergleich fehlgeschlagen! Erwartet: " + toHexString(expected[i]) + " | Tatsächlicher Wert: " + toHexString(testProcessor.channelSamples[i]));
                        return false;
                    }));
                    LOGGER.info("Test 2 mit folgenden Werten erfolgreich: Kanäle: {}; Samplegröße in Bits: {}; isBigEndian: {}", testFormat.getChannels(), testFormat.getSampleSizeInBits(), testFormat.isBigEndian());

                }
        );

        LOGGER.info("Die Methode setInput() funktioniert!\n\n\n");
    }

    String toHexString(int[] array) {
        return "[" + String.join(", ", Arrays.stream(array).mapToObj(Integer::toHexString).toArray(String[]::new)) + "]";
    }



    /**
     * Dieser Test ist ein Test direkt gegen die Klasse, da direkt auf die einzelnen Samples zugegriffen wird
     */
    @Test
    void reverse() {
        AudioProcessor testProcessor = getTestProcessor();
        LOGGER.info("Teste reverse()...");

        // Überprüfe, ob zweimaliges reversen die genau die gleichen Samples zurückgibt
        int[][] originalSamples = testProcessor.channelSamples;
        int[][] testSamples = ((AudioProcessor) testProcessor.reverse().reverse()).channelSamples;
        assertTrue(IntStream.range(0, originalSamples.length).allMatch(i -> Arrays.equals(originalSamples[i], testSamples[i])));
        LOGGER.info("Zweimaliges reversen erfolgreich!");

        // Referenzimplementierung
        int[][] test2Samples = ((AudioProcessor) testProcessor.reverse()).channelSamples;
        int[][] reference = Arrays.stream(originalSamples).map(samples -> IntStream.iterate(samples.length - 1, i -> i - 1).limit(samples.length).map(i -> samples[i]).toArray()).toArray(int[][]::new);
        assertTrue(IntStream.range(0, originalSamples.length).allMatch(i -> Arrays.equals(test2Samples[i], reference[i])));
        LOGGER.info("Referenzimplementierung erfolgreich!");
    }

    @Test
    void fadeIn() {
        AudioProcessor testProcessor = getTestProcessor();
        LOGGER.info("Teste fadeIn()...");

        int fadeTestSeconds = 2;

        // Überprüfe, ob das Testsignal zu gewünschtem Zeitraum immer lauter wird (relativ zu einem Signal ohne fade)
        int[][] originalSamples = testProcessor.channelSamples;
        int[][] testSamples = ((AudioProcessor) testProcessor.fadeIn(fadeTestSeconds)).channelSamples;
        float sampleRate = testProcessor.sampleRate;

        // Errechne Zwischenwerte
        int lastAffectedSample = Math.min((int) (fadeTestSeconds * sampleRate - 1), originalSamples[0].length);
        double maxFactor = 1 - AudioProcessor.NEEDED_FACTOR;

        // Überprüfe, ob der Lautstärkefaktor immer größer ist, als beim Sample davor (immer lauter werdend)
        // und zur gewünschter Zeit leiser, als das Original ist.
        for (int i = 0; i < originalSamples.length; i++) {
            double lastFactor = 0;
            for (int j = 0; j < lastAffectedSample; j++) {
                if (originalSamples[i][j] != 0) {
                    double factor = (double) testSamples[i][j] / originalSamples[i][j];

                    assertTrue(factor <= maxFactor, "factor <= maxFactor ist nicht erfüllt! | " + factor + " <= " + maxFactor);
                    assertTrue(lastFactor <= factor + DOUBLE_TOLERANCE, "lastFactor <= factor ist nicht erfüllt! | " + lastFactor + " <= " + factor);
                    lastFactor = factor;
                }

            }
            LOGGER.info("Bei Kanal " + (i + 1) + " wird das Signal korrekt lauter!");

            // Teste, ob Signal nach dem einfaden laut genug ist (Zeit genau getroffen)
            for (int j = lastAffectedSample + 1; j < originalSamples[i].length; j++) {
                if (originalSamples[i][j] != 0) {
                    double factor = (double) testSamples[i][j] / originalSamples[i][j];

                    assertTrue(factor >= maxFactor, "factor >= maxFactor ist nicht erfüllt! | " + factor + " <= " + maxFactor);
                }
            }
            LOGGER.info("Bei Kanal " + (i + 1) + " erreicht das Signal zum richtigen Zeitpunkt die volle Lautstärke!");
        }
        LOGGER.info("fadeIn() funktioniert korrekt!");

    }

    @Test
    void getOutput() {
        LOGGER.info("Starte den Test für getOutput()...\n");

        assertAll(
                // Test BigEndian
                () -> {
                    AudioFormat testFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 24, 2, 6, 48000,false);
                    byte[] expected = new byte[]{ 0x0C, 0x0B, 0x0A, 0x33, 0x22, 0x11, 0x0F, 0x0E, 0x0D, 0x66, 0x55, 0x44};
                    int[][] channelSamples = Arrays.stream(
                                    new int[][]{new int[]{ 0x0A0B0C00, 0x0D0E0F00 }, new int[]{ 0x11223300, 0x44556600}})
                            .map(array -> Arrays.stream(array).map(n -> n / AudioProcessor.HEADROOM_DIVISOR).toArray())
                            .toArray(int[][]::new);

                    AudioProcessor testProcessor = new AudioProcessor(channelSamples, testFormat.getSampleRate(), new AudioEffect[0][]);
                    byte[] output = testProcessor.getOutput(testFormat);

                    assertArrayEquals(expected, output, "Vergleich fehlgeschlagen! Erwartet: " + Arrays.toString(expected) + " | Tatsächlicher Wert: " + Arrays.toString(output));

                    LOGGER.info("Test 1 mit folgenden Werten erfolgreich: Kanäle: {}; Samplegröße in Bits: {}; isBigEndian: {}", testFormat.getChannels(), testFormat.getSampleSizeInBits(), testFormat.isBigEndian());

                },

                // Test SmallEndian
                () -> {
                    AudioFormat testFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100,true);
                    byte[] expected = new byte[]{ 0x12, 0x34, 0x76, 0x54 };
                    int[][] channelSamples = Arrays.stream(
                                    new int[][]{new int[]{ 0x1234_0000, 0x7654_0000 }})
                            .map(array -> Arrays.stream(array).map(n -> n / AudioProcessor.HEADROOM_DIVISOR).toArray())
                            .toArray(int[][]::new);

                    AudioProcessor testProcessor = new AudioProcessor(channelSamples, testFormat.getSampleRate(), new AudioEffect[0][]);
                    byte[] output = testProcessor.getOutput(testFormat);

                    assertArrayEquals(expected, output, "Vergleich fehlgeschlagen! Erwartet: " + Arrays.toString(expected) + " | Tatsächlicher Wert: " + Arrays.toString(output));

                    LOGGER.info("Test 2 mit folgenden Werten erfolgreich: Kanäle: {}; Samplegröße in Bits: {}; isBigEndian: {}", testFormat.getChannels(), testFormat.getSampleSizeInBits(), testFormat.isBigEndian());

                }
        );

        LOGGER.info("Die Methode getOutput() funktioniert!\n\n\n");
    }

    AudioProcessor getTestProcessor() {
        File file = new File("testSine1kHz-1dB.wav");
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