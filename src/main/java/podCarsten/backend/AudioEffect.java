package podCarsten.backend;

public interface AudioEffect {

    // STATISCHE FACTORY-METHODEN:

    /**
     * Gibt eine Instanz eines AudioEffects wieder, die beim Audiosignal alle Frequenzen
     * unterhalb der Angegebenen herausfiltert. Dabei handelt es sich um eine diskrete
     * Implementierung eines 2-Pol-Highpass-Filters, welcher die zu filternden Frequenzen
     * mit einer stärke von 12dB pro Oktave unter der Zielfrequenz abschwächt.
     *
     * Mehr Informationen: https://en.wikipedia.org/wiki/Equalization_(audio)#High-pass_and_low-pass_filters
     *
     * @param sampleRate des Audiosignals
     * @param frequency ab welcher der Effekt signifikant werden soll
     * @param q Breite des Übergangsbereichs
     *          < 0.71: weicher Frequenzgang
     *          = 0.71: Standardwert; kleinste Breite des Übergangsbereichs ohne Resonante Frequenzen
     *          > 0.71: härterer Frequenzgang mit resonantem Bereich bei der Zielfrequenz
     */
    static AudioEffect highPass(float sampleRate, double frequency, double q) {
        assertFilter(sampleRate, frequency, q);

        // Pre Calculations
        assert sampleRate > 0 && frequency > 0 && frequency < sampleRate && q > 0;
        double omega = 2 * Math.PI * (frequency / sampleRate);
        double alpha = Math.sin(omega) / (2 * q);
        double a0 = 1 + alpha;

        // Return new filter
        IIRFilter f = new IIRFilter(-2 * Math.cos(omega) / a0, (1 - alpha) / a0,
                (1 + Math.cos(omega)) / (2 * a0), -(1 + Math.cos(omega)) / a0, (1 + Math.cos(omega)) / (2 * a0),
                0, 0, null);

        AudioProcessor.LOGGER.info("Erstelle einen high-pass-IIR-Filter mit folgenden Parametern: a1: {}; a2: {}; b0: {}; b1: {}; b2: {}", f.a1(), f.a2(), f.b0(), f.b1(), f.b2());

        return f;
    }


    /**
     * Gibt eine Instanz eines AudioEffects wieder, die das Audiosignal
     * in einem bestimmten Frequenzbereich verstärken. Dabei sieht der Frequenzgang
     * einer Glockenfunktion ähnlich, weswegen dieser Filter oft Bell-Filter genannt wird.
     *
     * Mehr Informationen: https://en.wikipedia.org/wiki/Equalization_(audio)#Parametric_equalizer
     *
     * @param sampleRate des Audiosignals
     * @param frequency an der das Audiosignal um den gewünschten Wert
     * @param q Breite der Glockenform. Bei q = 1 halbiert sich die Verstärkung der Frequenzen
     *          etwa pro Oktave Entfernung zur Zielfrequenz.
     * @param dBGain amount of boost
     */
    static AudioEffect bell(float sampleRate, double frequency, double q, double dBGain) {
        assertFilter(sampleRate, frequency, q);
        if (dBGain == 0) return new IIRFilter(0, 0, 1, 0, 0, 0, 0, null);

        // Pre Calculations
        double omega = 2 * Math.PI * (frequency / sampleRate);
        double amount = Math.pow(10, dBGain / 40);
        double alpha = Math.sin(omega) / (q * Math.sqrt(2));
        double a0 = 1 + (alpha / amount);

        // Return new filter
        IIRFilter f = new IIRFilter((-2 * Math.cos(omega)) / a0, (1 - alpha / amount) / a0,
                (1 + alpha * amount) / a0, -(2 * Math.cos(omega)) / a0, (1 - alpha * amount) / a0,
                0, 0,  null
        );

        AudioProcessor.LOGGER.info("Erstelle einen glockenförmigen IIR-Filter mit folgenden Parametern: a1: {}; a2: {}; b0: {}; b1: {}; b2: {}", f.a1(), f.a2(), f.b0(), f.b1(), f.b2());

        return f;
    }


