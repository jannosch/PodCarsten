package podCarsten.backend;

import org.apache.logging.log4j.*;
import processing.core.*;

import javax.sound.sampled.AudioFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.stream.*;

class AudioProcessor implements Audible {

    // Empfohlener höchster Ausschlag für Audiodateien um Verzerrungen durch Dateikonvertierungen zu vermeiden
    static final double RECOMMENDED_PEAK = -1;
    // Faktor der bei exponentiellen Änderungen erreicht sein muss, bis die Änderung als erfüllt gilt
    // z.B. bei fadeIn()
    static final double NEEDED_FACTOR = 0.05;
    // Puffergröße in dB zum Übersteuerungsschutz
    static final int HEADROOM_DIVISOR = (int) Audible.dBToFactor(6);

    // Statisches Logger-Objekt, dass von der AL verwendet wird
    public static final Logger LOGGER = LogManager.getLogger("monitoring");

    /**
     * Gibt einen Faktor für eine exponentielle Funktion zurück, die exakt zu einer bestimmten Zeit den Zielwert erreicht.
     * Die exponentielle Funktion 'timeFactor ^ samples' erreicht NEEDED_FACTOR nach (second * sampleRate) Samples
     * (also nach 'second' Sekunden). Nötig für die "weichen" Lautstärkeänderungen von fadeIn() und des RMS-Kompressors.
     * @param seconds Dauer, bis der Wert erreicht werden soll.
     * @param sampleRate SampleRate des Signals.
     */
    static double timeFactor(double seconds, float sampleRate) {
        return 1 - Math.pow(NEEDED_FACTOR, 1 / (seconds * sampleRate));
    }

    /**
     * Rechnet von int-Werten der Implementierung in standardisierte dBFS um.
     * Für die Begriffserklärung, siehe die Dokumentation.
     *                                  <br><br>
     * Beispielwerte:                   <br>
     * MAX_VALUE → 0 dBFS               <br>
     * 0.5 * MAX_VALUE → -6 dBFS        <br>
     * 0.25 * MAX_VALUE → -12 dBFS      <br>
     * ...
     * @param intValue Wert des Samples
     * @return Lautstärkelevel in dBFS
     */
    static double intTOdBFS(int intValue) {
        return Math.log(Math.abs(intValue) / (double) Integer.MAX_VALUE) / Math.log(Math.pow(2, 1.0 / 6.0));
    }

    /**
     * Rechnet standardisierte dBFS in int-Wert der Implementierung um.
     * Für die Begriffserklärung, siehe die Dokumentation.
     *                                  <br><br>
     * Beispielwerte:                   <br>
     * 0 dBFS → MAX_VALUE               <br>
     * -6 dBFS → 0.5 * MAX_VALUE        <br>
     * -12 dBFS → 0.25 * MAX_VALUE      <br>
     * ...
     * @param dBFSValue Lautstärke in dBFS
     * @return Auslenkung als int
     */
    static int dBFSToInt(double dBFSValue) {
        return (int) (Integer.MAX_VALUE * Audible.dBToFactor(dBFSValue));
    }


    private static IntUnaryOperator bigEndianBitshift() {
        return (byteNumber) -> (4 - 1 - byteNumber) * 8;
        // Ungekürzte Version mit dem Parameter bytesPerSample
        // return (byteNumber) -> (bytesPerSample - 1 - byteNumber + 4 - bytesPerSample) * 8;
    }


    private static IntUnaryOperator smallEndianBitshift(int bytesPerSample) {
        return (byteNumber) -> (4 - bytesPerSample + byteNumber) * 8;
    }


    /**
     * Samples of the audio file are stored in the int-Array channelSamples[][]
     * The samples are stored per channel, so the first dimension of the array
     * i.E. if there are 2 audio-channels, channelSamples.length would be 2.
     * The number of samples must be equal for every channel:
     * channelSamples[i] == channelSamples[j] is always true for existing i's and j's
     */
    final int[][] channelSamples;
    final float sampleRate;             // Stores the sampleRate
    final AudioEffect[][] audioEffects; // Chain of all audioEffects

