package com.github.qpcrummer.beatmaker.processing;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import imgui.type.ImDouble;

import java.util.ArrayList;
import java.util.List;

public final class BeatOnsetExtractor implements OnsetHandler {
    public final List<ImDouble[]> beats = new ArrayList<>();
    public List<ImDouble[]> run() {
        int size = 512;
        int overlap = 256;
        int sampleRate = 16000;
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(MusicPlayer.currentSong.toFile().getAbsolutePath(), sampleRate, size, overlap);

        ComplexOnsetDetector detector = new ComplexOnsetDetector(size);
        BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
        detector.setHandler(handler);

        dispatcher.addAudioProcessor(detector);
        dispatcher.run();

        handler.trackBeats(this);

        List<ImDouble[]> copy = List.copyOf(beats);

        beats.clear();

        return copy;
    }

    @Override
    public void handleOnset(double time, double salience) {
        beats.add(BeatFile.convertToImDoubleArray(time, 0));
    }
}
