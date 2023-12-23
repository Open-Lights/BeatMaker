package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.data.Data;
import imgui.ImGui;
import imgui.type.ImInt;

public class ChannelInteractionGUI {
    public static boolean enable;
    private static final ImInt imint = new ImInt();

    static {
        imint.set(Data.totalChannels);
    }

    public static void render() {
        if (enable) {
            ImGui.begin("Channel Configuration");

            ImGui.inputInt("##channelAmount", imint, 0, 0);

            if (ImGui.button("Save")) {
                Data.updateChannels(imint.get());
                enable = false;
            }

            ImGui.end();
        }
    }
}
