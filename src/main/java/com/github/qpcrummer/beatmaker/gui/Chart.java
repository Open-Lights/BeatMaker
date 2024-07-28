package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.processing.BeatManager;
import com.github.qpcrummer.beatmaker.utils.ListUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.type.ImDouble;

import java.util.*;

public class Chart {
    public final List<Integer> channels = new ArrayList<>();
    public final List<ImDouble[]> timestamps = new ArrayList<>();
    private final List<Integer> solitaryConfinement = new ArrayList<>();
    private final float width;
    private final int id;
    private boolean showChannelEditPopup = false;
    private int index = 0;
    private final String title;
    private boolean immutable = false;

    public Chart(float width, int id, boolean generateEmptyTimeStamp) {
        this(width, id, generateEmptyTimeStamp, false);
    }

    public Chart(float width, int id, boolean generateEmptyTimeStamp, boolean immutable) {
        this(width, id, generateEmptyTimeStamp, immutable, "");
    }

    public Chart(float width, int id, boolean generateEmptyTimeStamp, boolean immutable, String title) {
        this.width = width;
        this.id = id;
        if (generateEmptyTimeStamp) {
            this.timestamps.add(generateEmptyImDoubleArray());
        }
        this.immutable = immutable;
        this.title = title;
    }

    // Rendering logic for the chart
    public void render(float x) {
        ImVec2 cursorScreenPos = ImGui.getCursorScreenPos();

        drawBorder(cursorScreenPos.x, cursorScreenPos.y, width + 15, ImGui.getIO().getDisplaySize().y - 6 * MainGUI.TOOLBAR_HEIGHT);

        ImGui.setCursorPosX(x);
        if (ImGui.button(immutable ? "Immutable Channel##" + this.id : "Edit Channel Info##" + this.id)) {
            if (!immutable) {
                this.showChannelEditPopup = true;
            }
        }

        ImGui.setCursorPosX(x);
        if (this.title.isEmpty() && !this.channels.isEmpty()) {
            String str = Arrays.toString(this.channels.toArray());
            if (str.length() > 21) {
                str = str.substring(0, 18) + "...";
            }
            ImGui.text(str);
        } else if (!this.title.isEmpty()) {
            ImGui.text(this.title);
        }

        ImGui.setCursorPosX(x);
        if (this.showChannelEditPopup) {
            ImGui.openPopup("Channel Editing##" + this.id);
            this.showChannelEditPopup = false;
        }

        ImGui.setCursorPosX(x);
        if (ImGui.beginPopup("Channel Editing##" + this.id)) {

            boolean reorder = false;
            ListIterator<Integer> iterator1 = this.channels.listIterator();
            while (iterator1.hasNext()) {
                int i = iterator1.next();
                if (ImGui.checkbox(String.valueOf(i), true)) {
                    Data.availableChannels.add(i);
                    iterator1.remove();
                    reorder = true;
                }
            }

            ListIterator<Integer> iterator2 = Data.availableChannels.listIterator();
            while (iterator2.hasNext()) {
                int i = iterator2.next();
                if (ImGui.checkbox(String.valueOf(i), false)) {
                    this.channels.add(i);
                    iterator2.remove();
                    reorder = true;
                }
            }

            if (reorder) {
                ListUtils.sortInt(this.channels);
                ListUtils.sortInt(Data.availableChannels);
            }

            ImGui.endPopup();
        }

        ImGui.pushItemWidth(width);

        ImVec2 pos = ImGui.getCursorScreenPos();
        drawLine(pos.x, pos.y, pos.x + width + 15, pos.y);
        ImGui.newLine();

        ImGui.popItemWidth();

        Iterator<ImDouble[]> iterator = this.timestamps.listIterator();
        int index = 0;

        ImGui.setCursorPosX(x);
        ImGui.beginChild("values##" + this.id, width + 15, ImGui.getIO().getDisplaySize().y - 8 * MainGUI.TOOLBAR_HEIGHT);
        while (iterator.hasNext()) {
            ImDouble[] doubles = iterator.next();

            if (ImGui.button("x##" + this.id + index)) {
                if (this.timestamps.size() > 1) {
                    iterator.remove();
                    continue;
                }
            }

            ImGui.sameLine();

            if (ImGui.button("+##" + this.id + index)) {
                this.solitaryConfinement.add(index + 1);
            }

            ImGui.sameLine();
            ImGui.pushItemWidth((width - 40) / 2);
            ImGui.inputDouble("##input1-" + this.id + index, doubles[0], 0, 0, "%.3f");
            ImGui.sameLine();
            ImGui.inputDouble("##input2-" + this.id + index, doubles[1], 0, 0, "%.3f");
            ImGui.popItemWidth();

            index++;
        }
        ImGui.endChild();

        if (!this.solitaryConfinement.isEmpty()) {
            for (int i : this.solitaryConfinement) {
                this.timestamps.add(i, generateEmptyImDoubleArray());
            }
            this.solitaryConfinement.clear();
        }

        ImGui.newLine();
    }

    private void drawBorder(float x, float y, float width, float height) {
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRect(x, y, x + width, y + height, ImGui.getColorU32(ImGuiCol.Border), 0, 0, 2);
    }

    private void drawLine(float x1, float y1, float x2, float y2) {
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addLine(x1, y1, x2, y2, ImGui.getColorU32(ImGuiCol.Border));
    }

    private ImDouble[] generateEmptyImDoubleArray() {
        ImDouble[] array = new ImDouble[2];
        array[0] = new ImDouble();
        array[1] = new ImDouble();
        return array;
    }

    public void removeChannel(Integer channel) {
        this.channels.remove(channel);
    }

    public void onRemoval() {
        Data.availableChannels.addAll(this.channels);
    }

    // Beat code
    public void checkForBeats(double currentPos) {
        if (this.index >= this.timestamps.size()) {
            return;
        }

        double first = this.timestamps.get(this.index)[0].get();
        if (currentPos >= first) {
            double last = this.timestamps.get(this.index)[1].get();
            double difference = last - first;

            if (last == 0 || difference < Data.MINIMUM_BEAT_LENGTH) {
                difference = 0.2;
            }

            for (int i : this.channels) {
                BeatManager.toggleLight(i, difference);
            }

            this.index++;
        }
    }

    public void resetBeats() {
        this.index = 0;
    }
}
