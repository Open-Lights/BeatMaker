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
import java.util.concurrent.Executors;

public class DemucsInstaller {
    private static final Path DEMUCS = Path.of("/demucs");
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

            DemucsVersion demucs = null;
            String gpuModel = null;
            if (operatingSystem == OS.UNKNOWN) {
                demucs = DemucsVersion.NONE;
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
                    demucs = DemucsVersion.NONE;
                } else {
                    demucs = determineCorrectDemucsVersion(gpuVendor, gpuModel, operatingSystem);
                }
            }
            InstallationGUI.progress.set(5);

            // Ask the user if the version is correct
            InstallationGUI.currentTask.set("Waiting For Confirmation");
        });
    }
    public static boolean needsInstallation() {
        return !Files.exists(DEMUCS);
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
        } else if (vendor.toLowerCase().contains("amd")) {
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
