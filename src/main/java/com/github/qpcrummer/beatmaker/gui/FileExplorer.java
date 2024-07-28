package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.audio.StemmedAudio;
import imgui.ImGui;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import com.github.qpcrummer.beatmaker.processing.BeatFile;

import java.io.File;
import java.nio.file.Path;

public class FileExplorer {
    private static boolean beatFileMode;
    private static final String folder = " (Folder)";
    private static File currentDirectory = new File(System.getProperty("user.dir"));
    private static File[] files;
    public static boolean enabled;

    static {
        updateFiles();
    }

    // Selected file
    private static File selectedFile;

    private static void updateFiles() {
        files = currentDirectory.listFiles();
    }

    public static void render() {
        if (enabled) {
            // Start a new frame
            ImGui.begin("File Explorer");

            // Back button
            if (ImGui.button("Back")) {
                if (currentDirectory.getParentFile() != null) {
                    // Go back one directory
                    currentDirectory = currentDirectory.getParentFile();
                    updateFiles();
                } else {
                    // Display drives if at the root level
                    File[] drives = File.listRoots();
                    if (drives != null) {
                        files = drives;
                    }
                }
            }

            // Display the current directory
            ImGui.sameLine();
            ImGui.text("Current Directory: " + currentDirectory.getAbsolutePath());

            // List the files and directories in the current directory
            if (ImGui.treeNodeEx("Files", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (files != null) {
                    for (File file : files) {
                        // Display selectable file names
                        String name = file.getName();

                        if (name.isBlank()) {
                            name = file.getPath();
                        } else if (file.isDirectory()) {
                            name = name + folder;
                        }

                        ImGui.selectable(name, selectedFile != null && selectedFile.equals(file),
                                ImGuiSelectableFlags.SpanAllColumns);

                        // Check if the item is selected and a directory
                        if (ImGui.isItemClicked(0) && file.isDirectory()) {
                            // Change directory if a directory is clicked
                            currentDirectory = file;
                            updateFiles();
                        }

                        // Check if the item is selected and not a directory
                        if (ImGui.isItemClicked(0) && !file.isDirectory()) {
                            // Update the selected file
                            selectedFile = file;
                        }
                    }
                }

                ImGui.treePop();
            }

            // Open and Cancel buttons
            if (ImGui.button("Open")) {
                // Handle opening the selected file
                if (selectedFile != null && selectedFile.isFile()) {
                    Path path = selectedFile.toPath();
                    String fileExtension = getFileExtension(path.getFileName().toString());

                    if (beatFileMode && fileExtension.equals("json")) {
                        Main.logger.info("Uploading beat file: " + path);
                        BeatFile.loadBeatFile(path);
                        reset();
                    } else if (!beatFileMode && fileExtension.equals("wav")) {
                        Main.logger.info("Uploading wav file: " + path);
                        MusicPlayer.currentAudio = new StemmedAudio(path);
                        MusicPlayer.loadSong();
                        reset();
                    } else {
                        Main.logger.warning("Unrecognized file type: " + fileExtension);
                    }
                }
            }

            ImGui.sameLine();

            if (ImGui.button("Cancel")) {
                reset();
            }

            // End the frame
            ImGui.end();
        }
    }

    /**
     * Sets whether to select BeatFiles or WAV Files for songs
     * @param beatFileMode if choosing BeatFiles
     */
    public static void setFileExplorerType(boolean beatFileMode) {
        FileExplorer.beatFileMode = beatFileMode;
    }

    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            // Return the substring after the last dot
            return fileName.substring(lastDotIndex + 1);
        }
        // No dot or dot at the beginning (hidden files)
        return "";
    }

    private static void reset() {
        enabled = false;
        selectedFile = null;
        Main.logger.info("Hiding File Explorer GUI");
    }
}
