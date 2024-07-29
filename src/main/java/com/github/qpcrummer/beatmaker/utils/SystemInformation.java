package com.github.qpcrummer.beatmaker.utils;

import org.lwjgl.system.Platform;
/*
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

 */

public class SystemInformation {
    //public final GPUVendor[] gpus;
    public final Platform operatingSystem;
    //public final float[] vram; // In GBs
    public final Platform.Architecture arch;

    public SystemInformation() {

        //SystemInfo si = new SystemInfo();
        //HardwareAbstractionLayer hal = si.getHardware();
        this.operatingSystem = Platform.get();

        /*
        int gpuCount = hal.getGraphicsCards().size();
        GPUVendor[] vendors = new GPUVendor[gpuCount];
        float[] vramTotal = new float[gpuCount];
        for (int i = 0; i < gpuCount; i++) {
            GraphicsCard card = hal.getGraphicsCards().get(i);
            GPUVendor vendor = determineVendor(card.getVendor());
            vendors[i] = vendor;
            vramTotal[i] = (float) (card.getVRam() / 1000000000.000);
        }
        this.gpus = vendors;
        this.vram = vramTotal;

         */

        this.arch = Platform.getArchitecture();
    }

    public enum GPUVendor {
        NVIDIA,
        AMD,
        INTEL,
        APPLE,
        UNKNOWN,
    }

    public int getVendorAIWeight(GPUVendor vendor) {
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

    private GPUVendor determineVendor(String vendor) {
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
}
