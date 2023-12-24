package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.utils.ListUtils;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImDouble;
import imgui.type.ImInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EffectSelectionGUI {
    private static final String[] effects = new String[] {"Select an Effect", "Build Up", "Chase"};
    private static final ImInt imInt = new ImInt();
    private static final List<Boolean> blink = new ArrayList<>();
    private static final ImDouble beginningTime = new ImDouble();
    private static final ImDouble endingTime = new ImDouble();
    private static final ImInt repeats = new ImInt();
    private static final List<Integer> selectedChannels = new ArrayList<>();
    private static final ImBoolean inverse = new ImBoolean();
    private static boolean selectingChannels = false;
    // Modifiable
    private static final ImDouble beginningTimeMod = new ImDouble();
    private static final ImDouble endingTimeMod = new ImDouble();
    private static final ImInt repeatsMod = new ImInt();
    private static final ImBoolean inverseMod = new ImBoolean();
    private static final List<Integer> selectedChannelsMod = new ArrayList<>();

    // Animation
    private static ScheduledExecutorService executorService;
    private static int animationIndex = 0;
    private static int animationInterval;

    public static void primeGUI() {
        imInt.set(0);
        beginningTime.set(0);
        endingTime.set(10);
        repeats.set(1);
        inverse.set(false);
        selectingChannels = false;
        selectedChannels.clear();

        beginningTimeMod.set(0);
        endingTimeMod.set(10);
        repeatsMod.set(1);
        inverseMod.set(false);
        selectedChannelsMod.clear();

        blink.clear();
        for (int i = 0; i < Data.totalChannels; i++) {
            blink.add(false);
        }
    }
    public static void render() {
        if (ImGui.beginPopupModal("Effect Selection")) {

            ImGui.text("Effects");
            ImGui.combo("##Effects", imInt, effects);

            // Draw the boxes
            int columns = Math.min(Data.totalChannels, 4);
            for (Integer i = 0; i < Data.totalChannels; i++) {
                if (i % columns != 0) {
                    ImGui.sameLine();
                }

                ImGui.pushID(i);
                boolean light = false;
                if (blink.get(i)) {
                    light = true;
                    ImGui.pushStyleColor(ImGuiCol.Button, 5, 232, 24, 255);
                }
                if (ImGui.button(String.valueOf(i), 30, 30)) {
                    if (selectingChannels) {
                        if (!selectedChannelsMod.remove(i)) {
                            selectedChannelsMod.add(i);
                            blink.set(i, true);
                        } else {
                            blink.set(i, false);
                        }
                    }
                }
                if (light) {
                    ImGui.popStyleColor();
                }
                ImGui.popID();
            }

            renderCustomizations();

            if (ImGui.button("Close")) {
                if (executorService != null) {
                    executorService.shutdownNow();
                }
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();

            if (ImGui.button("Export")) {

                if (executorService != null) {
                    executorService.shutdownNow();
                }
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private static void renderCustomizations() {
        if (imInt.get() > 0) {

            ImGui.separator();

            if (ImGui.button(selectingChannels ? "Finish Selection" : "Select Channels")) {
                selectingChannels = !selectingChannels;
            }

            ImGui.pushItemWidth(100);
            ImGui.inputDouble("Beginning Time", beginningTimeMod, 0, 0, "%.3f");
            ImGui.inputDouble("Ending Time", endingTimeMod, 0, 0, "%.3f");
            ImGui.inputInt("Repeat", repeatsMod, 0, 0);
            ImGui.popItemWidth();
            ImGui.checkbox("Inverse Effect", inverseMod);
            if (ImGui.button("Save Changes")) {
                beginningTime.set(beginningTimeMod);
                endingTime.set(endingTimeMod);
                if (repeatsMod.get() < 1) {
                    repeatsMod.set(1);
                }
                repeats.set(repeatsMod);
                inverse.set(inverseMod);
                ListUtils.sortInt(selectedChannelsMod);
                selectedChannels.clear();
                selectedChannels.addAll(selectedChannelsMod);
                change();
            }

            ImGui.separator();
        }
    }

    private static void renderBuildUp() {
        if (animationIndex >= 0 && animationIndex < selectedChannels.size()) {
            blink.set(selectedChannels.get(animationIndex), true);

            if (!inverse.get()) {
                animationIndex++;
            } else {
                animationIndex--;
            }
        } else {
            Collections.fill(blink, false);

            if (!inverse.get()) {
                animationIndex = 0;
            } else {
                animationIndex = selectedChannels.size() - 1;
            }
        }
    }

    private static void renderChase() {
        if (animationIndex >= 0 && animationIndex < selectedChannels.size()) {
            blink.set(selectedChannels.get(animationIndex), true);
            if (!inverse.get()) {
                if (animationIndex > 0) {
                    blink.set(selectedChannels.get(animationIndex - 1), false);
                }
                animationIndex++;
            } else {
                if (animationIndex < selectedChannels.size() - 1) {
                    blink.set(selectedChannels.get(animationIndex + 1), false);
                }
                animationIndex--;
            }
        } else {
            Collections.fill(blink, false);

            if (!inverse.get()) {
                animationIndex = 0;
            } else {
                animationIndex = selectedChannels.size() - 1;
            }
        }
    }


    private static void startAnimation() {
        if (!selectedChannels.isEmpty()) {
            animationIndex = -1;
            // Schedule a task to update the boolean list periodically
            int animationDuration = (int) (endingTime.get() * 1000 - beginningTime.get() * 1000);
            animationInterval = (animationDuration / selectedChannels.size()) / repeats.get();

            executorService.scheduleAtFixedRate(EffectSelectionGUI::updateAnimation,
                    0, animationInterval, TimeUnit.MILLISECONDS);
        }
    }

    private static void updateAnimation() {
        switch (imInt.get()) {
            case 1 -> renderBuildUp();
            case 2 -> renderChase();
        }
    }

    private static void change() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        startAnimation();
    }

    private static void readBeats(List<Chart> charts, List<ImDouble[]> beats) {
        animationIndex = -1;
        int beatIndex = 0;
        Collections.fill(blink, false);

        for (int r = 0; r < repeats.get(); r++) {
            for (int i = 0; i < selectedChannels.size(); i++) {
                updateAnimation();

                List<Integer> indexes = new ArrayList<>();

                for (Integer integer : findTrueIndices(blink)) {
                    indexes.add(selectedChannels.indexOf(integer));
                }

                for (Integer integer : indexes) {
                    charts.get(integer).timestamps.add(beats.get(beatIndex));
                }

                beatIndex++;
            }
        }
    }

    public static List<Integer> findTrueIndices(List<Boolean> booleanList) {
        return IntStream.range(0, booleanList.size())
                .filter(booleanList::get)
                .boxed()
                .collect(Collectors.toList());
    }

    private static void exportToChart() {
        List<Chart> charts = new ArrayList<>();

        for (Integer integer : selectedChannels) {
            Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false, true);
            chart.channels.add(integer);

            charts.add(chart);
        }


    }
}
