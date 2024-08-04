package com.github.qpcrummer.beatmaker.audio;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.utils.DemucsInstaller;
import net.lingala.zip4j.util.FileUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StemmedAudio {
    public final Path audioPath;
    public final Path fullAudioPath;
    public String model;
    public final Map<StemType, String> stems = new HashMap<>();

    public StemmedAudio(Path originalAudioPath) {
        String filename = FileUtils.getFileNameWithoutExtension(originalAudioPath.getFileName().toString());
        String output = DemucsInstaller.DEMUCS + "/" + filename;
        this.audioPath = Path.of(output);
        this.fullAudioPath = originalAudioPath;
    }

    public StemmedAudio(Path audioParentPath, Path originalAudioPath) {
        this.audioPath = audioParentPath;
        this.fullAudioPath = originalAudioPath;
    }

    public enum StemType {
        MAIN_STEM,
        VOCAL_STEM,
        GUITAR_STEM,
        PIANO_STEM,
        BASS_STEM,
        PERCUSSION_STEM,
    }

    public StemType addStem(String filename, String model, boolean overwrite) {
        this.model = model;
        StemType type = determineStemType(filename);
        if (!stems.containsKey(type) || overwrite) {
            stems.put(type, filename);
            Data.loadedStems.put(type, true);
        }
        return type;
    }

    public Optional<String> getStemPath(StemType type) {
        String filename = stems.get(type);

        if (filename != null) {
            return Optional.of(audioPath.toAbsolutePath() + "/" + model + "/" + filename);
        } else {
            return Optional.empty();
        }
    }

    public boolean hasStems() {
        return !stems.isEmpty();
    }

    private StemType determineStemType(String filename) {
        if (filename.contains("-vocals")) {
            return StemType.VOCAL_STEM;
        } else  if (filename.contains("-guitar")) {
            return StemType.GUITAR_STEM;
        } else if (filename.contains("-piano")) {
            return StemType.PIANO_STEM;
        } else if (filename.contains("drums")) {
            return StemType.PERCUSSION_STEM;
        } else if (filename.contains("bass")) {
            return StemType.BASS_STEM;
        } else {
            return StemType.MAIN_STEM;
        }
    }
}
