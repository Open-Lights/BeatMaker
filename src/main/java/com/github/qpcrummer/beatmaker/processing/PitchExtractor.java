package com.github.qpcrummer.beatmaker.processing;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import imgui.type.ImDouble;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PitchExtractor implements PitchDetectionHandler {
    public final List<ImDouble[]> beats = new ArrayList<>();

    public List<ImDouble[]> run(Path path) {
        PitchProcessor.PitchEstimationAlgorithm algo = PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;

        int size = 1024;
        int overlap = 0;
        int samplerate = 16000;
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(path.toAbsolutePath().toString(), 16000, size, overlap);
        dispatcher.addAudioProcessor(new PitchProcessor(algo, samplerate, size, this));
        dispatcher.run();
        List<ImDouble[]> copy = List.copyOf(beats);
        beats.clear();
        lastNote = 0;
        return copy;
    }

    private int lastNote = 0;

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        double timeStamp = audioEvent.getTimeStamp();
        float pitch = pitchDetectionResult.getPitch();
        if (hasNoteChanged(pitch)) {
            beats.add(BeatFile.convertToImDoubleArray(timeStamp, 0));
        }
    }

    private boolean hasNoteChanged(float frequency) {
        if (frequency > 0) {
            double division = frequency / 440f;
            double note = 12 * logBase(division, 2) + 49;
            int rounded = (int) Math.round(note);
            if (lastNote != rounded) {
                lastNote = rounded;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // TODO Thresholds
    private boolean significantDifference(int value, int comparingValue, int minThreshold) {
        return Math.abs(value - comparingValue) >= minThreshold;
    }

    private double logBase(double d, int base) {
        return Math.log(d) / Math.log(base);
    }
}
