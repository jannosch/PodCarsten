package podCarsten.frontend;

import aj.frame.AJFrame;
import aj.frame.components.*;
import aj.vector.BindableVector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import podCarsten.backend.Audible;
import podCarsten.backend.AudioEffect;
import processing.core.*;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.UnaryOperator;

public class Main {

    final static int COLOR_PRIMARY = 0xFF1BB9BB;
    final static int COLOR_PRIMARY_DARK = 0xFF235155;
    final static int COLOR_YELLOW = 0xFFF6A231;
    final static int COLOR_YELLOW_DARK = 0xFF553C23;
    final static int COLOR_RED = 0xFFFF4D5B;
    final static int COLOR_RED_DARK = 0xFF552325;
    final static int COLOR_GREEN = 0xFF2FB961;
    final static int COLOR_BACKGROUND = 0xFF14151A;
    final static int COLOR_MIDDLEGROUND = 0xFF333A42;
    final static int COLOR_ELEMENTS = 0xFF536270;
    final static int COLOR_TEXT = 0xFFE6EBF0;

    final static String TEXT_LOAD_FILE = "load file";
    final static String TEXT_SAVE_FILE = "save file";

    final static String TEXT_TITLE = "current file:";
    final static String TEXT_NO_FILE = "select 'load file'";
    final static String TEXT_FILE_NOT_SUPPORTED = "the file-format is not supported!";
    final static String[] TEXT_MUSIC = new String[]{"doesn't contain \nmusic", "contains \nmusic"};
    final static String[] TEXT_VOICE = new String[]{"male voices \nonly", "different \nregisters", "female voices \nonly"};
    final static String[] TEXT_BYPASS = new String[]{"effects bypassed", "effects active"};
    final static String TEXT_LIGHTNESS = "lightness";
    final static String TEXT_TONE = "tone";
    final static String TEXT_CLARITY = "clarity";
    final static String TEXT_PROXIMITY = "treble";
    final static String TEXT_LEVELING = "leveling";

    final static String ICON_START = "src/img/icon_start.svg";
    final static String ICON_REWIND = "src/img/icon_rewind.svg";
    final static String ICON_PLAY = "src/img/icon_play.svg";
    final static String ICON_PAUSE = "src/img/icon_pause.svg";
    final static String ICON_SKIP = "src/img/icon_skip.svg";
    final static String ICON_END = "src/img/icon_end.svg";
    final static String ICON_LIGHTNESS = "src/img/icon_lightness.svg";
    final static String ICON_TONE = "src/img/icon_tone.svg";
    final static String ICON_CLARITY = "src/img/icon_clarity.svg";
    final static String ICON_PROXIMITY = "src/img/icon_proximity.svg";
    final static String ICON_LEVELING = "src/img/icon_leveling.svg";

    static PShape SHAPE_PLAY;
    static PShape SHAPE_PAUSE;


    public static final Logger LOGGER = LogManager.getLogger("monitoring");


    File audioFile = null;
    byte[] sampleBytes = null;
    AudioFormat format = null;
    Audible rootAudible;

    AJFrame frame;

    AudioTrackComponent trackComponent;
    Switch switchMusic;
    Switch switchVoice;
    Switch switchBypass;

    Holder barBottom;
    Slider sliderLightness;
    Slider sliderTone;
    Slider sliderClarity;
    Slider sliderProximity;
    Slider sliderLeveling;
    UnaryOperator<Float> mapper = (amount) -> 0.75f * (float) Math.pow(amount, 3) + 0.25f * amount;

    long showNotSupportedUntil;

