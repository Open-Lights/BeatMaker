package com.github.qpcrummer.beatmaker.utils;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.audio.StemmedAudio;
import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.DemucsGUI;
import com.github.qpcrummer.beatmaker.processing.Generator;
import net.lingala.zip4j.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DemucsUtils {
    private static final ExecutorService downloader = Executors.newSingleThreadExecutor();
    public static void createStems(StemmedAudio stemmedAudio, boolean sixChannels) {
        if (stemmedAudio!= null && stemmedAudio.hasStems()) {
            return;
        }

        Path audioPath = stemmedAudio.fullAudioPath;

        if (isDemucsEnabled()) {
            String model;
            if (sixChannels) {
                model = "htdemucs_6s";
            } else {
                model = "htdemucs";
            }

            String filename = FileUtils.getFileNameWithoutExtension(audioPath.getFileName().toString());
            String output = DemucsInstaller.DEMUCS.toAbsolutePath() + "/" + filename;

            downloader.execute(() -> asyncStemCreation(audioPath, model, output));
        }
    }

    private static void asyncStemCreation(Path audioPath, String model, String output) {
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "python.exe", "-m", "demucs", audioPath.toAbsolutePath().toString(), "-n", model, "-o", output, "--filename", "{track}-{stem}.{ext}").directory(DemucsInstaller.PYTHON.toAbsolutePath().toFile()).redirectErrorStream(true);

        try {
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Main.logger.info(line);
                Pattern pattern = Pattern.compile("\\d+\\.?\\d*%");
                Matcher matcher = pattern.matcher(line);

                while (matcher.find()) {
                    String match = matcher.group();
                    String number = match.substring(0, match.length() - 1);
                    int value = Integer.parseInt(number);
                    DemucsGUI.progress.set(value);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: Process exited with error code " + exitCode);
            }
            DemucsGUI.reset();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        StemmedAudio audio = new StemmedAudio(Path.of(output), audioPath);
        File parentDir = new File(output + "/" + model + "/");
        for (File stem : parentDir.listFiles()) {
            StemmedAudio.StemType type = audio.addStem(stem.getName(), model, false);
            Generator.generateWithOnsetExtractor(stemToString(type), Path.of(audio.getStemPath(type).get()));
        }
        Generator.generateWithBeatExtractor("BPM");
        MusicPlayer.currentAudio = audio;
    }

    public static boolean allStemsEnabled() {
        return Data.loadedStems.values().stream().allMatch(Boolean::booleanValue);
    }

    public static String stemToString(StemmedAudio.StemType type) {
        String output;
        switch (type) {
            case BASS_STEM -> output = "Bass";
            case PERCUSSION_STEM -> output = "Drums";
            case PIANO_STEM -> output = "Piano";
            case GUITAR_STEM -> output = "Guitar";
            case VOCAL_STEM -> output = "Vocal";
            case null, default -> output = "Other";
        }
        return output;
    }

    public static boolean isDemucsEnabled() {
        return Config.demucsInstalled;
    }
}
