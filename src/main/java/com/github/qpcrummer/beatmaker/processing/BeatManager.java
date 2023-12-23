package com.github.qpcrummer.beatmaker.processing;

import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.Chart;
import com.github.qpcrummer.beatmaker.gui.MainGUI;
import com.github.qpcrummer.beatmaker.utils.Timer;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BeatManager {
    public static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void initialize() {
        executorService.scheduleAtFixedRate(() -> {
            if (MusicPlayer.playing) {
                double currentTime = BeatFile.millisecondsToSecondsFormatted(MusicPlayer.getPositionMilliseconds());
                MainGUI.time.set(currentTime);
                Data.charts.parallelStream().forEach(chart -> chart.checkForBeats(currentTime));
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * Toggles the light for a duration
     * @param light channel to blink light
     * @param duration duration in seconds
     */
    public static void toggleLight(int light, double duration) {
        Data.blinkBooleans.set(light, true);
        Timer.wait(duration, () -> Data.blinkBooleans.set(light, false));
    }

    /**
     * Resets all indexes in the Charts
     */
    public static void resetBeats() {
        Data.charts.parallelStream().forEach(Chart::resetBeats);
        Collections.fill(Data.blinkBooleans, false);
    }
}
