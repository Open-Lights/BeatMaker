package com.github.qpcrummer.beatmaker.data;

import com.github.qpcrummer.beatmaker.gui.Chart;
import imgui.type.ImDouble;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Data {
    /**
     * The total channels available on your setup to control
     * Default: 16
     */
    public static int totalChannels = 16;

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
     * The path where all beat files are saved
     */
    public static final Path savePath = Paths.get("saves");

    /**
     * Minimum beat length in seconds
     */
    public static final double MINIMUM_BEAT_LENGTH = 0.2;

    /**
     * Time intervals to set
     */
    public static final ImDouble[] timeIntervals = new ImDouble[] {new ImDouble(), new ImDouble()};

    /**
     * Whether to use the time intervals
     */
    public static boolean doIntervals;

    /**
     * Fills the availableChannels List
     */
    public static void initialize() {
        timeIntervals[0].set(0.000);
        timeIntervals[1].set(0.000);
        for (int i = 0; i < totalChannels; i++) {
            availableChannels.add(i);
            blinkBooleans.add(false);
        }
    }

    /**
     * Sets the total number of channels available to control
     * Default: 16
     * @param newAmount New total
     */
    public static void updateChannels(int newAmount) {
        if (newAmount > totalChannels) {
            availableChannels.addAll(difference(newAmount - 1, totalChannels - 1, true, false));
            totalChannels = newAmount;
        } else if (newAmount < totalChannels) {
            for (Integer n : difference(totalChannels - 1, newAmount - 1, true, false)) {
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

            totalChannels = newAmount;
        }

        blinkBooleans.clear();
        for (int i = 0; i < totalChannels; i++) {
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
