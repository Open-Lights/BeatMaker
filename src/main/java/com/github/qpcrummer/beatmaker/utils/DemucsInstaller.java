package com.github.qpcrummer.beatmaker.utils;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.gui.InstallationGUI;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.lwjgl.system.Platform;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DemucsInstaller {
    private static final Path DEPENDENCIES = Path.of("openlights/dependencies");
    public static final Path DEMUCS = Path.of("openlights/dependencies/demucs");
    public static final Path PYTHON = Path.of("openlights/dependencies/python");
    private static final Path PIP = Path.of("openlights/dependencies/python/Scripts");
    private static final Path FFMPEG = Path.of("openlights/dependencies/python/ffmpeg/");
    private static final String PYTHON_URL_BASE = "https://www.python.org/ftp/python/3.8.10/python-3.8.10-";
    private static final String ARM_PYTHON_URL= "https://www.python.org/ftp/python/3.11.9/python-3.11.9-";
    private static SystemInformation info;
    private static String cancellationError = "";

    public static void installDependencies(boolean installingDemucs) {
        new Thread(() -> {
            if (installingDemucs) {
                createFolders();
                gatherSystemData();
                for (int i = 0; true; i++) {
                    if (downloadPython() && verifyPythonInstallation()) {
                        break;
                    } else {
                        deleteDirectory(PYTHON.toFile());
                    }

                    if (i == 2) {
                        cancelInstallation("Python");
                        return;
                    }
                }

                for (int i = 0; true; i++) {
                    installPip();
                    if (verifyPipInstallation()) {
                        break;
                    } else {
                        deleteDirectory(PIP.toFile());
                    }
                    if (i == 2) {
                        cancelInstallation("Pip");
                        return;
                    }
                }

                downloadFFMPEG();
                installPyTorch();
                installPySoundFile();
                installDemucs();
                Config.demucsInstalled = true;
            }
            finishUp();
        }).start();
    }

    private static void createFolders() {
        createFolder(DEPENDENCIES, "Creating Dependencies Folder");
        createFolder(DEMUCS, "Creating Demucs Folder");

        setProgress(1);
    }

    private static void gatherSystemData() {
        setCurrentTask("Gathering System Data");
        info = new SystemInformation();
        setProgress(3);
    }

    private static boolean downloadPython() {
        if (Files.notExists(PYTHON)) {
            setCurrentTask("Downloading Python");
            String[] url = getAppropriatePythonURL();

            if (url == null) {
                cancellationError = "Python URL is considered null";
                return false;
            } else {
                Path path = Path.of(DEPENDENCIES + "/python-" + url[1]);
                downloadFile(url[0] + url[1], path, 15);

                setCurrentTask("Installing Python");
                extractFile(path, PYTHON, 20);
            }
        } else {
            setProgress(20);
        }
        return true;
    }

    private static boolean verifyPythonInstallation() {
        setCurrentTask("Verifying Python Installation");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "python.exe", "-V").directory(PYTHON.toAbsolutePath().toFile()).redirectErrorStream(true);
        try {
            Process process = builder.start();

            // Read the output of the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Main.logger.info(line + " has loaded properly");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error: Process exited with error code " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            cancellationError = e.toString();
            Main.logger.warning("Error loading Python; Attempting Reinstallation");
            return false;
        }
        return true;
    }

    private static void installPip() {
        setCurrentTask("Preparing Python For Pip");
        preparePythonFile();
        setProgress(21);

        setCurrentTask("Downloading Pip Installer");
        Path output = Path.of(PYTHON + "/get-pip.py");
        if (Files.notExists(output)) {
            downloadFile("https://bootstrap.pypa.io/get-pip.py", output, 25);
        }

        setCurrentTask("Installing Pip");
        runPipInstaller();
        setProgress(30);
    }

    private static void runPipInstaller() {
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "python.exe", "get-pip.py").directory(PYTHON.toAbsolutePath().toFile()).redirectErrorStream(true);
        try {
            Process process = builder.start();

            // Read the output of the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Main.logger.warning("Failed to load Pip. Exit code: " + exitCode);
            }
        } catch (IOException e) {
            Main.logger.warning("Error starting the process: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Main.logger.warning("Process interrupted: " + e.getMessage());
        }
    }

    private static void preparePythonFile() {
        File file = Path.of(PYTHON.toAbsolutePath() + "/python38._pth").toFile();

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (lineNumber == 5 && line.startsWith("#")) {
                    line = line.substring(1); // Remove the first character if it is '#'
                }
                content.append(line).append(System.lineSeparator());
                lineNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean verifyPipInstallation() {
        setCurrentTask("Verifying Pip Installation");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "pip.exe", "-V").directory(PIP.toAbsolutePath().toFile());
        try {
            Process process = builder.start();

            // Read the output of the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Main.logger.info(line + " has loaded properly");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Main.logger.warning("Failed to load Pip");
            }

        } catch (IOException | InterruptedException e) {
            cancellationError = e.toString();
            Main.logger.warning("Failed to load Pip");
        }
        return true;
    }

    private static void downloadFFMPEG() {
        setCurrentTask("Downloading FFMPEG");
        Path output = Path.of(PYTHON + "/ffmpeg.zip");
        downloadFile("https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip", output, 33);
        setCurrentTask("Installing FFMPEG");
        extractFile(output, PYTHON, 35);
        for (File file : PYTHON.toFile().listFiles()) {
            if (file.getName().contains("ffmpeg")) {
                File renamed = FFMPEG.toFile();
                file.renameTo(renamed);
                try {
                    if (Files.notExists(Path.of(PYTHON + "/ffmpeg.exe"))) {
                        Files.move(Path.of(renamed.getAbsolutePath() + "/bin/ffmpeg.exe"), Path.of(PYTHON + "/ffmpeg.exe"));
                    }
                    if (Files.notExists(Path.of(PYTHON + "/ffplay.exe"))) {
                        Files.move(Path.of(renamed.getAbsolutePath() + "/bin/ffplay.exe"), Path.of(PYTHON + "/ffplay.exe"));
                    }
                    if (Files.notExists(Path.of(PYTHON + "/ffprobe.exe"))) {
                        Files.move(Path.of(renamed.getAbsolutePath() + "/bin/ffprobe.exe"), Path.of(PYTHON + "/ffprobe.exe"));
                    }
                    deleteDirectory(renamed);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
    }

    private static void installPySoundFile() {
        installPackageWithPip("PySoundFile", pipProcess("PySoundFile"),  3, 6, 0, 45);
    }

    private static void installPyTorch() {
        if (supportsCUDA()) {
            installPackageWithPip("PyTorch with CUDA 11.8", pipProcess("torch==2.1.2+cu118", "-f", "https://download.pytorch.org/whl/torch_stable.html"),  2, 2, 0, 60);
            installPackageWithPip("PyTorch Audio", pipProcess("torchaudio==2.1.2+cu118", "-f", "https://download.pytorch.org/whl/torch_stable.html"),  2, 1, 0, 65);
        }
    }

    private static void installDemucs() {
        installPackageWithPip("Demucs", pipProcess("demucs"),  30, 55, 11, 90);
    }

    private static ProcessBuilder pipProcess(String pkg, String... args) {
        List<String> command = new ArrayList<>();
        command.add("cmd");
        command.add("/c");
        command.add("pip.exe");
        command.add("install");
        command.add(pkg);
        command.addAll(Arrays.asList(args));

        return new ProcessBuilder(command).directory(PIP.toAbsolutePath().toFile());
    }

    private static void installPackageWithPip(String pkg, ProcessBuilder builder, int packagesCollected, int packagesDownloaded, int packagesBuilt, int finalGoal) {
        setCurrentTask("Installing Package: " + pkg);
        resetDownloadProgress();
        int currentGap = finalGoal - InstallationGUI.progress.get();
        int preGoal = Math.round(finalGoal - (currentGap * 0.5f));
        int denominator = packagesBuilt + packagesDownloaded;
        try {
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean cached = false;
            int progressCounter = 0;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("Using cached")) {
                    cached = true;
                }

                if (cached) {
                    if (line.contains("Collecting")) {
                        progressCounter++;
                    }
                    relayDownloadProgress(preGoal, (float) progressCounter / packagesCollected);
                } else {
                    if (line.contains("Downloading")) {
                        progressCounter++;
                    } else if (line.contains("Building")) {
                        progressCounter++;
                    }
                    relayDownloadProgress(preGoal, (float) progressCounter / denominator);
                }

                if (line.contains("Installing collected packages")) {
                    relayDownloadProgress(finalGoal, 0.5f);
                } else if (line.contains("Successfully installed")) {
                    relayDownloadProgress(finalGoal, 1.0f);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Main.logger.warning("Failed to install " + pkg);
            }

        } catch (IOException | InterruptedException e) {
            cancellationError = e.toString();
            Main.logger.warning("Failed to install " + pkg);
        }
    }

    private static void finishUp() {
        setCurrentTask("Preparing Open Lights Beat Editor");
        Config.installationShown = true;
        Config.saveConfig(false);
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        setProgress(100);
    }

    private static void createFolder(Path path, String taskName) {
        setCurrentTask(taskName);
        if (Files.notExists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String[] getAppropriatePythonURL() {
        switch (info.operatingSystem) {
            case Platform.LINUX -> {
                // TODO Figure out how to install Python
                return null;
            }
            case Platform.WINDOWS -> {
                switch (info.arch) {
                    case X64 -> {
                        return new String[] {PYTHON_URL_BASE, "embed-amd64.zip"};
                    }
                    case X86 -> {
                        return new String[] {PYTHON_URL_BASE, "embed-win32.zip"};
                    }
                    case ARM64 -> {
                        return new String[] {ARM_PYTHON_URL, "embed-arm64.zip"};
                    }
                    default -> {
                        return null;
                    }
                }
            }
            case Platform.MACOSX -> {
                switch (info.arch) {
                    case X64, ARM32 -> {
                        return null;
                    }
                    case X86 -> {
                        return new String[] {PYTHON_URL_BASE, "macosx10.9.pkg"};
                    }
                    case ARM64 -> {
                        return new String[] {PYTHON_URL_BASE, "macos11.pkg"};
                    }
                }
            }
        }
        return null;
    }

    private static boolean supportsCUDA() {
        for (int i = 0; i < info.gpus.length; i++) {
            if (info.gpus[i] == SystemInformation.GPUVendor.NVIDIA && info.vram[i] >= 2) {
                return true;
            }
        }
        return false;
    }

    private static void setCurrentTask(String task) {
        InstallationGUI.currentTask.set(task);
    }

    private static void setProgress(int i) {
        InstallationGUI.progress.set(i);
    }

    private static int gap;
    private static void relayDownloadProgress(int goal, float currentProgress) {
        int pastProgress = InstallationGUI.progress.get();
        if (gap == 0) {
            gap = goal - pastProgress;
        }
        int newProgress = Math.round(goal - (gap * (1 - currentProgress)));
        InstallationGUI.progress.set(Math.max(newProgress, pastProgress));

        if (newProgress == goal) {
            gap = 0;
        }
    }

    private static void resetDownloadProgress() {
        gap = 0;
    }

    private static void downloadFile(String fileUrl, Path destination, int progressGoal) {
        resetDownloadProgress();
        try {
            URL url = URI.create(fileUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            long completeFileSize = connection.getContentLengthLong();
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            FileOutputStream fileOut = new FileOutputStream(destination.toFile());
            BufferedOutputStream out = new BufferedOutputStream(fileOut, 1024);
            byte[] data = new byte[1024];
            long downloadedFileSize = 0;
            int x;

            while ((x = in.read(data, 0, 1024)) >= 0) {
                downloadedFileSize += x;
                float progress = (float) downloadedFileSize / completeFileSize;
                relayDownloadProgress(progressGoal, progress);
                out.write(data, 0, x);
            }

            out.close();
            in.close();
        } catch (IOException e) {
            Main.logger.warning("Failed to download file");
        }
    }

    private static void extractFile(Path input, Path output, int progressGoal) {
        resetDownloadProgress();
        try (ZipFile zip = new ZipFile(input.toFile())) {
            ProgressMonitor progressMonitor = zip.getProgressMonitor();
            zip.setRunInThread(true);
            zip.extractAll(output.toAbsolutePath().toString());

            while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
                relayDownloadProgress(progressGoal, progressMonitor.getPercentDone() / 100f);
                Thread.sleep(20);
            }

            Files.deleteIfExists(input);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private static void cancelInstallation(String reason) {
        setCurrentTask("Canceled Installation Due To " + reason);
        Main.logger.warning(cancellationError);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        setProgress(100);
    }

    public static boolean needsInstallation() {
        if (!Config.installationShown) {
            return true;
        }

        return Config.demucsInstalled && (
                Files.notExists(DEMUCS) ||
                        Files.notExists(DEPENDENCIES) ||
                        Files.notExists(PYTHON) ||
                        Files.notExists(PIP) ||
                        Files.notExists(Path.of(PYTHON + "/python.exe")) ||
                        Files.notExists(Path.of(PIP + "/pip.exe")) ||
                        Files.notExists(Path.of(PYTHON + "/Lib/site-packages/demucs"))
                );
    }
}
