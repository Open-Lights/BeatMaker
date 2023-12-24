package com.github.qpcrummer.beatmaker.processing;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.Chart;
import com.github.qpcrummer.beatmaker.gui.MainGUI;
import imgui.type.ImDouble;
import imgui.type.ImInt;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public final class Generator {
    private static final BeatOnsetExtractor beatExtractor = new BeatOnsetExtractor();
    private static final GeneralOnsetExtractor onsetExtractor = new GeneralOnsetExtractor();
    public static final ImInt percussionSensitivity = new ImInt();
    public static final ImDouble percussionThreshold = new ImDouble();
    public static final ImDouble complexPeakThreshold = new ImDouble();
    public static final ImDouble complexMinimumInterOnsetInterval = new ImDouble();
    public static final ImDouble complexSilenceThreshold = new ImDouble();
    // Defaults
    static {
        defaults();
    }

    public static void defaults() {
        percussionSensitivity.set(20);
        percussionThreshold.set(8.0);
        complexPeakThreshold.set(0.3);
        complexMinimumInterOnsetInterval.set(0.03);
        complexSilenceThreshold.set(-70.0);
    }
    public static void generatePercussionChartsForSong() {
        if (MusicPlayer.currentSong != null) {
            List<ImDouble[]> times = new ArrayList<>();
            CountDownLatch processingDone = new CountDownLatch(1);

            try {
                AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(MusicPlayer.currentSong.toFile(), 2048, 1024);

                PercussionOnsetDetector onsetDetector = new PercussionOnsetDetector(dispatcher.getFormat().getSampleRate(), 2048, (time, salience) -> times.add(BeatFile.convertToImDoubleArray(time, 0)), 40, 1.0);

                dispatcher.addAudioProcessor(onsetDetector);

                // Add a listener for completion
                dispatcher.addAudioProcessor(new AudioProcessor() {
                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        return true;
                    }

                    @Override
                    public void processingFinished() {
                        processingDone.countDown();
                    }
                });

                new Thread(dispatcher).start();

                // Wait for the processing to finish
                processingDone.await();

            } catch (UnsupportedAudioFileException | IOException | InterruptedException e) {
                Main.logger.warning("Failed to generate chart for song");
            }

            Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
            chart.timestamps.addAll(times);

            Data.charts.add(chart);
        }
    }

    public static void generateComplexChartsForSong() {
        if (MusicPlayer.currentSong != null) {
            List<ImDouble[]> times = new ArrayList<>();
            CountDownLatch processingDone = new CountDownLatch(1);

            try {
                AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(MusicPlayer.currentSong.toFile(), 2048, 1024);

                ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(2048, 0.7, 0.1);
                onsetDetector.setHandler((time, salience) -> times.add(BeatFile.convertToImDoubleArray(time, 0)));

                dispatcher.addAudioProcessor(onsetDetector);

                // Add a listener for completion
                dispatcher.addAudioProcessor(new AudioProcessor() {
                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        return true;
                    }

                    @Override
                    public void processingFinished() {
                        processingDone.countDown();
                    }
                });

                new Thread(dispatcher).start();

                // Wait for the processing to finish
                processingDone.await();

            } catch (UnsupportedAudioFileException | IOException | InterruptedException e) {
                Main.logger.warning("Failed to generate chart for song");
            }

            // Assuming you have a Chart class and a method convertToImDoubleArray
            // to convert the time and salience into ImDouble array.
            Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
            chart.timestamps.addAll(times);

            Data.charts.add(chart);
        }
    }

    public static void generateWithBeatExtractor() {
        if (MusicPlayer.currentSong != null) {
            Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
            chart.timestamps.addAll(beatExtractor.run());

            Data.charts.add(chart);
        }
    }

    public static void generateWithOnsetExtractor() {
        if (MusicPlayer.currentSong != null) {
            Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
            chart.timestamps.addAll(onsetExtractor.run());

            Data.charts.add(chart);
        }
    }
}
