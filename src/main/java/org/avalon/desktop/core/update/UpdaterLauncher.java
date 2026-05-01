package org.avalon.desktop.core.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class UpdaterLauncher {
    private static final Logger logger = LoggerFactory.getLogger(UpdaterLauncher.class);

    public void launchUpdater(Path downloadedUpdaterPath, Path mainAppPath) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        
        // Obtener la ruta del JAR actual de la aplicación principal
        String currentJarPath = new File(mainAppPath.toUri()).getAbsolutePath();
        
        ProcessBuilder pb = new ProcessBuilder(
            javaBin,
            "-jar",
            downloadedUpdaterPath.toAbsolutePath().toString(),
            currentJarPath // Argumento 1: Ruta del JAR de la aplicación principal
        );
        
        pb.inheritIO(); // Redirigir la salida del updater a la consola actual
        Process p = pb.start();
        logger.info("Updater launched: {}", downloadedUpdaterPath);
    }
}
