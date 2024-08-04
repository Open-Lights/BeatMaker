package com.github.qpcrummer.beatmaker.processing;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import imgui.type.ImDouble;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GeneralOnsetExtractor implements OnsetHandler {
    public final List<ImDouble[]> beats = new ArrayList<>();

    public List<ImDouble[]> run() {
        return run(MusicPlayer.currentAudio.fullAudioPath);
    }

    public List<ImDouble[]> run(Path path) {
        int size = 512;
        int overlap = 256;
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(path.toFile().getAbsolutePath(), 44100, size, overlap);
        ComplexOnsetDetector detector = new ComplexOnsetDetector(size, 0.7, 0.1);
        detector.setHandler(this);
        dispatcher.addAudioProcessor(detector);

        dispatcher.run();

        List<ImDouble[]> copy = List.copyOf(beats);

        beats.clear();

        return copy;
    }

    @Override
    public void handleOnset(double time, double salience) {
        beats.add(BeatFile.convertToImDoubleArray(time, 0));
    }
}
