package podCarsten.backend;

import java.util.Arrays;

import static podCarsten.backend.AudioProcessor.LOGGER;

record RMSCompressor(double reductionFactor, double ratio, double thresholdValue, double attackFactor, double releaseFactor,
                     int rmsIndex, int[] rmsWindow, int[] processedSamples) implements AudioEffect {
    // Da das record package-private ist, erhält die IL keinen Zugriff auf die Daten.


    // Die Größe des RMS-Fensters ist zeitlich vordefiniert.
    static final double RMS_SECONDS = 0.002;



    /**
     * Verarbeitet die mitgegeben Samples und speichert diese in der zurückgegebenen Instanz.
     * In rmsWindow[] werden die letzten Samples gespeichert, um daraus den aktuellen RMS-Wert zu
     * errechnen, aus dem das Level der Kompression hervorgeht.
     *      <br><br>
     * Die Implementierung des Kompressors habe ich vollständig selber entwickelt (mit Vorwissen
     * aus der Musiktechnik zu Themen, wie RMS und grundlegenden Funktionen von Kompressoren).
     *      <br><br>
     * Mehr Informationen zur Funktionsweise von RMS-Kompressoren in der Dokumentation.
     *      <br><br>
     * @param samples Zu verarbeitende Samples
     * @return Neue Instanz, die die verarbeiteten Samples enthält.
     */
    public AudioEffect processSamples(int... samples) {
        // Speichere alle sich ändernden Werte in Variablen
        double reductionFactor = this.reductionFactor;
        int rmsIndex = this.rmsIndex;
        int[] rmsWindow = this.rmsWindow.clone();
        int[] processedSamples = new int[samples.length];

        // Da Kompression das Lautstärkelevel stark verändern kann, wird das gesamte Signal
        // nach der Kompression verstärkt, um das Durchschnittslevel approximiert konstant zu halten.
        double autoGain = 0.175 * ratio * Math.pow(2, -20 * thresholdValue / Integer.MAX_VALUE) + 1;

        for (int i = 0; i < samples.length; i++) {
            // Ersetzte den ältesten Wert aus dem array mit dem Aktuellen.
            // Funktionsweise ähnlich zu Moving-Average-Algorithmen.
            rmsWindow[rmsIndex = (rmsIndex + 1) % rmsWindow.length] = samples[i];

            // Errechne den aktuellen Lautstärkewert.
            // Da die hohen Zahlen schnell einen Overflow erzeugen, wird erst durch die Fenstergröße geteilt
            // und dann erneut multipliziert:
            //      s² / windowLength  <=>  (s / windowLength) * s
            double rmsValue = Math.sqrt((double) Arrays.stream(this.rmsWindow)
                    .mapToLong(s -> ((long) s / this.rmsWindow.length) * s).sum());

            // Errechne den Ziellautstärkewert für die Eingabe
            double targetValue = rmsValue < thresholdValue ? rmsValue : thresholdValue + (rmsValue - thresholdValue) / ratio;

            // Wende die Attack-/Release-Faktoren an, sodass der Zielwert nach der voreingestellten
            // Zeit erreicht wird.
            if (rmsValue * reductionFactor > targetValue)
                reductionFactor = reductionFactor - attackFactor * (targetValue / (rmsValue * reductionFactor));
            else reductionFactor += (1 - reductionFactor) * releaseFactor;

            // Wende die Kompression und die Lautstärkekompensation an
            processedSamples[i] = (int) (samples[i] * reductionFactor * autoGain);
        }

        if (samples.length > 4096)
            LOGGER.info("Ein RMS-Compressor wurde auf das Audiosignal angewendet. Aktueller gain-Reduktion: {}; Aktueller auto-gain: {}", reductionFactor, autoGain);

        return new RMSCompressor(reductionFactor, ratio, thresholdValue, attackFactor, releaseFactor, rmsIndex, rmsWindow, processedSamples);
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
        if (audioEffect instanceof RMSCompressor other) // Impliziert: audioEffect != null
            return new RMSCompressor(other.reductionFactor, ratio, thresholdValue, attackFactor, releaseFactor,
                    other.rmsIndex, other.rmsWindow, other.processedSamples);
        return this;
    }
}
