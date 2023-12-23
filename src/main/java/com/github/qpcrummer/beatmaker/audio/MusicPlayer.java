package com.github.qpcrummer.beatmaker.audio;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class MusicPlayer {
    public static Path currentSong;
    private static Clip clip;
    public static boolean playing;
    private static long position;
    public static void loadSong() {
        if (currentSong != null) {
            reset();
            try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(currentSong.toFile())) {
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
            return true;
        }
        return false;
    }

    public static void pause() {
        if (playing) {
            position = clip.getMicrosecondPosition();
            clip.stop();
            playing = !playing;
        }
    }

    public static boolean setPosition(long microsecondPosition) {
        if (clip == null) {
            return false;
        }
        pause();
        position = microsecondPosition;
        return true;
    }

    private static void reset() {
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

    public static int getSongLengthSec() {
        if (clip != null) {
            return (int) TimeUnit.SECONDS.convert(clip.getMicrosecondLength(), TimeUnit.MICROSECONDS);
        }
        return 0;
    }
}