    public Main() {
        frame = new AJFrame(1680, 915);
        SHAPE_PLAY = frame.loadShape(ICON_PLAY);
        SHAPE_PAUSE = frame.loadShape(ICON_PAUSE);

        VerticalBar barMain = new VerticalBar(frame.getHolder());

        Holder barTop = (Holder) new Holder(barMain) {
            @Override
            protected void drawComponent(PGraphics g) {
                g.fill(COLOR_MIDDLEGROUND);
                g.rect(0, 0, size.getX(), size.getY());
            }
        }.setHeight(frame.getSize(), 0.1f);

        Button buttonLoadFile = (Button) new Button(
                new BindableVector().bind(barTop.getSize(), 0.03f, 0.15f),
                new BindableVector().bind(barTop.getSize(), 0.13f, 0.7f),
                TEXT_LOAD_FILE,
                COLOR_ELEMENTS,
                COLOR_TEXT,
                16
        ).addListener(() -> frame.selectInputFile(this::loadFile)).addTo(barTop);

        Button buttonSaveFile = (Button) new Button(
                new BindableVector().bind(barTop.getSize(), 0.82f, 0.15f),
                new BindableVector().bind(barTop.getSize(), 0.13f, 0.7f),
                TEXT_SAVE_FILE,
                COLOR_ELEMENTS,
                COLOR_TEXT,
                16
        ).addListener(() -> frame.selectOutputFile(this::saveFile)).addTo(barTop);

        Button buttonStart = (Button) new Button(
                new BindableVector(-32 - 128 - 10, -32).bind(barTop.getSize(), 0.5f, 0.5f),
                new BindableVector(64, 64),
                frame.loadShape(ICON_START),
                COLOR_ELEMENTS,
                COLOR_TEXT,
                new float[]{16, 0, 0, 16}
        ).addTo(barTop);

        Button buttonRewind = (Button) new Button(
                new BindableVector(-32 - 64 - 5, -32).bind(barTop.getSize(), 0.5f, 0.5f),
                new BindableVector(64, 64),
                frame.loadShape(ICON_REWIND),
                COLOR_ELEMENTS,
                COLOR_TEXT,
                0
        ).addTo(barTop);

        Button buttonPlay = (Button) new Button(
                new BindableVector(-32, -32).bind(barTop.getSize(), 0.5f, 0.5f),
                new BindableVector(64, 64),
                frame.loadShape(ICON_PLAY),
                COLOR_ELEMENTS,
                COLOR_TEXT,
                0
        ).addTo(barTop);

        Button buttonSkip = (Button) new Button(
                new BindableVector(-32 + 64 + 5, -32).bind(barTop.getSize(), 0.5f, 0.5f),
                new BindableVector(64, 64),
                frame.loadShape(ICON_SKIP),
                COLOR_ELEMENTS,
                COLOR_TEXT,
                0
        ).addTo(barTop);

        Button buttonEnd = (Button) new Button(
                new BindableVector(-32 + 128 + 10, -32).bind(barTop.getSize(), 0.5f, 0.5f),
                new BindableVector(64, 64),
                frame.loadShape(ICON_END),
                COLOR_ELEMENTS,
                COLOR_TEXT,
                new float[]{0, 16, 16, 0}
        ).addTo(barTop);




        // AudioTrack mit Informationen zum Audiotrack; Verwaltet die Position des Cursors
        trackComponent = (AudioTrackComponent) new AudioTrackComponent(
                new BindableVector(),
                new BindableVector(),
                buttonPlay
        ).setHeight(frame.getSize(), 0.4f).addTo(barMain);
        buttonPlay.addListener(trackComponent::togglePlay);

        buttonStart.addListener(() -> trackComponent.jumpAmount(-1e5));
        buttonEnd.addListener(() -> trackComponent.jumpAmount(1e5));
        buttonRewind.addListener(() -> trackComponent.jumpAmount(-30));
        buttonSkip.addListener(() -> trackComponent.jumpAmount(30));



        barBottom = (Holder) new Holder(barMain) {
            @Override
            protected void drawComponent(PGraphics g) {
                g.fill(COLOR_MIDDLEGROUND);
                g.rect(0, 0, size.getX(), size.getY());

                g.fill(COLOR_TEXT);
                g.textAlign(PConstants.LEFT);
                g.textFont(AJFrame.FONT_H1_THICC);

                if (audioFile != null) {
                    g.text(TEXT_TITLE, 0.03f * size.getX(), 0.148f * size.getY());
                    g.textFont(AJFrame.FONT_H1);
                    g.fill(COLOR_PRIMARY);
                    g.text(audioFile.getName(), 0.03f * size.getX() + 210, 0.148f * size.getY());
                } else {
                    if (showNotSupportedUntil > System.currentTimeMillis()) {
                        g.text(TEXT_FILE_NOT_SUPPORTED, 0.03f * size.getX(), 0.148f * size.getY());
                        drawOnNextCall();

                    } else g.text(TEXT_NO_FILE, 0.03f * size.getX(), 0.148f * size.getY());
                }

                g.textFont(AJFrame.FONT_TEXT);
                g.textLeading(30);
            }
        }.setHeight(frame.getSize(), 0.5f);

        switchMusic = (Switch) new Switch(
                new BindableVector().bind(barBottom.getSize(), 0.02f, 0.3f),
                new BindableVector().bind(barBottom.getSize(), 0.075f, 0.15f),
                new int[]{COLOR_ELEMENTS, COLOR_PRIMARY},
                new int[]{COLOR_BACKGROUND, COLOR_PRIMARY_DARK}
        ).addDrawer((g, state) -> {
            g.fill(COLOR_TEXT);
            g.textAlign(PConstants.LEFT);
            g.text(TEXT_MUSIC[state], 130,  30);
        }).addListener(state -> setEffects()).addTo(barBottom);

        switchVoice = (Switch) new Switch(
                new BindableVector().bind(barBottom.getSize(), 0.02f, 0.525f),
                new BindableVector().bind(barBottom.getSize(), 0.075f, 0.15f),
                new int[] { COLOR_YELLOW, COLOR_ELEMENTS, COLOR_RED },
                new int[] { COLOR_YELLOW_DARK, COLOR_BACKGROUND, COLOR_RED_DARK }
        ).addDrawer((g, state) -> {
            g.fill(COLOR_TEXT);
            g.textAlign(PConstants.LEFT);
            g.text(TEXT_VOICE[state], 130,  30);
        }).setState(1).addListener(state -> setEffects()).addTo(barBottom);

        switchBypass = (Switch) new Switch(
                new BindableVector().bind(barBottom.getSize(), 0.02f, 0.75f),
                new BindableVector().bind(barBottom.getSize(), 0.075f, 0.15f),
                new int[]{COLOR_ELEMENTS, COLOR_PRIMARY},
                new int[]{COLOR_BACKGROUND, COLOR_PRIMARY_DARK}
        ).addDrawer((g, state) -> {
            g.fill(COLOR_TEXT);
            g.textAlign(PConstants.LEFT);
            g.text(TEXT_BYPASS[state], 130,  44);
        }).setState(1).addListener(state -> setEffects()).addTo(barBottom);


        float sliderDistance = 0.06f;
        sliderLightness = (Slider) new Slider(
                new BindableVector().bind(barBottom.getSize(), 0.25f, 0.25f),
                new BindableVector().bind(barBottom.getSize(), sliderDistance, 0.7f),
                TEXT_LIGHTNESS,
                COLOR_MIDDLEGROUND,
                COLOR_RED,
                true,
                mapper,
                frame.loadShape(ICON_LIGHTNESS)
        ).addListener(this::setLightness).addTo(barBottom);

        sliderTone = (Slider) new Slider(
                new BindableVector().bind(barBottom.getSize(), sliderDistance, 0).bind(sliderLightness.getPosition()),
                new BindableVector().bind(sliderLightness.getSize()),
                TEXT_TONE,
                COLOR_MIDDLEGROUND,
                COLOR_YELLOW,
                true,
                mapper,
                frame.loadShape(ICON_TONE)
        ).addListener(this::setTone).addTo(barBottom);

        sliderClarity = (Slider) new Slider(
                new BindableVector().bind(barBottom.getSize(), sliderDistance, 0).bind(sliderTone.getPosition()),
                new BindableVector().bind(sliderLightness.getSize()),
                TEXT_CLARITY,
                COLOR_MIDDLEGROUND,
                COLOR_PRIMARY,
                true,
                mapper,
                frame.loadShape(ICON_CLARITY)
        ).addListener(this::setClarity).addTo(barBottom);

        sliderProximity = (Slider) new Slider(
                new BindableVector().bind(barBottom.getSize(), sliderDistance, 0).bind(sliderClarity.getPosition()),
                new BindableVector().bind(sliderLightness.getSize()),
                TEXT_PROXIMITY,
                COLOR_MIDDLEGROUND,
                COLOR_GREEN,
                true,
                mapper,
                frame.loadShape(ICON_PROXIMITY)
        ).addListener(this::setProximity).addTo(barBottom);

        sliderLeveling = (Slider) new Slider(
                new BindableVector().bind(barBottom.getSize(), sliderDistance, 0).bind(sliderProximity.getPosition()),
                new BindableVector().bind(sliderLightness.getSize()),
                TEXT_LEVELING,
                COLOR_MIDDLEGROUND,
                COLOR_RED,
                false,
                (amount) -> 0.5f * (amount + 1),
                frame.loadShape(ICON_LEVELING)
        ).addListener(this::setLeveling).addTo(barBottom);


        FFTComponent fftComponent = (FFTComponent) new FFTComponent(
                new BindableVector().bind(barBottom.getSize(), 0.56f, 0),
                new BindableVector().bind(barBottom.getSize(), 1-0.56f, 1),
                trackComponent
        ).addTo(barBottom);
        buttonPlay.addListener(fftComponent::drawOnNextCall);
    }