    /**
     * Empty constructor is only used to get initial instance
     */
    AudioProcessor() {
        channelSamples = new int[0][0];
        sampleRate = Float.NaN;
        audioEffects = new AudioEffect[1][0];
    }

    /**
     * Package-Private Constructor, called to create new instance to return
     * @param channelSamples
     * @param sampleRate
     * @param audioEffects
     */
    AudioProcessor(int[][] channelSamples, float sampleRate, AudioEffect[][] audioEffects) {
        this.channelSamples = channelSamples;
        this.sampleRate = sampleRate;
        this.audioEffects = audioEffects;
    }

    /**
     * Set effect chain
     * @param audioEffects
     * @return new instance
     */

    public Audible setEffects(AudioEffect... audioEffects) {
        assert audioEffects != null : "Das AudioEffect-Array darf nicht null sein!";
        assert Arrays.stream(audioEffects).noneMatch(Objects::isNull) : "Einzelne Effektgeräte dürfen nicht null sein!";

        return new AudioProcessor(channelSamples, sampleRate,
                Stream.generate(() -> audioEffects).limit(channelSamples.length).toArray(AudioEffect[][]::new)
        );
    }


    /**
     * Lade neue Samples in bytes Kodiert. Die genaue Kodierung legt das AudioFormat-Objekt fest.
     * Mehr Informationen zu der Kodierung von Audiodateien sind in der README zu finden.
     * Bitte zuerst lesen.
     * @param bytes - Das array an Bytes in der das Audiosignal kodiert ist
     * @param format - Die Kodierung des Audiosignals
     * @return eine neue Audible-Instanz, die die den neu geladenen Buffer dekodiert enthält
     */

    public Audible setInput(byte[] bytes, AudioFormat format) {
        assert bytes != null && bytes.length > 0 : "Die eingelesenen Bytes dürfen nicht leer sein!";
        assertFormat(format);
        assert bytes.length % format.getFrameSize() == 0 : "Jeder Channel muss gleich viele Samples pro Frame besitzen!";

        // Hilfreiche Variablen
        int bytesPerFrame = format.getFrameSize();
        int bytesPerSample = format.getSampleSizeInBits() / 8;

        LOGGER.info("Lade Input mit der Länge von " + new DecimalFormat().format(bytes.length) + " bytes und einer Framerate von " + format.getFrameRate() + " Hz");

        // Array in das alle Samples gespeichert werden
        int[][] inputSamples = new int[format.getChannels()][bytes.length / bytesPerFrame];

        LOGGER.info("Das Signal wird in {} Kanälen mit einer Länge von {} Samples gespeichert", inputSamples.length, inputSamples[0].length);

        // Ein UnaryOperator wird abhängig von der Kodierung mit der Information aktuellen bytesPerSample gespeichert,
        // der beim Herauslesen der Bytes die Anzahl an zu verschiebenen bits liefert.
        IntUnaryOperator bitShifter = format.isBigEndian() ? bigEndianBitshift() : smallEndianBitshift(bytesPerSample);

        // Hier wird das Audiosignal dekodiert und in einfach verarbeitbarer Form abgespeichert.
        // Der Stream ähnelt dabei mehr einer for-Schleife, statt einer klassischen Anwendung
        // von Streams, da es keinen Rückgabewert gibt und in ein außerhalb deklariertes Array befüllt.
        // Diese Implementierung ist notwendig, da gleichzeitig Positionen in zwei verschiedenen
        // Arrays bekannt sein müssen. Ich den folgenden Codeblock trotzdem als Stream implementiert,
        // um Parallelität nutzen zu können, die das Laden eines Signals um grob 30 % beschleunigt.
        IntStream.range(0, inputSamples.length * inputSamples[0].length).parallel().forEach(i -> {
            // Errechne die Positionen in den beiden Arrays
            int channel = i / inputSamples[0].length;
            int sample = i % inputSamples[0].length;
            int byteIndex = channel * bytesPerSample + sample * bytesPerFrame;

            for (int byteNr = 0; byteNr < bytesPerSample; byteNr++) {

                /*
                Verfahren in dieser Zeile

                 1. Errechnet die aktuelle Position des zu lesenden Bytes und liest dieses.
                 2. Entfernt unnötige Bits, die durch das automatische eingefügte Vorzeichen entstanden sind
                    mithilfe von &0xff
                 3. Ermittle mithilfe des UnaryOperators die richtige Anzahl für den Bitshift
                    und führe diesen durch.
                 4. Füge die Bits an der nun richtigen Position zu dem Sample mithilfe von |= hinzu.

                 Das Ergebnis nach dieser Schleife ist ein vollständig zusammengesetztes Sample.
                 Mehr Informationen zur Kodierung sind in der Dokumentation.
                 */

                inputSamples[channel][sample] |= (bytes[byteIndex + byteNr] & 0xff) << bitShifter.applyAsInt(byteNr);
            }

            // Verringert die Lautstärke des internen Signals, um Übersteuern zu verhindern.
            inputSamples[channel][sample] /= HEADROOM_DIVISOR;


        });

        LOGGER.info("Das gesamte Audiosignal wurde erfolgreich dekodiert!");

        // Erstelle eine neue Instanz zum Zurückgeben
        Audible audible = new AudioProcessor(inputSamples, format.getSampleRate(), audioEffects);

        //LOGGER.info("Übersicht der Erstellten Instanz:\n{}", audible);

        // Leere audioEffects, falls Änderungen im Audioformat aufgetreten sind
        if (inputSamples.length != audioEffects.length || sampleRate != format.getSampleRate())
            audible = audible.setEffects();

        return audible;
    }


