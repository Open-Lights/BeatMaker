package com.github.qpcrummer.beatmaker.processing;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.audio.MusicPlayer;
import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.Chart;
import com.github.qpcrummer.beatmaker.gui.MainGUI;
import imgui.type.ImDouble;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class BeatFile {
    private static final String txt = ".txt";

    public static void saveAll() {
        for (Chart chart : Data.charts) {
            save(chart.timestamps, chart.channels);
        }
    }

    private static void save(List<ImDouble[]> beats, List<Integer> channels) {
        if (MusicPlayer.currentSong != null) {
            int[] array = new int[channels.size()];
            for (int i = 0; i < channels.size(); i++) {
                array[i] = channels.get(i);
            }
            String name = getSongName() + "-" + joinIntArray(array) + txt;

            try {
                writeListToFile(beats, name);
            } catch (IOException e) {
                Main.logger.warning("Failed to save beat file");
                throw new RuntimeException(e);
            }
        } else {
            Main.logger.warning("Failed to save beat file because no song is loaded");
        }
    }

    private static void writeListToFile(List<ImDouble[]> list, String filePath) throws IOException {
        Path outputPath = Paths.get(Data.savePath + "/" + filePath);

        List<String> lines = list.stream()
                .map(BeatFile::imDoubleToString)
                .collect(Collectors.toList());

        Main.logger.info("Saving file");
        Files.writeString(outputPath, String.join(System.lineSeparator(), lines));
    }

    private static String imDoubleToString(ImDouble[] imDoubles) {
        double first = imDoubles[0].get();
        double last = imDoubles[1].get();

        if (last == 0 || first + Data.MINIMUM_BEAT_LENGTH > last) {
            return String.valueOf(secondsToMicroseconds(first));
        } else {
            long[] array = new long[] {secondsToMicroseconds(first), secondsToMicroseconds(last)};
            return arrayToString(array);
        }
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
     * Converts milliseconds to seconds and formats it to 3 decimal places
     * @param milliseconds time in milliseconds
     * @return time in seconds
     */
    public static double millisecondsToSecondsFormatted(long milliseconds) {
        return (double) milliseconds / 1000.d;
    }

    /**
     * Converts microseconds to seconds and formats it to 3 decimal places
     * @param microseconds time in microseconds
     * @return time in seconds
     */
    public static double microsecondsToSecondsFormatted(long microseconds) {
        double secondsUnformatted = microseconds * 0.000001;
        return Math.round(secondsUnformatted * 1000.0) / 1000.0;
    }

    private static String arrayToString(long[] array) {
        return "[" + String.join(", ", LongStream.of(array).mapToObj(String::valueOf).toArray(String[]::new)) + "]";
    }

    private static String joinIntArray(int[] array) {
        return Arrays.stream(array)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining("_"));
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
        Data.charts.add(readBeatsFromFile(beatFile));
    }

    private static Chart readBeatsFromFile(final Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            final List<ImDouble[]> beats = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {

                if (line.contains("[")) {
                    final String[] elements = line.replaceAll("[\\[\\]]", "").split(",\\s*");

                    ImDouble first = new ImDouble();
                    first.set(microsecondsToSecondsFormatted(Long.parseLong(elements[0])));
                    ImDouble last = new ImDouble();
                    last.set(microsecondsToSecondsFormatted(Long.parseLong(elements[1])));

                    beats.add(new ImDouble[] {first, last});
                } else {
                    ImDouble first = new ImDouble();
                    first.set(microsecondsToSecondsFormatted(Long.parseLong(line)));
                    ImDouble last = new ImDouble();
                    last.set(0.000);
                    beats.add(new ImDouble[] {first, last});
                }
            }

            List<Integer> channels = extractIntList(filePath.toFile().getName());

            Chart chart = new Chart(MainGUI.CHART_WIDTH, ThreadLocalRandom.current().nextInt(), false);
            chart.timestamps.addAll(beats);
            chart.channels.addAll(channels);
            return chart;
        } catch (IOException e) {
            Main.logger.warning("Failed to read beats from File: " + filePath);
        }
        return null;
    }

    private static List<Integer> extractIntList(final String input) {
        final Pattern pattern = Pattern.compile("\\d+");
        final Matcher matcher = pattern.matcher(input);

        final ArrayList<Integer> numberList = new ArrayList<>();
        while (matcher.find()) {
            final int number = Integer.parseInt(matcher.group());

            if (Data.availableChannels.contains(number)) {
                Data.availableChannels.remove(number);
                numberList.add(number);
            } else {
                Main.logger.warning("Channel " + number + " cannot be loaded as it is already in use");
            }
        }

        return numberList;
    }
}
