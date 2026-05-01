package org.avalon.desktop.core.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class DownloadService {
    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

    public Path downloadFile(String fileURL, String fileName, Consumer<Double> progressCallback) throws Exception {
        URL url = new URL(fileURL);
        Path outputPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
        
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile())) {
            
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            long fileSize = url.openConnection().getContentLengthLong();

            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (fileSize > 0 && progressCallback != null) {
                    progressCallback.accept((double) totalBytesRead / fileSize);
                }
            }
            logger.info("Downloaded {} to {}", fileURL, outputPath);
            return outputPath;
        }
    }
}
