package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.processing.BeatFile;
import com.github.qpcrummer.beatmaker.utils.Config;
import com.github.qpcrummer.beatmaker.utils.Timer;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

import java.util.concurrent.ThreadLocalRandom;

public class Recorder {
    private static final ImString countdown = new ImString();
    private static short countdownTimer = -1;
    private static boolean decrement;
    private static boolean recording;
    private static long heldTime = -1;
    public static boolean enable;
    private static Chart recordedChart;
    private static final int MINIMUM_BEAT_LENGTH_MS = (int) (Config.minBeatLength * 100);
    public static void render() {
        if (ImGui.beginPopupModal("Beat Recorder", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {
            ImGui.setWindowSize(400f, 350f);

            ImGui.indent(17f);
            if (MusicPlayer.currentAudio == null) {
                ImGui.text("Please select a song using File -> Open WAV");
            } else {
                ImGui.text("Press the button for blinks.\nHold the button for extended holds.\nPress the button to begin.");
            }

            if (MusicPlayer.currentAudio != null) {
                ImGui.text(countdown.get());

                if (ImGui.button("Beat", 350, 150) || decrement) {
                    if (!recording) {
                        recordedChart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);

                        switch (countdownTimer) {
                            case -1 -> {
                                countdownTimer = 5;
                                Timer.wait(1000, () -> decrement = true);
                            }
                            case 0 -> {
                                countdownTimer--;
                                recording = true;
                                MusicPlayer.play();
                            }
                            default -> {
                                countdownTimer--;
                                Timer.wait(1000, () -> decrement = true);
                            }
                        }

                        decrement = false;
                        countdown.set(countdownTimer);
                    }
                }
            }

            ImGui.indent(155f);
            if (ImGui.button("Close")) {
                if (MusicPlayer.playing) {
                    MusicPlayer.pause();
                }

                ImGui.closeCurrentPopup();
            }

            // Recording
            // TODO Improve recording consistency
            if (recording) {
                if (ImGui.isItemActivated()) {
                    heldTime = MusicPlayer.getPositionMilliseconds();
                } else if (ImGui.isItemDeactivated()) {
                    long reference = MusicPlayer.getPositionMilliseconds();
                    long time = reference - heldTime;

                    if (time < MINIMUM_BEAT_LENGTH_MS) {
                        recordedChart.timestamps.add(BeatFile.convertToImDoubleArray(BeatFile.millisecondsToSecondsFormatted(heldTime), 0));
                    } else {
                        recordedChart.timestamps.add(BeatFile.convertToImDoubleArray(BeatFile.millisecondsToSecondsFormatted(heldTime), BeatFile.millisecondsToSecondsFormatted(reference)));
                    }

                    heldTime = -1;
                }

                // Completed
                if (MusicPlayer.getPositionMilliseconds() >= MusicPlayer.getSongLengthMilliseconds() || !MusicPlayer.playing) {
                    heldTime = -1;
                    recording = false;
                    Data.charts.add(recordedChart);
                    recordedChart = null;
                    countdownTimer = -1;
                    countdown.set("Recording Complete");
                    decrement = false;
                    ImGui.closeCurrentPopup();
                }
            }

            ImGui.end();
        }
    }
}
