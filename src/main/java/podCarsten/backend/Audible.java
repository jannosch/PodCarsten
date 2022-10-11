package podCarsten.backend;

import javax.sound.sampled.AudioFormat;

public interface Audible {


    /**
     * Statische Factory-Methode für das Erstellen einer initialen Instanz.
     * Wird benötigt, damit die Interaktionslogik keine unvollständige
     * Instanz erhalten kann.
     * @param bytes der Audiosignals
     * @param audioFormat Format des Audiosignals
     * @param audioEffects Optionale Effekte
     * @return eine vollständige Audible-Instanz
     */
    static Audible of(byte[] bytes, AudioFormat audioFormat, AudioEffect... audioEffects) {
        return new AudioProcessor().setInput(bytes, audioFormat).setEffects(audioEffects);
    }

    /**
     * Errechne aus einem Wert in dB einen Faktor aus, mit dem alle Samples
     * des Audiosignals multipliziert werden können, sodass die gewünschte
     * Lautstärkeänderung erreicht wird.
     * @param dBGain Die gewünschte Lautstärkeänderung in dB
     * @return
     */
    static double dBToFactor(double dBGain) {
        return Math.pow(2, dBGain / 6);
    }



    // BLOCK 1: Änderungen von bestimmten Voreinstellungen

    /** Setzt AudioEffekte, die mit processEffects() angewendet werden können */
    Audible setEffects(AudioEffect... audioEffects);

    /** Lädt ein neues Audiosignal und dekodiert dieses */
    Audible setInput(byte[] bytes, AudioFormat audioFormat);

    /** Gibt einen Teilabschnitt des Audiosignals einer bereits bestehenden Instanz zurück */
    Audible splice(int fromSample, int numSamples, Audible lastAudible);



    // BLOCK 2: Änderungen auf das Audiosignal abhängig von Voreinstellungen

    /** Wendet die Effekte auf das Audiosignal im Puffer an */
    Audible processEffects();

    /** Setzt die Lautstärke auf ein standardisiertes Level */
    Audible normalize();

    /** Kehrt die Reihenfolge des Audiosignals um */
    Audible reverse();

    /** Lässt das Audiosignal zu Beginn in einer bestimmten Zeit einfaden */
    Audible fadeIn(double seconds);

    /**
     *  Lässt das Audiosignal am Ende eine bestimmte Zeit lang ausfaden.
     *  Profitiert von asserts von fadeIn.
     * @param seconds Dauer des Ausfadens in Sekunden
     * @return Eine neue Instanz mit dem gewünschten Effekt
     */
    default Audible fadeOut(double seconds) {
        return this.reverse().fadeIn(seconds).reverse();
    }

    /**
     * Lasst das Audiosignal am Anfang und am Ende eine bestimmte Zeit
     * lang aus-/einfaden.
     * Profitiert bon asserts von fadeIn.
     * @param seconds Dauer in Sekunden
     * @return Eine neue Instanz mit dem gewünschten Effekt
     */
    default Audible fade(double seconds) {
        return this.fadeIn(seconds).fadeOut(seconds);
    }



    // BLOCK 3: Daten aus der Instanz werden in aufbereiteter Form zurückgegeben

    /** Gibt das Audiosignal im gewünschten Format in einem byte-Array zurück */
    byte[] getOutput(AudioFormat audioFormat);

    /** Gibt den höchsten Ausschlag des Audiosignals in dBFS zurück */
    double getPeakInDBFS();

    /** Gibt die Intensität einer bestimmten Frequenz zurück */
    double getFFTdBFS(double frequency);

    /** Gibt die Audioeffekte der einzelnen Kanäle zurück.
     * Genutzt zur Kommunikation verschiedener Instanzen */
    AudioEffect[][] getAudioEffects();


}
