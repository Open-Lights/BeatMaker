package com.github.qpcrummer.beatmaker.gui;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

import java.util.concurrent.atomic.AtomicInteger;

public class DemucsGUI {
    public static boolean enable;
    public static AtomicInteger progress = new AtomicInteger(0);

    public static void render() {
        if (enable) {
            ImGui.setNextWindowSize(200f, 80f);
            ImGui.begin("AI Generation Progress", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize);
            ImGui.setWindowFocus();


            ImGui.text("Generation " + progress.get() + "% Completed");
            ImGui.progressBar((float) progress.get() / 100f);

            ImGui.end();
        }
    }

    public static void reset() {
        enable = false;
        progress.set(0);
    }
}