    /**
     * Wendet alle Effekte auf den einzelnen Kanälen in der richtigen Reihenfolge an.
     * Dazu werden die neuen AudioEffect-Instanzen zwischengespeichert und die verarbeiteten
     * Samples herausgelesen.
     * Auch hier ist eine implementierung mit der vorgesehenen Verwendung von Streams nicht möglich, da das
     * Ergebnis des vorausgehenden Effektes Ausgangspunkt für die nächste Berechnung ist. Die äußere
     * for-Schleife habe ich dennoch aus Effizienzgründen mit einem parallelen Stream implementiert.
     * @return
     */
    public Audible processEffects() {
        int[][] processedChannelSamples = channelSamples.clone();

        if (channelSamples[0].length > 4096)
            LOGGER.info("Wende Effekte auf das Audiosignal an...");

        IntStream.range(0, channelSamples.length).parallel().forEach(channel -> {
            for (int effectNr = 0; effectNr < audioEffects[0].length; effectNr++) {
                audioEffects[channel][effectNr] = audioEffects[channel][effectNr].processSamples(processedChannelSamples[channel]);
                processedChannelSamples[channel] = audioEffects[channel][effectNr].getProcessedSamples();
            }
        });

        // Logge nur, wenn ein längerer Verarbeitungszeitraum vorhanden ist um Konsolen-Spam zu vermeiden
        if (channelSamples[0].length > 4096)
            LOGGER.info("Das Audiosignal wurde mit {} Effekten pro Kanal verarbeitet.", audioEffects[0].length);

        return new AudioProcessor(processedChannelSamples, sampleRate, audioEffects);
    }


    /**
     * Verstärkt das Audiosignal auf ein standardisiertes Lautstärkelevel.
     * @return Eine neue Instanz mit dem gewünschten Effekt
     */
    public Audible normalize() {
        // Errechne den Faktor so, dass das Sample mit dem höchsten Ausschlag genau
        // den empfohlenen Höchstwert (RECOMMENDED_PEAK) trifft.
        double factor = Audible.dBToFactor(RECOMMENDED_PEAK - getPeakInDBFS());

        LOGGER.info("Normalisiere das Audiosignal mit einem gain-Faktor von: {}", factor);

        // Multipliziert jedes Sample mit dem errechneten Faktor und gibt eine neue Instanz zurück
        return new AudioProcessor(Arrays.stream(channelSamples).map(samples ->
                Arrays.stream(samples).map(s -> (int) (s * factor)).toArray()).toArray(int[][]::new), sampleRate, audioEffects);
    }


    /**
     * Kehrt das Audiosignal um, sodass die Datei rückwärts gespielt wird.
     * @return Eine neue Instanz mit dem gewünschten Effect
     */
    public Audible reverse() {
        return new AudioProcessor(Arrays.stream(channelSamples).map(samples -> IntStream.rangeClosed(1, samples.length).map(i -> samples[samples.length - i]).toArray()).toArray(int[][]::new), sampleRate, audioEffects);
    }


