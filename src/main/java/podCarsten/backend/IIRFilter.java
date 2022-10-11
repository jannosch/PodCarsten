package podCarsten.backend;

import static podCarsten.backend.AudioProcessor.LOGGER;

record IIRFilter(double a1, double a2, double b0, double b1, double b2, int dry1, int dry2,
                        int[] processedSamples) implements AudioEffect {
    // Da das record package-private ist, erhält die IL keinen Zugriff auf die Daten.

    // Constructor-asserts durch statische Factory-Methode nicht nötig

    /**
     * Verarbeitet die mitgegeben Samples und speichert diese in der zurückgegebenen Instanz.
     *      <br><br>
     * Die Implementierung der IIR-Filter habe ich aus der mathematischen Darstellung aus dem Buch
     * Audio EQ Cookbook von Robert Bristow-Johnson entwickelt.
     * Link: http://shepazu.github.io/Audio-EQ-Cookbook/audio-eq-cookbook.html
     *      <br><br>
     * Mehr Informationen zur Funktionsweise von IIR-Filtern in der Dokumentation.
     * @param samples Zu verarbeitende Samples
     * @return Neue Instanz, die die verarbeiteten Samples enthält.
     */
    public AudioEffect processSamples(int... samples) {
        assert samples != null;

        int dry1 = this.dry1;
        int dry2 = this.dry2;
        int wet1 = processedSamples != null && processedSamples.length >= 1 ? processedSamples[processedSamples.length - 1] : 0;
        int wet2 = processedSamples != null && processedSamples.length >= 2 ? processedSamples[processedSamples.length - 2] : 0;

        int[] processedSamples = new int[samples.length];

        // Mit dieser Schleife werden alle Samples hintereinander verarbeitet.
        // Da alle Samples in der richtigen Reihenfolge abhängig von den Werten davor
        // berechnet werden, habe ich mich gegen eine Implementierung mit Streams entschieden.
        for (int i = 0; i < samples.length; i++) {
            // Errechne das neue Sample und speichere die letzten Werte für die zukünftigen Berechnungen ab.
            processedSamples[i] = (int) (-a2 * wet2 - a1 * wet1 + b2 * dry2 + b1 * dry1 + b0 * samples[i]);
            wet2 = wet1;
            wet1 = processedSamples[i];
            dry2 = dry1;
            dry1 = samples[i];

            // Sehr kompakte Version, die ich aus Übersichtlichkeitsgründen leider nicht verwende,
            // die aber die gleiche Funktion erfüllt.
            // processedSamples[i] = wet1 = (int) (-a2 * wet2 - a1 * (wet2 = wet1) + b2 * dry2 + b1 * (dry2 = dry1) + b0 * (dry1 = samples[i]));
        }

        if (samples.length > 4096)
            LOGGER.info("Ein IIR-Filter wurde auf das Audiosignal angewendet");

        return new IIRFilter(a1, a2, b0, b1, b2, dry1, dry2, processedSamples);
    }


    /**
     * @return Die zuletzt verarbeiteten Samples
     */
    public int[] getProcessedSamples() {
        return processedSamples;
    }


    /**
     * Liest den Zustand des vorherigen Effektes und gibt eine neue Instanz mit neuen Parametern,
     * aber dem zuletzt aufgetretenen Zustand zurück.
     * @param audioEffect Instanz, die den alten Zustand enthält
     */
    public AudioEffect readFromPreviousEffect(AudioEffect audioEffect) {
        if (audioEffect instanceof IIRFilter other) // Impliziert: audioEffect != null
            return new IIRFilter(a1, a2, b0, b1, b2, other.dry1, other.dry2, other.processedSamples);
        return this;
    }
}