    /*
    Hier ist ein Beispiel für eine weitere Equalizer-Form, die in diesem Fall ein "high Shelf" ist.
    Dieser Filter verstärkt alle Frequenzen über der angegebenen Frequenz um den angegebenen Wert
    mit einem weichen Übergang um den Bereich der angegebenen Frequenz.

    static AudioEffect highShelf(float sampleRate, double frequency, double q, double dBGain) {
        assertFilter(sampleRate, frequency, q);
        if (dBGain == 0) return new IIRFilter(0, 0, 1, 0, 0, 0, 0, null);

        // Pre Calculations
        double amount = Math.pow(10, dBGain / 40);
        double omega = 2 * Math.PI * (frequency / sampleRate);
        double alpha = Math.sin(omega) / 2 * Math.sqrt((amount + 1 / amount) * (1 / q - 1) + 2);
        double cos = Math.cos(omega);
        double a0 = (amount + 1) - (amount - 1) * cos + 2 * Math.sqrt(amount) * alpha;

        // Return new filter
        return new IIRFilter(2 * ((amount - 1) - (amount + 1) * cos) / a0,
                ((amount + 1) - (amount - 1) * cos - 2 * Math.sqrt(amount) * alpha) / a0,
                amount * (amount + 1 + (amount - 1) * cos + 2 * Math.sqrt(amount) * alpha) / a0,
                -2 * amount * (amount - 1 + (amount + 1) * cos) / a0,
                amount * (amount + 1 + (amount - 1) * cos - 2 * Math.sqrt(amount) * alpha) / a0,
                0, 0,  null);
    }
     */


    /**
     * Diese Methode wird verwendet, um die Design-by-Contract-Bedingungen für die Factory-Methoden der
     * IIR-Filter zu überprüfen
     */
    private static void assertFilter(float sampleRate, double frequency, double q) {
        assert sampleRate > 0 : "Die Samplerate muss eine positive Zahl sein!";
        assert frequency > 0 && frequency <= sampleRate * 0.5 : "Die Frequenz muss > 0 sein und darf das Nyquist-Limit nicht übersteigen! (" + frequency + ")";
        assert q > 0 : "Der Parameter q muss eine positive Zahl sein!";
    }


    /**
     * Gibt eine Instanz einer RMS-Compressors mit den gewünschten Parametern zurück. Für mehr Informationen
     * siehe Klasse RMSCompressor.
     * @param ratio Verhältnis in dem das Audiosignal über dem Schwellenwert abgeschwächt werden soll.
     *              (Bei einer ratio von 2 wird das Ausgangssignal über der Schwelle um 1dB lauter,
     *              wenn das Eingangssignal 2dB lauter wird)
     * @param threshold Schwellenwert in dBFS ab dem der Kompressor die Lautstärke verringern soll.
     * @param attackSecond Anzahl der Sekunden, bis der Kompressor vollständig auf lauter werdende Signale reagiert.
     * @param releaseSecond Anzahl der Sekunden, bis der Kompressor vollständig auf leiser werdende Signale reagiert.
     * @param sampleRate Samplerate des Audiosignals
     */
    static RMSCompressor rmsCompressor(double ratio, double threshold, double attackSecond, double releaseSecond, float sampleRate) {
        assert ratio > 0 && threshold <= 0 && attackSecond > 0 && releaseSecond > 0 : "Die Angaben sind fehlerhaft";
        assert sampleRate > 0 : "Die Samplerate muss eine positive Zahl sein!";

        RMSCompressor c = new RMSCompressor(1, ratio, AudioProcessor.dBFSToInt(threshold) / Math.sqrt(2),
                AudioProcessor.timeFactor(attackSecond, sampleRate), AudioProcessor.timeFactor(releaseSecond, sampleRate),
                0, new int[(int) (RMSCompressor.RMS_SECONDS * sampleRate)], new int[0]);

        AudioProcessor.LOGGER.info("Erstelle einen RMS-Kompressor mit folgenden Parametern: Thresholdwert: {}; Attack-Faktor: {}; Release-Faktor: {}; RMS-Fenstergröße: {}", c.thresholdValue(), c.attackFactor(), c.releaseFactor(), c.rmsWindow().length);

        return c;
    }



    // METHODEN DES INTERFACES

    /** Gibt eine neue Instanz des Audioeffektes zurück,
     *  aus der das verarbeitete Signal ausgelesen werden kann */
    AudioEffect processSamples(int... samples);

    /** Gibt das als letztes verarbeitete Audiosignal zurück */
    int[] getProcessedSamples();

    /** Liest Daten zur Verarbeitung aus einem vorherigen Effekt aus, sodass ein
     *  ungestörtesAustauschen von AudioEffect-Instanzen stattfinden kann */
    AudioEffect readFromPreviousEffect(AudioEffect audioEffect);

}
