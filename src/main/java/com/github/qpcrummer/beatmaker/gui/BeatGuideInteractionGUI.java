package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.utils.Comparer;
import com.github.qpcrummer.beatmaker.utils.ListUtils;
import imgui.ImGui;
import imgui.type.ImDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BeatGuideInteractionGUI {
    public static boolean enable;
    public static boolean removal;
    private static final List<Chart> toBeRemoved = new ArrayList<>();

    public static void render() {
        if (enable) {
            ImGui.begin("BeatGuideInteraction");

            int index = 0;
            for (Chart chart : Data.charts) {
                boolean contains = toBeRemoved.contains(chart);
                if (ImGui.checkbox(chart.getTitle(index), contains)) {
                    if (contains) {
                        toBeRemoved.remove(chart);
                    } else {
                        toBeRemoved.add(chart);
                    }
                }
                index++;
            }

            if (ImGui.button(toBeRemoved.isEmpty() ? "Close" : removal ? "Delete" : "Merge")) {
                if (!removal) {
                    List<ImDouble[]> outcome = new ArrayList<>();
                    List<Integer> channels = new ArrayList<>();

                    for (Chart chart : toBeRemoved) {
                        for (Integer channel: chart.channels) {
                            if (!channels.contains(channel)) {
                                channels.add(channel);
                            }
                        }

                        if (outcome.isEmpty()) {
                            outcome.addAll(chart.timestamps);
                        } else {
                            outcome = Comparer.removeOverlap(outcome, chart.timestamps);
                        }
                    }

                    Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
                    chart.timestamps.addAll(outcome);
                    chart.channels.addAll(channels);
                    Data.charts.add(chart);
                }

                for (Chart chart : toBeRemoved) {
                    Data.charts.remove(chart);
                }
                toBeRemoved.clear();
                ListUtils.sortInt(Data.availableChannels);

                enable = false;
            }

            ImGui.end();
        }
    }
}
