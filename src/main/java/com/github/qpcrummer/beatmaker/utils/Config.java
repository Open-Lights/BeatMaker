package com.github.qpcrummer.beatmaker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public final class Config {
    private static final Path CONFIG = Path.of("openlights/config.properties");
    final static String
            CONFIG_VERSION_KEY = "config-version",
            INSTALLATION_SHOWN_KEY = "installation_shown",
            DEMUCS_KEY = "demucs-installed";

    public static final Properties properties = new Properties();
    public static final String cfgver = "1.0";
    public static boolean demucsInstalled, installationShown;

    /**
     * Save the config
     */
    public static void saveConfig(){
        try (OutputStream output = Files.newOutputStream(CONFIG, StandardOpenOption.CREATE)) {
            fillDefaults();
            properties.store(output, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        parse();
    }

    /**
     * If the config value doesn't exist, set it to default
     */
    private static void fillDefaults() {
        checkProperty(CONFIG_VERSION_KEY, cfgver);
        checkProperty(INSTALLATION_SHOWN_KEY, "false");
        checkProperty(DEMUCS_KEY, "false");
    }

    private static void checkProperty(String key, String defaultValue) {
        if (!properties.containsKey(key)) {
            properties.setProperty(key, defaultValue);
        }
    }

    /**
     * Loads the config
     */
    public static void loadConfig() {
        if (Files.notExists(CONFIG)) {
            saveConfig();
        }

        try (InputStream input = Files.newInputStream(CONFIG)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the config to convert into Objects
     */
    public static void parse() {
        fillDefaults();
        demucsInstalled = Boolean.parseBoolean(properties.getProperty(DEMUCS_KEY));
        installationShown = Boolean.parseBoolean(properties.getProperty(INSTALLATION_SHOWN_KEY));
    }
}
