package com.github.qpcrummer.beatmaker.gui;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.utils.DemucsInstaller;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.app.Application;
import imgui.app.Configuration;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.lwjgl.opengl.GL11C;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static AtomicInteger progress = new AtomicInteger(100);
    public static AtomicReference<String> currentTask = new AtomicReference<>("Loading...");
    public static AtomicBoolean renderDropdownSelection = new AtomicBoolean(false);
    public static AtomicReference<String[]> dropdownOptions = new AtomicReference<>();
    ImInt index = new ImInt(0);
    public static AtomicInteger selectedIndex = new AtomicInteger(0);
    private static float cachedLargestDropdownWidth;

    @Override
    protected void configure(Configuration config) {
        config.setHeight(WINDOW_HEIGHT);
        config.setWidth(WINDOW_WIDTH - 20);
        config.setTitle("Installing Dependencies...");
    }

    @Override
    protected void preRun() {
        super.preRun();
        DemucsInstaller.installDependencies();
        glfwSetWindowAttrib(this.getHandle(), GLFW_RESIZABLE, 0);
        BACKGROUND_TEXTURE = loadTextureFromFile(Paths.get("src", "main", "resources", "assets", "installation_background.png"));
        LOADING_BAR_TEXTURE = loadTextureFromFile(Paths.get("src", "main", "resources", "assets", "loading_bar.png"));
    }

    @Override
    public void process() {
        renderInstallationScreen();

        if (progress.get() == 100) {
            launch(new Main());
            this.dispose();
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
        ImVec2 textDimensions = calcTextSize(currentTask.get());
        ImGui.setCursorPos((WINDOW_WIDTH - textDimensions.x) * 0.5f, WINDOW_HEIGHT * 0.65f);
        ImGui.text(currentTask.get());
        if (renderDropdownSelection.get()) {
            if (cachedLargestDropdownWidth == 0) {
                float longest = 0;
                for (String string : dropdownOptions.get()) {
                    float length = calcTextSize(string).x;
                    if (length > longest) {
                        longest = length;
                    }
                }
                cachedLargestDropdownWidth = longest + 30; // Add 30 for padding
            }
            ImGui.setCursorPos((WINDOW_WIDTH - cachedLargestDropdownWidth) * 0.5f, WINDOW_HEIGHT * 0.70f);
            ImGui.setNextItemWidth(cachedLargestDropdownWidth);
            ImGui.combo("##", index, dropdownOptions.get());
            ImVec2 buttonDimensions = calcTextSize("Confirm");
            ImGui.setCursorPos((WINDOW_WIDTH - buttonDimensions.x) * 0.5f, WINDOW_HEIGHT * 0.75f);
            ImGui.button("Confirm");
            if (ImGui.isItemClicked()) {
                selectedIndex.set(index.get());
                renderDropdownSelection.set(false);
                cachedLargestDropdownWidth = 0;
            }
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

        int imageTexture = GL11C.glGenTextures();
        glBindTexture(GL_TEXTURE_2D, imageTexture);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, imageWidth[0], imageHeight[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        stbi_image_free(imageData);

        return new int[] {imageTexture, imageWidth[0], imageHeight[0]};
    }

    private ImVec2 calcTextSize(String text) {
        ImVec2 value = new ImVec2();
        ImGui.calcTextSize(value, text);
        return value;
    }
}
