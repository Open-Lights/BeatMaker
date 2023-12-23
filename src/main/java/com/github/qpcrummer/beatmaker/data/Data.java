package com.github.qpcrummer.beatmaker.data;

import com.github.qpcrummer.beatmaker.gui.Chart;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
     * Fills the availableChannels List
     */
    public static void initialize() {
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
            availableChannels.addAll(Arrays.asList(difference(newAmount, totalChannels, true, false, 1)));
            totalChannels = newAmount;
        } else if (newAmount < totalChannels) {
            for (int n : difference(totalChannels, newAmount, true, false, 1)) {
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
     * @param includeUpper Whether to include the upperBound in the result
     * @param includeLower Whether to include the lowerBound in the result
     * @param lowerBy Lowers all values in the array be this amount
     * @return Returns an array of integers
     */
    private static Integer[] difference(int upperBound, int lowerBound, boolean includeUpper, boolean includeLower, int lowerBy) {
        int difference = upperBound - lowerBound;

        if (includeLower && includeUpper) {
            difference++;
        } else if (!includeLower && !includeUpper) {
            difference--;
        }

        Integer[] result = new Integer[difference];

        int startingPos;
        if (includeLower) {
            startingPos = lowerBound;
        } else {
            startingPos = lowerBound + 1;
        }

        int endingPos;
        if (includeUpper) {
            endingPos = upperBound + 1;
        } else {
            endingPos = upperBound;
        }

        int index = 0;
        for (int i = startingPos; i < endingPos; i++) {
            result[index] = i - lowerBy;
            index++;
        }

        return result;
    }
}
