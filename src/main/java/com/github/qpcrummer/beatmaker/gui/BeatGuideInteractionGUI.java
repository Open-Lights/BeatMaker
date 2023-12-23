package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.data.Data;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class BeatGuideInteractionGUI {
    public static boolean enable;
    private static final List<Chart> toBeRemoved = new ArrayList<>();

    public static void render() {
        if (enable) {
            ImGui.begin("BeatGuideInteraction");

            int index = 0;
            for (Chart chart : Data.charts) {
                boolean contains = toBeRemoved.contains(chart);
                if (ImGui.checkbox("chartSelected" + index, contains)) {
                    if (contains) {
                        toBeRemoved.remove(chart);
                    } else {
                        toBeRemoved.add(chart);
                    }
                }
                index++;
            }

            if (ImGui.button(toBeRemoved.isEmpty() ? "Close" : "Delete")) {
                for (Chart chart : toBeRemoved) {
                    chart.onRemoval();
                    Data.charts.remove(chart);
                }
                enable = false;
            }

            ImGui.end();
        }
    }
}
