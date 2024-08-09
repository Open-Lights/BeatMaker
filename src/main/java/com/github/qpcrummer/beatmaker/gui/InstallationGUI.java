package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.utils.DemucsInstaller;
import com.github.qpcrummer.beatmaker.utils.FileDownloadingUtils;
import com.github.qpcrummer.beatmaker.utils.GUIUtils;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

public class InstallationGUI extends Application {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private int[] BACKGROUND_TEXTURE;
    private int[] LOADING_BAR_TEXTURE;
    private final Path assets = Path.of("openlights/assets/");
    private final Path backgroundImg = Path.of("openlights/assets/installation_background.png");
    private final Path loadingBarImg = Path.of("openlights/assets/loading_bar.png");
    public static AtomicInteger progress = new AtomicInteger(0);
    public static AtomicReference<String> currentTask = new AtomicReference<>("Please Select Your Installation Method");
    public static AtomicBoolean renderDropdownSelection = new AtomicBoolean(false);
    public static AtomicReference<String[]> dropdownOptions = new AtomicReference<>();
    ImInt index = new ImInt(0);
    public static AtomicInteger selectedIndex = new AtomicInteger(0);
    private static float cachedLargestDropdownWidth;
    private boolean installingDemucs;

    @Override
    protected void configure(Configuration config) {
        config.setHeight(WINDOW_HEIGHT);
        config.setWidth(WINDOW_WIDTH - 20);
        config.setTitle("Installing Dependencies...");
    }

    @Override
    protected void preRun() {
        super.preRun();
        glfwSetWindowAttrib(this.getHandle(), GLFW_RESIZABLE, 0);
        downloadAssets();
        BACKGROUND_TEXTURE = loadTextureFromFile(backgroundImg);
        LOADING_BAR_TEXTURE = loadTextureFromFile(loadingBarImg);
    }

    private void downloadAssets() {
        if (Files.notExists(assets)) {
            try {
                Files.createDirectory(assets);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        FileDownloadingUtils.downloadFile("https://github.com/Open-Lights/BeatMaker/blob/main/src/main/resources/assets/installation_background.png?raw=true", backgroundImg);
        FileDownloadingUtils.downloadFile("https://github.com/Open-Lights/BeatMaker/blob/main/src/main/resources/assets/loading_bar.png?raw=true", loadingBarImg);
    }

    @Override
    public void process() {
        renderInstallationScreen();
    }


    @Override
    protected void endFrame() {
        super.endFrame();
        if (progress.get() == 100) {
            this.disposeWindow();
            launch(new Main());
        }
    }

    private void renderInstallationScreen() {
        ImGui.setNextWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        ImGui.setNextWindowPos(-10, 0);
        ImGui.begin("Installations", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        ImGui.setCursorPos(0, 0);
        ImGui.image(BACKGROUND_TEXTURE[0], BACKGROUND_TEXTURE[1], BACKGROUND_TEXTURE[2]);
        ImGui.setCursorPos(0, 0);
        ImGui.image(LOADING_BAR_TEXTURE[0], LOADING_BAR_TEXTURE[1] * (progress.get() / 100f), LOADING_BAR_TEXTURE[2]);
        GUIUtils.setFont(1.5f);
        ImVec2 textDimensions = GUIUtils.calcTextSize(currentTask.get());
        ImGui.setCursorPos((WINDOW_WIDTH - textDimensions.x) * 0.5f, WINDOW_HEIGHT * 0.65f);
        ImGui.text(currentTask.get());
        GUIUtils.clearFontSize();
        if (installingDemucs) {
            if (renderDropdownSelection.get()) {
                if (cachedLargestDropdownWidth == 0) {
                    float longest = 0;
                    for (String string : dropdownOptions.get()) {
                        float length = GUIUtils.calcTextSize(string).x;
                        if (length > longest) {
                            longest = length;
                        }
                    }
                    cachedLargestDropdownWidth = longest + 30; // Add 30 for padding
                }
                ImGui.setCursorPos((WINDOW_WIDTH - cachedLargestDropdownWidth) * 0.5f, WINDOW_HEIGHT * 0.70f);
                ImGui.setNextItemWidth(cachedLargestDropdownWidth);
                ImGui.combo("##", index, dropdownOptions.get());
                ImVec2 buttonDimensions = GUIUtils.calcTextSize("Confirm");
                ImGui.setCursorPos((WINDOW_WIDTH - buttonDimensions.x) * 0.5f, WINDOW_HEIGHT * 0.75f);
                ImGui.button("Confirm");
                if (ImGui.isItemClicked()) {
                    selectedIndex.set(index.get());
                    renderDropdownSelection.set(false);
                    cachedLargestDropdownWidth = 0;
                }
            }
        } else {
            GUIUtils.setFont(1.5f);
            ImVec2 installTextWidth = GUIUtils.calcTextSize("Install Demucs AI");
            ImGui.setCursorPos((WINDOW_WIDTH - installTextWidth.x) * 0.5f, WINDOW_HEIGHT * 0.70f);
            ImGui.button("Install Demucs AI");
            if (ImGui.isItemClicked()) {
                installingDemucs = true;
                DemucsInstaller.installDependencies(true);
            }
            ImVec2 skipTextWidth = GUIUtils.calcTextSize("Skip Demucs AI");
            ImGui.setCursorPos((WINDOW_WIDTH - skipTextWidth.x) * 0.5f, WINDOW_HEIGHT * 0.77f);
            ImGui.button("Skip Demucs AI");
            if (ImGui.isItemClicked()) {
                installingDemucs = true; // Not actually though
                DemucsInstaller.installDependencies(false);
            }
            GUIUtils.clearFontSize();
        }
        ImGui.end();
    }

    private int[] loadTextureFromFile(Path path) {
        int[] imageWidth = new int[] {0};
        int[] imageHeight = new int[] {0};
        CharSequence data = path.toAbsolutePath().toString();
        ByteBuffer imageData = stbi_load(data, imageWidth, imageHeight, new int[] {4}, 4);

        if (imageData == null) {
            Main.logger.warning("Image Not Found; Path: " + path);
            return new int[3];
        }

        int imageTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, imageTexture);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, imageWidth[0], imageHeight[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        stbi_image_free(imageData);

        return new int[] {imageTexture, imageWidth[0], imageHeight[0]};
    }
}