    private void loadFile(File file) {
        trackComponent.stop();

        if (!file.getName().endsWith(".wav") && !file.getName().endsWith(".aiff")) {
            LOGGER.info("Die Datei {} wird nicht unterstützt!", file.getName());
            showNotSupportedUntil = System.currentTimeMillis() + 1000 * 7;
            barBottom.drawOnNextCall();
            audioFile = null;
            rootAudible = null;
            trackComponent.audible = null;
            trackComponent.amounts = new float[0];
            trackComponent.drawOnNextCall();
            return;
        }

        this.audioFile = file;

        // Load File
        try {
            if (AudioSystem.getAudioFileFormat(file) != null) {
                trackComponent.amounts = new float[0];
                trackComponent.drawOnNextCall();
                barBottom.drawOnNextCall();
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
                sampleBytes = audioStream.readAllBytes();
                format = audioStream.getFormat();
                trackComponent.samplePosition = 0;

                // Create first instance of Audible Interface
                rootAudible = Audible.of(sampleBytes, format);
                trackComponent.loadAudible(rootAudible, sampleBytes.length / (format.getSampleSizeInBits() / 8.0) / format.getChannels() / format.getSampleRate(), format.getFrameRate(), format);

                setEffects();
            }

        } catch (UnsupportedAudioFileException | IOException e) {
            LOGGER.info("Die Datei {} wird nicht unterstützt!", file.getName());
            showNotSupportedUntil = System.currentTimeMillis() + 1000 * 7;
            barBottom.drawOnNextCall();
        }
    }