    /**
     * Lässt das Audiosignal zu Beginn lauter werden, sodass keine Störgeräusche durch
     * das Abspielen auftreten.
     * @param seconds - Dauer des Ausfadens
     * @return eine neue Instanz mit dem gewünschten Effekt
     */
    public Audible fadeIn(double seconds) {
        assert seconds > 0 && seconds < channelSamples[0].length / sampleRate : "Die Fade-Zeit muss im Verarbeitungszeitraum liegen! | Angegeben: " + seconds + "s von " + channelSamples[0].length / sampleRate + "s | Länge: " + channelSamples[0].length + " Samples";

        // Errechne den Faktor, um die das Audiosignal pro Sekunde exponentiell lauter/leiser wird.
        double timeFactor = AudioProcessor.timeFactor(seconds, sampleRate);

        return new AudioProcessor(Arrays.stream(channelSamples).map(int[]::clone).peek(samples -> {
            double gainFactor = 0;

            // Rechne den Faktor auf die aktuelle Lautstärke dazu und wende das Lautstärkelevel an.
            for (int i = 0; i < samples.length; i++)
                samples[i] = (int) (samples[i] * (gainFactor += (1 - gainFactor) * timeFactor));

        }).toArray(int[][]::new), sampleRate, audioEffects);
    }


    /**
     * Erstellt eine neue Instanz, die nur ein Teil des Audiosignals enthält. Zudem kann eine Instanz
     * übergeben werden, woraus die zwischengespeicherten Zustände der Effektgeräte entnommen werden können.
     * @param fromSample Startposition
     * @param numSamples Länge des Teilabschnitts
     * @param lastAudible Instanz aus der die Zustände der Effektgeräte gelesen werden sollen
     * @return Eine neue Instanz mit nur einem Teilabschnitt des Audiosignals
     */
    public Audible splice(int fromSample, int numSamples, Audible lastAudible) {
        assert fromSample >= 0 && fromSample < channelSamples[0].length : "Die angegebene Startzeit ist ungültig! Startzeit: " + fromSample;
        assert numSamples > 0 && fromSample + numSamples <= channelSamples[0].length : "Der angegebene Zeitraum ist ungültig! Startzeit: " + fromSample + " | Länge: " + numSamples + " | Anzahl aller Samples: " + channelSamples[0].length;

        // Erstelle einen Teilabschnitt des channelSamples-Arrays
        int[][] spicedSamples = Arrays.stream(channelSamples).map(samples ->
                Arrays.stream(samples, fromSample, fromSample + numSamples).toArray()).toArray(int[][]::new);

        // Übernehme den Puffer der letzten Audioeffekte.
        if (lastAudible != null) {
            AudioEffect[][] lastEffects = lastAudible.getAudioEffects();
            return new AudioProcessor(spicedSamples, sampleRate, audioEffects.length == lastEffects.length && audioEffects[0].length == lastEffects[0].length ?
                    IntStream.range(0, audioEffects.length).mapToObj(i -> IntStream.range(0, audioEffects[i].length)
                                    .mapToObj(j -> audioEffects[i][j].readFromPreviousEffect(lastEffects[i][j]))
                                    .toArray(AudioEffect[]::new))
                            .toArray(AudioEffect[][]::new) : audioEffects);

        } else return new AudioProcessor(spicedSamples, sampleRate, audioEffects);
    }


