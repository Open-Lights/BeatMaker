package com.github.qpcrummer.beatmaker.data;

import com.github.qpcrummer.beatmaker.audio.StemmedAudio;
import com.github.qpcrummer.beatmaker.gui.Chart;
import com.github.qpcrummer.beatmaker.utils.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Data {
    /**
     * List of Integers of the available channels that aren't being used
     */
    public static final List<Integer> availableChannels = new ArrayList<>();

    /**
     * List of Booleans to show which boxes should be lit
     */
    public static final List<Boolean> blinkBooleans = new ArrayList<>();

    /**
     * List of Charts currently being rendered
     */
    public static final List<Chart> charts = new ArrayList<>();

    /**
     * List of Charts that can be modified by other threads
     */
    public static final List<Chart> asyncCharts = new ArrayList<>();

    /**
     * Locks the asyncChart
     */
    public static final Semaphore asyncChartsLock = new Semaphore(1);

    /**
     * The path where all beat files are saved
     */
    public static final Path savePath = Paths.get("saves");

    /**
     * Stems that are in the current song
     */
    public static final Map<StemmedAudio.StemType, Boolean> loadedStems = new HashMap<>();

    /**
     * Fills the availableChannels List
     */
    public static void initialize() {
        for (int i = 0; i < Config.channels; i++) {
            availableChannels.add(i);
            blinkBooleans.add(false);
        }
    }

    /**
     * Sets the total number of channels available to control
     * Default: Specified by Config
     * @param newAmount New total
     */
    public static void updateChannels(int newAmount) {
        if (newAmount > Config.channels) {
            availableChannels.addAll(difference(newAmount - 1, Config.channels - 1, true, false));
            Config.channels = newAmount;
        } else if (newAmount < Config.channels) {
            for (Integer n : difference(Config.channels - 1, newAmount - 1, true, false)) {
                if (availableChannels.contains(n)) {
                    availableChannels.remove(n);
                } else {
                    for (Chart chart : charts) {
                        if (chart.channels.contains(n)) {
                            chart.removeChannel(n);
                            break;
                        }
                    }
                }
            }

            Config.channels = newAmount;
        }

        blinkBooleans.clear();
        for (int i = 0; i < Config.channels; i++) {
            blinkBooleans.add(false);
        }
    }

    /**
     * Gets an array of numbers between two integers
     * @param upperBound Largest integer
     * @param lowerBound Smallest integer
     * @param includeUpperBound Whether to include the upperBound in the result
     * @param includeLowerBound Whether to include the lowerBound in the result
     * @return Returns an array of integers
     */
    private static List<Integer> difference(int upperBound, int lowerBound, boolean includeUpperBound, boolean includeLowerBound) {
        List<Integer> numbers = new ArrayList<>();
        if (includeUpperBound) {
            upperBound++;
        }
        if (!includeLowerBound) {
            lowerBound++;
        }
        for (int i = lowerBound; i < upperBound; i++) {
            numbers.add(i);
        }
        return numbers;
    }
}
