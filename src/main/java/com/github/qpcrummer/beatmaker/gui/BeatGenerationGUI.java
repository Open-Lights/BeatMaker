package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.processing.Generator;
import imgui.ImGui;
import imgui.type.ImDouble;
import imgui.type.ImInt;

public class BeatGenerationGUI {
    public static boolean enable;
    private static final ImInt percussionSensitivity = new ImInt();
    private static final ImDouble percussionThreshold = new ImDouble();
    private static final ImDouble complexPeakThreshold = new ImDouble();
    private static final ImDouble complexMinimumInterOnsetInterval = new ImDouble();
    private static final ImDouble complexSilenceThreshold = new ImDouble();

    static {
        percussionSensitivity.set(20);
        percussionThreshold.set(8.0);
        complexPeakThreshold.set(0.3);
        complexMinimumInterOnsetInterval.set(0.03);
        complexSilenceThreshold.set(-70.0);
    }

    public static void render() {
        if (enable) {
            ImGui.begin("Channel Configuration");

            ImGui.inputInt("Percussion Sensitivity", percussionSensitivity, 0, 0);
            ImGui.inputDouble("Percussion Threshold", percussionThreshold, 0, 0);
            ImGui.inputDouble("Complex Peak Threshold", complexPeakThreshold, 0, 0);
            ImGui.inputDouble("Complex Minimum Inter Onset Interval", complexMinimumInterOnsetInterval, 0, 0);
            ImGui.inputDouble("Complex Silence Threshold", complexSilenceThreshold, 0, 0);

            if (ImGui.button("Save")) {
                Generator.percussionSensitivity.set(percussionSensitivity);
                Generator.percussionThreshold.set(percussionThreshold);
                Generator.complexPeakThreshold.set(complexPeakThreshold);
                Generator.complexMinimumInterOnsetInterval.set(complexMinimumInterOnsetInterval);
                Generator.complexSilenceThreshold.set(complexSilenceThreshold);
                enable = false;
            }
            ImGui.sameLine();
            if (ImGui.button("Defaults")) {
                Generator.defaults();
            }

            ImGui.end();
        }
    }
}
