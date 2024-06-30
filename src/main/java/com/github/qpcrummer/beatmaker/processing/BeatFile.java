package com.github.qpcrummer.beatmaker.processing;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.Chart;
import com.github.qpcrummer.beatmaker.gui.MainGUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import imgui.type.ImDouble;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BeatFile {
    private static final byte BLINK = 0;
    private static final byte ON = 1;
    private static final byte OFF = 2;

    public static void saveAll() {
        Map<String, SortedMap<Integer, Byte>> data = new HashMap<>();

        for (Chart chart : Data.charts) {
            data.put(intArrayToString(integerArrayToIntArray(chart.channels.toArray(new Integer[0]))), decodeTimeStamps(chart.timestamps));
        }

        save(data);
    }

    private static int[] integerArrayToIntArray(Integer[] array) {
        return Arrays.stream(array).mapToInt(Integer::intValue).toArray();
    }

    private static String intArrayToString(int[] array) {
        return Arrays.stream(array)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private static SortedMap<Integer, Byte> decodeTimeStamps(List<ImDouble[]> beats) {
        SortedMap<Integer, Byte> data = new TreeMap<>();
        for (ImDouble[] timestamp: beats) {
            double first = timestamp[0].get();
            double last = timestamp[1].get();

            if (last == 0 || first + Data.MINIMUM_BEAT_LENGTH > last) {
                data.put(secondsToMilliseconds(first), BLINK);
            } else {
                data.put(secondsToMilliseconds(first), ON);
                data.put(secondsToMilliseconds(last), OFF);
            }
        }
        return data;
    }

    private static void save(Map<String, SortedMap<Integer, Byte>> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String path = Data.savePath + "/" + String.format("%s.json", getSongName());
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Main.logger.info("Data saved as a json");
    }

    /**
     * Converts seconds to milliseconds
     * @param seconds time in seconds
     * @return time in milliseconds
     */
    public static long secondsToMicroseconds(double seconds) {
        return (long) (seconds * 1000000);
    }

    /**
     * Converts seconds to milliseconds
     * @param seconds time in seconds
     * @return time in milliseconds
     */
    public static int secondsToMilliseconds(double seconds) {
        return (int) (seconds * 1000);
    }

    /**
     * Converts milliseconds to seconds and formats it to 3 decimal places
     * @param milliseconds time in milliseconds
     * @return time in seconds
     */
    public static double millisecondsToSecondsFormatted(long milliseconds) {
        return (double) milliseconds / 1000.d;
    }

    private static String getSongName() {
        return MusicPlayer.currentSong.getFileName().toString().replace(".wav", "");
    }

    /**
     * Converts two doubles into a ImDouble array
     * @param first smallest double
     * @param last largest double
     * @return ImDouble array
     */
    public static ImDouble[] convertToImDoubleArray(double first, double last) {
        ImDouble imFirst = new ImDouble();
        imFirst.set(first);
        ImDouble imLast = new ImDouble();
        imLast.set(last);
        return new ImDouble[] {imFirst, imLast};
    }

    public static void loadBeatFile(Path beatFile) {
        Data.charts.addAll(readBeatsFromJson(beatFile));
    }

    private static Integer[] fromString(String string) {
        return Arrays.stream(string.split(",\\s*"))
                .map(Integer::valueOf)
                .toArray(Integer[]::new);
    }

    private static List<Chart> readBeatsFromJson(Path filePath) {
        List<Chart> charts = new ArrayList<>();
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath.toString())) {
            Type type = new TypeToken<Map<String, SortedMap<Integer, Byte>>>() {}.getType();
            Map<String, SortedMap<Integer, Byte>> deserializedData = gson.fromJson(reader, type);

            ImDouble heldBeats = new ImDouble();

            for (String channels : deserializedData.keySet()) {
                Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
                SortedMap<Integer, Byte> data = deserializedData.get(channels);
                List<ImDouble[]> imDoubles = new ArrayList<>();

                for (int timestamp: data.keySet()) {
                    switch (data.get(timestamp)) {
                        case 0 -> imDoubles.add(new ImDouble[] {new ImDouble(millisecondsToSecondsFormatted(timestamp)), new ImDouble(0.000)});
                        case 1 -> heldBeats = new ImDouble(millisecondsToSecondsFormatted(timestamp));
                        case 2 -> imDoubles.add(new ImDouble[] {heldBeats, new ImDouble(millisecondsToSecondsFormatted(timestamp))});
                    }
                }
                chart.timestamps.addAll(imDoubles);
                chart.channels.addAll(List.of(fromString(channels)));
                charts.add(chart);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return charts;
    }
}
