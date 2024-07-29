package com.github.qpcrummer.beatmaker.utils;

import com.github.qpcrummer.beatmaker.Main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileDownloadingUtils {
    public static void downloadFile(String fileUrl, Path destination) {
        if (Files.notExists(destination)) {
            try {
                URL url = URI.create(fileUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                FileOutputStream fileOut = new FileOutputStream(destination.toFile());
                BufferedOutputStream out = new BufferedOutputStream(fileOut, 1024);
                byte[] data = new byte[1024];
                int x;

                while ((x = in.read(data, 0, 1024)) >= 0) {
                    out.write(data, 0, x);
                }

                out.close();
                in.close();
            } catch (IOException e) {
                Main.logger.warning("Failed to download file");
            }
        } else {
            Main.logger.info("Not downloading " + fileUrl + " as it already exists");
        }
    }
}