    private void saveFile(File file) {
        Audible audible = trackComponent.audible;

        if (audible != null) {
            if (!file.getName().endsWith(".wav") && !file.getName().endsWith(".aiff")) {
                if (format.isBigEndian()) {
                    LOGGER.info("Das Dateiformat '.aiff' wurde beim Speichern automatisch erkannt und die dementsprechende Dateiendung ergänzt.");
                    file = new File(file.getAbsolutePath() + ".aiff");
                } else {
                    LOGGER.info("Das Dateiformat '.wav' wurde beim Speichern automatisch erkannt und die dementsprechende Dateiendung ergänzt.");
                    file = new File(file.getAbsolutePath() + ".wav");
                }
            }


            byte[] bytes = audible.processEffects().fade(0.01).normalize().getOutput(format);


            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            AudioInputStream audioInputStream =  new AudioInputStream(inputStream, format, bytes.length / format.getFrameSize());


            // Writing output file
            try {
                file.delete();
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
                audioInputStream.close();
            } catch (IOException e) {

            }
        }
    }

    private void setEffects() {
        trackComponent.bypass(switchBypass.getState() == 0);
        if (!trackComponent.bypassed) {
            setHighPass();
            setLightness(sliderLightness.getAmount());
            setTone(sliderTone.getAmount());
            setClarity(sliderClarity.getAmount());
            setProximity(sliderProximity.getAmount());
            setLeveling(sliderLeveling.getAmount());
        }
    }

    private void setHighPass() {
        if (format != null) {
            double frequency;

            if (switchMusic.getState() == 1)
                frequency = 36;
            else if (switchVoice.getState() == 0)
                frequency = 90;
            else if (switchVoice.getState() == 1)
                frequency = 100;
            else
                frequency = 160;

            trackComponent.setEffect(0, AudioEffect.highPass(format.getSampleRate(), frequency, 0.71));
        }
    }

    private void setLightness(float amount) {
        if (format != null) {
            double frequency = 180;
            if (switchVoice.getState() == 0) {
                frequency = 150;
            } else if (switchVoice.getState() == 2) {
                frequency = 250;
            }

            trackComponent.setEffect(1, AudioEffect.bell(format.getSampleRate(), frequency, 2, -12 * amount));
        }
    }

    private void setTone(float amount) {
        if (format != null) {
            double lowFrequency = 500;
            if (switchVoice.getState() == 0) {
                lowFrequency = 450;
            } else if (switchVoice.getState() == 2) {
                lowFrequency = 550;
            }

            trackComponent.setEffect(2, AudioEffect.bell(format.getSampleRate(), lowFrequency, 2, -14 * amount));
            trackComponent.setEffect(3, AudioEffect.bell(format.getSampleRate(), 1800, 2, 12 * amount));
        }
    }

    private void setClarity(float amount) {
        if (format != null)
            trackComponent.setEffect(4, AudioEffect.bell(format.getSampleRate(), 3500, 2, 12 * amount));
    }

    private void setProximity(float amount) {
        if (format != null)
            trackComponent.setEffect(5, AudioEffect.bell(format.getSampleRate(), 8000, 1, 10 * amount));
    }

    private void setLeveling(float amount) {
        if (format != null)
            trackComponent.setEffect(6, AudioEffect.rmsCompressor(3 + 3 * amount * amount, (-6 - 30 * amount) * (1 - Math.pow(1e7, -amount)), 0.01, 0.6, format.getSampleRate()));
    }

    public static void main(String[] args) {
        new Main();
    }
}