    /**
     * Das Konvertieren in eine abspeicherbare Form funktioniert genauso, wie das dekodieren in setBuffer().
     * @param format Das gewünschte AudioFormat
     * @return Das Audiosignal kodiert in einem einzelnen byte-Array
     */
    public byte[] getOutput(AudioFormat format) {
        assertFormat(format);
        assert format.getSampleRate() == sampleRate : "Die Samplerate des Outputdatei muss gleich der Samplerate der Inputdatei sein";

        int bytesPerFrame = format.getFrameSize();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        // Speichere einen UnaryOperator, der dynamisch die Anzahl der zu verschiebenen bits errechnet.
        IntUnaryOperator bitShifter = format.isBigEndian() ? bigEndianBitshift() : smallEndianBitshift(bytesPerSample);

        // Errechne die Länge des byte-Arrays durch die Anzahl der Kanäle * die Anzahl der Samples pro Kanal *
        // der Größe eines Frames in bytes.
        byte[] bytes = new byte[channelSamples[0].length * format.getFrameSize()];

        LOGGER.info("Speichere das Audiosignals in einem byte-Array mit einer Länge von {} bytes und einer Framegröße von {} bytes", bytes.length, format.getFrameSize());

        // Sehr ähnliche Implementierung, wie bei setBuffer() mit feinen Unterschieden
        IntStream.range(0, channelSamples.length * channelSamples[0].length).parallel().forEach(i -> {
            // Errechne die Positionen in den beiden Arrays
            int channel = i / channelSamples[0].length;
            int sample = i % channelSamples[0].length;
            int byteIndex = channel * bytesPerSample + sample * bytesPerFrame;
            int sampleData = channelSamples[channel][sample] * HEADROOM_DIVISOR;

            // Zerlege die Sampledaten in bytes und speichere diese an der richtigen Position.
            // Mehr Informationen zur Kodierung sind in der Dokumentation.
            for (int byteNr = 0; byteNr < bytesPerSample; byteNr++) {
                bytes[byteIndex + byteNr] = (byte) (sampleData >>> bitShifter.applyAsInt(byteNr));
            }
        });

        LOGGER.info("Audiosignal erfolgreich kodiert!");

        return bytes;
    }


    /**
     * Errechnet den maximalen Ausschlag in dBFS.
     * Dazu wird aus allen Samples der Wert mit dem höchsten Betrag ausgewählt und in dBFS umgerechnet.
     * @return eine neue Instanz mit dem gewünschten Effekt
     */
    public double getPeakInDBFS() {
        return intTOdBFS(Arrays.stream(channelSamples).parallel().flatMapToInt(Arrays::stream).map(Math::abs).max().orElse(1) * HEADROOM_DIVISOR);
    }


    /**
     * Errechnet die Intensität einer bestimmten Frequenz mithilfe des Fast-Fourier-Transforms, eine
     * recheneffiziente implementierung des Discrete-Fourier-Transforms.
     *
     * Hinweise:
     *   - Je länger der aktualisierte Buffer, desto enger wird die analysierte Frequenz,
     *     sodass Frequenzen mit nur wenigen cent neben der Zielfrequenz schon nicht mehr beachtet werden.
     *   - Aufgrund des Analyseverfahrens, nehmen mögliche destruktive Interferenzen bei längeren Zeiträumen
     *     zu, sodass das Ergebnis bei langen Audiodateien zusätzlich beeinflusst werden kann.
     *  => Das Verfahren funktioniert bei kurzen Analysezeiträumen ab ~64 samples bis < ~0,1s am besten. (Werte erfahrungsgemäß)
     * @param frequency - Frequenz der Analyse
     * @return die Intensität der angegebenen Frequenz
     */
    public double getFFTdBFS(double frequency) {
        assert frequency > 0 && frequency <= sampleRate * 0.5 : "Die Frequenz muss > 0 sein und darf das Nyquist-Limit nicht übersteigen! (" + frequency + ")";

        //      k = Phasen pro Sample
        //  <=> k = Pufferlänge * SamplesProPhase
        //  <=> k = Pufferlänge * Frequenz / SampleRate
        //
        // vom Typen float, da mit der Klasse PVector gearbeitet wird
        float k = (float) (channelSamples[0].length * frequency / sampleRate);

        // Jeder Kanal wird einzeln analysiert und am Ende der Durchschnitt der Kanäle errechnet
        double fftValue = Arrays.stream(channelSamples)
                .map(samples -> IntStream.range(0, channelSamples[1].length)
                        // Jedes Sample wird als komplexe Zahl mithilfe eines zweidimensionalen PVectors dargestellt.
                        // Der Winkel der Komplexen Zahl entspricht dem Phasenwinkel (deswegen PVector.fromAngle(Phasenwinkel))
                        // und die Länge bzw. der Betrag stellt den Ausschlag des Samples dar.
                        // (Entstandener Vektor der Länge 1 wird mit dem Ausschlag des Samples multipliziert)
                        .mapToObj(i -> PVector.fromAngle(2 * PConstants.PI * k * i / channelSamples[0].length).mult(channelSamples[1][i]))
                        // Alle entstandenen komplexen Werte werden addiert
                        .reduce(new PVector(), (complex1, complex2) -> PVector.add(complex1, complex2)))
                // Die Intensität der Testfrequenz ist nun proportional zum Betrag
                // der komplexen Zahl.
                // (PVector::mag errechnet die Länge des Vektors bzw. den Betrag der komplexen Zahl)
                .mapToDouble(PVector::mag).average().orElse(0);

        // Damit ist der Vorschlag für ein ganzes Abschlussprojekt aus Hr. Herzbergs Projektideen
        // "Frequenzanalyse mit der Diskreten Fourier Transformation"
        // praktisch in einem einzigen (verschachtelten) Stream gelöst ;-)

        // Zum Zurückgeben wird der errechnete Wert noch mit dem Proportionalitätsfaktor
        // verrechnet und in die Einheit dBFS umgerechnet
        return intTOdBFS((int) (HEADROOM_DIVISOR * fftValue * 2.0 / channelSamples[0].length));
    }

