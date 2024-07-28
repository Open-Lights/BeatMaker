package com.github.qpcrummer.beatmaker.audio;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.MainGUI;
import com.github.qpcrummer.beatmaker.processing.BeatFile;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MusicPlayer {
    // TODO Support playing only stems
    public static StemmedAudio currentAudio;
    private static Clip clip;
    public static boolean playing;
    private static long position;

    public static void loadSong() {
        if (currentAudio != null) {
            reset();
            try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(currentAudio.fullAudioPath.toFile())) {
                clip = AudioSystem.getClip();
                clip.open(inputStream);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean play() {
        System.gc();
        if (clip == null) {
            return false;
        }
        if (!playing) {
            clip.setMicrosecondPosition(position);
            clip.start();
            playing = !playing;
            MainGUI.isPlayButtonPressed = true;
            return true;
        }
        return false;
    }

    public static void pause() {
        if (playing) {
            position = clip.getMicrosecondPosition();
            clip.stop();
            playing = !playing;
            MainGUI.isPlayButtonPressed = false;
        }
    }

    public static boolean setPosition(long microsecondPosition) {
        if (clip == null) {
            return false;
        }
        pause();
        position = microsecondPosition;
        MainGUI.time.set(BeatFile.millisecondsToSecondsFormatted(MusicPlayer.getPositionMilliseconds()));
        return true;
    }

    private static void reset() {
        Data.loadedStems.clear();
        if (clip != null) {
            clip.close();
            playing = false;
            position = 0;
        }
    }

    public static long getPositionMilliseconds() {
        if (clip != null) {
            return TimeUnit.MILLISECONDS.convert(clip.getMicrosecondPosition(), TimeUnit.MICROSECONDS);
        }
        return 0;
    }

    public static long getSongLengthMilliseconds() {
        if (clip != null) {
            return TimeUnit.MILLISECONDS.convert(clip.getMicrosecondLength(), TimeUnit.MICROSECONDS);
        }
        return 0;
    }
}
