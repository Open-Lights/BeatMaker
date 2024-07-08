package com.github.qpcrummer.beatmaker.utils;

import com.github.qpcrummer.beatmaker.Main;
import com.github.qpcrummer.beatmaker.gui.InstallationGUI;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class DemucsInstaller {
    private static final Path DEMUCS = Path.of("demucs");
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
    public static boolean needsInstallation() {
        return !Files.exists(DEMUCS);
    }

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

    private enum GPUVendor {
        NVIDIA,
        AMD,
        INTEL,
        APPLE,
        UNKNOWN,
    }

    private static int getVendorAIWeight(GPUVendor vendor) {
        switch (vendor) {
            case NVIDIA -> {
                return 5;
            }
            case APPLE -> {
                return 4;
            }
            case INTEL -> {
                return 3;
            }
            case AMD -> {
                return 2;
            }
            case UNKNOWN -> {
                return 1;
            }
            default -> {
                return 0;
            }
        }
    }

    private enum OS {
        LINUX,
        WINDOWS,
        MACOS,
        UNKNOWN,
    }

    private static GPUVendor determineVendor(String vendor) {
        if (vendor.toLowerCase().contains("nvidia")) {
            return GPUVendor.NVIDIA;
        } else if (vendor.toLowerCase().contains("advanced micro devices")) {
            return GPUVendor.AMD;
        } else if (vendor.toLowerCase().contains("intel")) {
            return GPUVendor.INTEL;
        } else if (vendor.toLowerCase().contains("apple")) {
            return GPUVendor.APPLE;
        } else {
            return GPUVendor.UNKNOWN;
        }
    }

    private static OS determineOS(String operatingSystem) {
        if (operatingSystem.toLowerCase().contains("windows")) {
            return OS.WINDOWS;
        } else if (operatingSystem.contains("nix")) {
            return OS.LINUX;
        } else if (operatingSystem.toLowerCase().contains("mac")) {
            return OS.MACOS;
        } else {
            return OS.UNKNOWN;
        }
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
}