    /**
     * @return Kopie des Audioeffekt-Arrays
     */
    public AudioEffect[][] getAudioEffects() {
        return Arrays.stream(audioEffects).map(AudioEffect[]::clone).toArray(AudioEffect[][]::new);
    }


    /**
     * Erhalte grundlegende Informationen zu dem aktuellen Audiosignal
     */
    public String toString() {
        String s = "\n";

        s += " - AudioProcessor -\n";
        s += "Länge: " + String.format("%.2f", channelSamples[0].length / sampleRate) + " s\n";
        s += "Kanäle: " + channelSamples.length + "\n";
        s += "Höchster Ausschlag: " + String.format("%.1f dBFS", getPeakInDBFS()) + "\n";

        // Entferne den Kommentar, um die Frequenzanalyse anzeigen zu lassen.
        // Deaktiviert, da diese bei langen Audiosignalen kaum aussagekraft hat.
        /*
        s += "\nFrequenzanalyse:\n";
        s += Arrays.stream(new int[]{100, 150, 300, 440, 750, 1000, 2000, 4000, 8000, 12000})
                .limit(11)
                .mapToObj(freq -> String.format("%5d Hz: ", freq) + "=".repeat((int) Math.max(40 + 0.5 * getFFTdBFS(freq), 0)))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        s += "\n          | -80 dBFS         | -40 dBFS         | 0 dBFS";
*/

        // Optional können aufeinanderfolgende Samples mit dem selben Wert überprüft werden,
        // was jedoch überwiegend beim debuggen neuer (De-)Kodierungsmethoden hilfreich ist.
/*
        long countPairs = Arrays.stream(channelSamples).mapToLong(
                samples -> IntStream.range(1, samples.length).filter(i -> samples[i-1] == samples[i]).count()).sum();
        s += "\n\nEs sind " + countPairs + " identische aufeinanderfolgende Samples enthalten. Das sind " + String.format("%.1f",(1d * countPairs / channelSamples[0].length / channelSamples.length * 100)) + "% von allen Samples.\n";
*/

        return s;
    }


    /**
     * Überprüfe die Bedingungen, die ein AudioFormat erfüllen muss.
     * @param format
     */
    private void assertFormat(AudioFormat format) {
        assert format != null : "Das Audioformat darf nicht null sein!";
        assert format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) : "Es wird nur die Kodierung 'PCM_SIGNED' unterstützt!";
        assert format.getChannels() >= 1 : "Die Audiodatei benötigt mindestens 1 Kanal!";
        assert format.getSampleSizeInBits() == 16 || format.getSampleSizeInBits() == 24 || format.getSampleSizeInBits() == 32 :
                "Es werden nur Audiodateien mit 16-Bit, 24-bit oder 32-bit-Kodierungen pro Sample unterstützt";
        assert Float.isFinite(format.getSampleRate()) && format.getSampleRate() > 0 : "Die Samplerate muss > 0 sein!";
    }
}

