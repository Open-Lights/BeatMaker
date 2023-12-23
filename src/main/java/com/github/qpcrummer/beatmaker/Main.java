package com.github.qpcrummer.beatmaker;

import com.github.qpcrummer.beatmaker.data.Data;
import com.github.qpcrummer.beatmaker.gui.*;
import com.github.qpcrummer.beatmaker.processing.BeatManager;
import imgui.app.Application;
import imgui.app.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Main extends Application {
    private long previousTime = System.currentTimeMillis();
    private int targetFrameRate = 15;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    public static final Logger logger = Logger.getLogger("Christmas Celebrator Beat Editor");

    public static void main(String[] args) {
        launch(new Main());
    }

    @Override
    protected void preRun() {
        super.preRun();
        logger.info("Loading Christmas Celebrator Song Editor");
        Data.initialize();
        BeatManager.initialize();

        Path saveDir = Path.of("saves\\");
        if (Files.notExists(saveDir)) {
            try {
                Files.createDirectory(saveDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void process() {
        MainGUI.render();
        ChannelInteractionGUI.render();
        BeatGuideInteractionGUI.render();
        FileExplorer.render();
        Recorder.render();
    }

    @Override
    protected void configure(Configuration config) {
        config.setTitle("Christmas Celebrator Song Editor");
        config.setHeight(WINDOW_HEIGHT);
        config.setWidth(WINDOW_WIDTH);
    }

    @Override
    protected void runFrame() {
        long currentTime = System.currentTimeMillis();
        double elapsedTime = currentTime - this.previousTime;

        if (elapsedTime >= targetFrameRate) {
            super.runFrame();
            this.previousTime = currentTime;
        }

    }

    @Override
    protected void disposeWindow() {
        super.disposeWindow();
        System.exit(1);
    }
}
