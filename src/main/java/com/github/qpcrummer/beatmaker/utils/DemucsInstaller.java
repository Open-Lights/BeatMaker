package com.github.qpcrummer.beatmaker.utils;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.gui.InstallationGUI;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.lwjgl.system.Platform;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DemucsInstaller {
    private static final Path DEPENDENCIES = Path.of("openlights/dependencies");
    private static final Path DEMUCS = Path.of("openlights/dependencies/demucs");
    private static final Path PYTHON = Path.of("openlights/dependencies/python");
    private static final Path VENV = Path.of("openlights/python-env");
    private static final Path PIP = Path.of("openlights/dependencies/python/Scripts");
    private static final String PYTHON_URL_BASE = "https://www.python.org/ftp/python/3.8.10/python-3.8.10-";
    private static final String ARM_PYTHON_URL= "https://www.python.org/ftp/python/3.11.9/python-3.11.9-";
    private static SystemInformation info;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static String cancellationError = "";

    public static void installDependencies() {
        executor.submit(() -> {
            createFolders();
            gatherSystemData();
            for (int i = 0; i < 3; i++) {
                downloadPython();
                if (verifyPythonInstallation()) {
                    break;
                } else {
                    deleteDirectory(PYTHON.toFile());
                }
                if (i == 2) {
                    System.out.println("TEST!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    cancelInstallation("Python");
                    Thread.currentThread().interrupt();
                }
            }

            for (int i = 0; i < 3; i++) {
                installPip();
                if (verifyPipInstallation()) {
                    break;
                } else {
                    deleteDirectory(PIP.toFile());
                }
                if (i == 2) {
                    cancelInstallation("Pip");
                }
            }
            //createPythonVirtualEnvironment();
        });
    }

    private static void createFolders() {
        createFolder(DEPENDENCIES, "Creating Dependencies Folder");
        createFolder(DEMUCS, "Creating Demucs Folder");
        createFolder(VENV, "Creating Python Virtual Environment Folder");

        setProgress(1);
    }

    private static void gatherSystemData() {
        setCurrentTask("Gathering System Data");
        info = new SystemInformation();
        setProgress(3);
    }

    private static void downloadPython() {
        if (Files.notExists(PYTHON)) {
            setCurrentTask("Downloading Python");
            String[] url = getAppropriatePythonURL();

            if (url == null) {
                cancellationError = "Python URL is considered null";
                cancelInstallation("Python URL");
            } else {
                Path path = Path.of(DEPENDENCIES + "/python-" + url[1]);
                downloadFile(url[0] + url[1], path, 15);

                setCurrentTask("Installing Python");
                extractFile(path, PYTHON, 20);
            }
        } else {
            setProgress(20);
        }
    }

    private static boolean verifyPythonInstallation() {
        setCurrentTask("Verifying Python Installation");
        String command = "pythonw.exe -V";
        ProcessBuilder builder = new ProcessBuilder(command).directory(PYTHON.toAbsolutePath().toFile());
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
        downloadFile("https://bootstrap.pypa.io/get-pip.py", output, 25);

        setCurrentTask("Installing Pip");
        runPipInstaller();
        setProgress(30);
    }

    private static void runPipInstaller() {
        String command = "pythonw.exe get-pip.py";
        ProcessBuilder builder = new ProcessBuilder(command).directory(PYTHON.toAbsolutePath().toFile());
        try {
            builder.start();
        } catch (IOException e) {
            Main.logger.warning("Failed to install Pip");
        }
    }

    private static void preparePythonFile() {
        File file = Path.of(PYTHON + "/python38._pth").toFile();
        try (BufferedReader reader = new BufferedReader(new FileReader(file));
             BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }

            // Modify the content (remove the last '#' and space)
            int lastIndex = content.lastIndexOf("#");
            if (lastIndex != -1) {
                content.delete(lastIndex, content.length());
            }

            writer.write(content.toString());
        } catch (IOException e) {
            Main.logger.warning("Failed to edit python38._pth");
        }
    }

    private static boolean verifyPipInstallation() {
        setCurrentTask("Verifying Pip Installation");
        String command = "pip.exe -V";
        ProcessBuilder builder = new ProcessBuilder(command).directory(PIP.toAbsolutePath().toFile());
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
                Main.logger.warning("Failed to load Pip");
            }

        } catch (IOException | InterruptedException e) {
            cancellationError = e.toString();
            Main.logger.warning("Failed to load Pip");
        }
        return true;
    }

    private static void createPythonVirtualEnvironment() {
        setCurrentTask("Creating Python Virtual Environment");
        String pythonPath = PYTHON + "/pythonw.exe";
        String createEnvCommand = pythonPath + " -m venv " + VENV.toAbsolutePath();
        ProcessBuilder builder = new ProcessBuilder().command(createEnvCommand);
        try {
            Process process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    }

    private static void downloadFile(String fileUrl, Path destination, int progressGoal) {
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
        executor.shutdownNow();
    }

    /*
    public static void installDemucs() {
        Executors.newCachedThreadPool().submit(() -> {
            // Create folder
            InstallationGUI.currentTask.set("Creating Folder");
            try {
                Files.createDirectory(DEMUCS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            InstallationGUI.progress.set(1);

            // Gathering Data
            InstallationGUI.currentTask.set("Gathering System Data");
            SystemInfo si = new SystemInfo();
            OperatingSystem os = si.getOperatingSystem();
            HardwareAbstractionLayer hal = si.getHardware();
            OS operatingSystem = determineOS(os.getFamily());

            DemucsVersion[] supportedVersions = new DemucsVersion[6];
            String gpuModel = null;
            if (operatingSystem == OS.UNKNOWN) {
                supportedVersions[0] = DemucsVersion.NONE;
            } else {
                GPUVendor gpuVendor = null;
                for (GraphicsCard card : hal.getGraphicsCards()) {
                    GPUVendor vendor = determineVendor(card.getVendor());
                    if (gpuVendor == null) {
                        gpuModel = card.getName();
                        gpuVendor = vendor;
                    } else {
                        if (getVendorAIWeight(vendor) > getVendorAIWeight(gpuVendor)) {
                            gpuVendor = vendor;
                            gpuModel = card.getName();
                        }
                    }
                }

                if (gpuVendor == null) {
                    if (operatingSystem == OS.WINDOWS) {
                        supportedVersions[0] = DemucsVersion.CPU_WINDOWS;
                    } else {
                        supportedVersions[0] = DemucsVersion.NONE;
                    }
                } else {
                    supportedVersions[getVendorAIWeight(gpuVendor)] = determineCorrectDemucsVersion(gpuVendor, gpuModel, operatingSystem);
                }
            }

            organizeDemucsVersionArray(supportedVersions);
            InstallationGUI.renderDropdownSelection.set(true);

            InstallationGUI.progress.set(5);

            // Ask the user if the version is correct
            InstallationGUI.currentTask.set("Waiting For Confirmation");

            while (InstallationGUI.renderDropdownSelection.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            InstallationGUI.currentTask.set("Downloading Demucs");
            
        });
    }

     */
    public static boolean needsInstallation() {
        return !Files.exists(DEMUCS);
    }

    /*
    private static void organizeDemucsVersionArray(DemucsVersion[] array) {
        List<String> list = new ArrayList<>();
        Collections.reverse(Arrays.asList(array));

        for (DemucsVersion ver: array) {
            if (ver != null) {
                list.add(getDemucsDescription(ver));
            }
        }

        InstallationGUI.dropdownOptions.set(list.toArray(new String[0]));
    }

    private enum DemucsVersion {
        CUDA_WINDOWS,
        CPU_WINDOWS,
        MKL_WINDOWS,
        CPU_MAC,
        MPS_MAC,
        CUDA_LINUX,
        NONE,
    }

    private static String getDemucsDescription(DemucsVersion version) {
        switch (version) {
            case CUDA_WINDOWS -> {
                return "OS: Windows; Nvidia CUDA";
            }
            case CUDA_LINUX -> {
                return "OS: Linux; AMD CUDA";
            }
            case MKL_WINDOWS -> {
                return "OS: Windows; Intel MKL";
            }
            case MPS_MAC -> {
                return "OS: MacOS; Apple Silicon MPS";
            }
            case CPU_WINDOWS -> {
                return "OS: Windows; CPU";
            }
            case CPU_MAC -> {
                return "OS: MacOS; Intel CPU";
            }
            default -> {
                return "OS: Unknown; AI Disabled";
            }
        }
    }

    private static DemucsVersion determineCorrectDemucsVersion(GPUVendor vendor, String model, OS os) {
        if (vendor == GPUVendor.NVIDIA) {
            if (os == OS.WINDOWS) {
                if (checkCudaSupport()) {
                    return DemucsVersion.CUDA_WINDOWS;
                } else {
                    return DemucsVersion.CPU_WINDOWS;
                }
            } else if (os == OS.MACOS) {
                return DemucsVersion.CPU_MAC;
            } else {
                return DemucsVersion.NONE;
            }
        } else if (vendor == GPUVendor.INTEL) {
            if (os == OS.WINDOWS) {
                if (checkIntelAISupport(model)) {
                    return DemucsVersion.MKL_WINDOWS;
                } else {
                    return DemucsVersion.CPU_WINDOWS;
                }
            } else if (os == OS.MACOS){
                return DemucsVersion.CPU_MAC;
            } else {
                return DemucsVersion.NONE;
            }
        } else if (vendor == GPUVendor.AMD) {
            if (os == OS.WINDOWS) {
                return DemucsVersion.CPU_WINDOWS;
            } else if (os == OS.LINUX) {
                // TODO Test for CUDA/ROCm support
                return DemucsVersion.CUDA_LINUX;
            } else {
                return DemucsVersion.CPU_MAC;
            }
        } else if (vendor == GPUVendor.APPLE) {
            return DemucsVersion.MPS_MAC;
        } else {
            return DemucsVersion.NONE;
        }
    }

    private static boolean checkCudaSupport() {
        try {
            ProcessBuilder builder = new ProcessBuilder().command("nvidia-smi");
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("CUDA Version")) {
                    return true;
                }
            }
        } catch (Exception e) {
            Main.logger.warning("Failed to find NVIDA software");
        }
        return false;
    }

    private static boolean checkIntelAISupport(String model) {
        return model.toLowerCase().contains("arc") || model.toLowerCase().contains("xe");
    }

     */
}
