package com.github.qpcrummer.beatmaker.utils;

import imgui.ImGui;
import imgui.ImVec2;

public final class GUIUtils {
    /**
     * Sets the font size. Make sure you run clearFontSize afterward!
     * @param fontSize float font size (default 1.0f)
     */
    public static void setFont(final float fontSize) {
        ImGui.getFont().setScale(fontSize);
        ImGui.pushFont(ImGui.getFont());
    }

    /**
     * Resets the font size and clears older font changes
     */
    public static void clearFontSize() {
        ImGui.getFont().setScale(1.0f);
        ImGui.popFont();
    }

    /**
     * Calculates the height and width of the text
     * @param text String to measure
     * @return ImVec2 of the String
     */
    public static ImVec2 calcTextSize(String text) {
        ImVec2 value = new ImVec2();
        ImGui.calcTextSize(value, text);
        return value;
    }
}
