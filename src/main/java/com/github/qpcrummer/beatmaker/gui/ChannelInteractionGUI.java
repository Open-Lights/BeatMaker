package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.utils.Config;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;

public class ChannelInteractionGUI {
    private static final ImInt imint = new ImInt();

    static {
        imint.set(Config.channels);
    }

    public static void render() {
        if (ImGui.beginPopupModal("Channel Configuration", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {
            ImGui.setWindowSize(200f, 80f);
            ImGui.pushItemWidth(50f);
            ImGui.inputInt("Total Channels", imint, 0, 0);
            ImGui.popItemWidth();

            if (ImGui.button("Save")) {
                Data.updateChannels(imint.get());
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }
}
