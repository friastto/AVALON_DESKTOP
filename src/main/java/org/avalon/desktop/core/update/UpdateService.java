package org.avalon.desktop.core.update;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.avalon.desktop.Main;
import org.avalon.desktop.core.update.github.GithubReleaseClient;
import org.avalon.desktop.core.update.github.ReleaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class UpdateService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);

    private final String CURRENT_VERSION = "1.0.0";
    private final String REPO_OWNER = "friastto";
    private final String REPO_NAME = "AVALON_DESKTOP";
    
    private final GithubReleaseClient client;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty downloadActive = new SimpleBooleanProperty(false);

    // Ruta donde se guardará el script de actualización
    private static final Path UPDATE_SCRIPT_DIR = Paths.get(System.getProperty("user.home"), "AppData", "Local", "AvalonDesktop", "updates");
    private static final String UPDATE_SCRIPT_NAME = "apply_update.bat";

    @Inject
    public UpdateService() {
        this.client = new GithubReleaseClient(REPO_OWNER, REPO_NAME);
        // Asegurarse de que el directorio del script de actualización exista
        try {
            Files.createDirectories(UPDATE_SCRIPT_DIR);
        } catch (IOException e) {
            logger.error("No se pudo crear el directorio para el script de actualización: {}", UPDATE_SCRIPT_DIR, e);
        }
    }

    public Optional<ReleaseInfo> checkForUpdates() {
        logger.info("Verificando actualizaciones...");
        return client.getLatestRelease().filter(release -> {
            boolean newer = VersionComparator.isNewer(CURRENT_VERSION, release.tagName());
            if (newer) {
                logger.info("Nueva versión disponible: {} (actual: {})", release.tagName(), CURRENT_VERSION);
            } else {
                logger.info("No hay actualizaciones disponibles. (actual: {})", CURRENT_VERSION);
            }
            return newer;
        });
    }

    public CompletableFuture<Path> downloadAndApply(ReleaseInfo release) {
        CompletableFuture<Path> downloadFuture = new CompletableFuture<>();

        if (release.assets() == null || release.assets().isEmpty()) {
            logger.warn("No hay assets de descarga en la release.");
            downloadFuture.completeExceptionally(new IllegalStateException("No download assets found."));
            return downloadFuture;
        }
        
        ReleaseInfo.Asset asset = release.assets().get(0);
        String url = asset.browserDownloadUrl();
        String fileName = asset.name();
        
        Platform.runLater(() -> {
            downloadActive.set(true);
            downloadProgress.set(0.0);
        });

        new Thread(() -> {
            Path tempFile = null;
            try {
                HttpClient httpClient = HttpClient.newBuilder()
                                        .followRedirects(HttpClient.Redirect.ALWAYS)
                                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(url))
                                        .header("User-Agent", "AvalonDesktop-UpdateClient/1.0")
                                        .GET()
                                        .build();
                
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                
                logger.info("Código de estado HTTP de la descarga: {}", response.statusCode());

                long totalBytes = asset.size(); 
                logger.info("Tamaño esperado (del asset de GitHub): {} bytes", totalBytes);

                tempFile = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
                
                logger.info("Iniciando descarga de {} a {}", url, tempFile);

                long bytesRead = 0;
                try (InputStream is = response.body();
                     var os = Files.newOutputStream(tempFile)) {
                    
                    byte[] buffer = new byte[8192];
                    int read;
                    
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                        bytesRead += read;
                        final double progress = totalBytes > 0 ? (double) bytesRead / totalBytes : -1;
                        Platform.runLater(() -> downloadProgress.set(progress));
                    }
                }
                
                logger.info("Bytes realmente leídos durante la descarga: {} bytes", bytesRead);

                if (totalBytes > 0 && bytesRead != totalBytes) {
                    throw new IllegalStateException("Descarga incompleta: se leyeron " + bytesRead + " bytes, pero se esperaban " + totalBytes + " bytes.");
                }

                logger.info("Descarga completada. Archivo guardado en: {}", tempFile);
                Platform.runLater(() -> downloadProgress.set(1.0));
                
                downloadFuture.complete(tempFile);
                
            } catch (Exception e) {
                logger.error("Error durante la descarga de la actualización: {}", e.getMessage(), e);
                Platform.runLater(() -> downloadProgress.set(-1.0));
                downloadFuture.completeExceptionally(e);

            } finally {
                Platform.runLater(() -> downloadActive.set(false));
            }
        }, "UpdateDownloadThread").start();

        return downloadFuture;
    }

    public Path createUpdateScript(Path downloadedZipPath) throws IOException, URISyntaxException {
        logger.info("Preparando script de actualización para: {}", downloadedZipPath);

        // Obtener la ruta de la carpeta raíz de la aplicación jpackage actual
        // Si la app se ejecuta como C:\path\to\AvalonDesktopApp\app\AvalonDesktop.exe
        // Necesitamos C:\path\to\AvalonDesktopApp
        URI currentAppUri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path currentAppRootPath;

        if (currentAppUri.getScheme().equals("file")) {
            Path appPath = Paths.get(currentAppUri);
            // Subir dos niveles para llegar a la raíz de la imagen de jpackage (e.g., C:\path\to\AvalonDesktopApp)
            // Esto asume la estructura: <APP_ROOT>/app/launcher.exe
            currentAppRootPath = appPath.getParent().getParent();
        } else {
            logger.warn("La aplicación no se está ejecutando desde una imagen de jpackage. No se puede preparar el script de actualización. currentAppUri: {}", currentAppUri);
            throw new IllegalStateException("La aplicación no se está ejecutando desde una imagen de jpackage. La actualización automática no es posible.");
        }

        // Crear un directorio temporal para la descompresión
        Path tempUnzipDir = Paths.get(System.getProperty("java.io.tmpdir"), "AvalonDesktop_Update_Unzip_" + System.currentTimeMillis());
        Files.createDirectories(tempUnzipDir);

        // El nombre de la carpeta de la imagen de la aplicación dentro del ZIP (de jlinkImageName en pom.xml)
        String appImageFolderName = "AvalonDesktopApp"; // Debe coincidir con jlinkImageName en pom.xml

        // El nombre del ejecutable del lanzador (de launcher en pom.xml)
        String launcherExeName = "AvalonDesktop.exe"; // Debe coincidir con launcher en pom.xml + .exe

        // Ruta donde se guardará el script de actualización
        Path updateScriptFile = UPDATE_SCRIPT_DIR.resolve(UPDATE_SCRIPT_NAME);
        
        // Contenido del script de actualización
        String scriptContent = String.format(
            "@echo off\n" +
            "echo Aplicando actualizacion...\n" +
            "timeout /t 2 /nobreak > NUL\n" + // Esperar 2 segundos para asegurar que la app anterior se cierre
            "echo Descomprimiendo nueva version...\n" +
            "PowerShell -Command \"Expand-Archive -Path '%s' -DestinationPath '%s' -Force\"\n" + // Descomprimir el ZIP
            "echo Reemplazando archivos de la aplicacion...\n" +
            "xcopy /s /e /y \"%s\\%s\\*\" \"%s\"\n" + // Copiar el contenido descomprimido sobre la raíz de la app actual
            "echo Eliminando archivos temporales...\n" +
            "del \"%s\"\n" + // Eliminar el ZIP descargado
            "rmdir /s /q \"%s\\%s\"\n" + // Eliminar la carpeta de la app dentro del directorio temporal de descompresión
            "rmdir /s /q \"%s\"\n" + // Eliminar el directorio temporal de descompresión
            "echo Reiniciando aplicacion...\n" +
            "start \"\" \"%s\\%s\"\n" + // Reiniciar la aplicación usando el ejecutable de jpackage
            "echo Eliminando script de actualizacion...\n" +
            "del \"%s\"\n", // Eliminar el propio script
            downloadedZipPath.toAbsolutePath().toString(), // %s para Path al ZIP descargado
            tempUnzipDir.toAbsolutePath().toString(), // %s para directorio temporal de descompresión
            tempUnzipDir.toAbsolutePath().toString(), // %s para directorio temporal de descompresión
            appImageFolderName, // %s para nombre de la carpeta de la app dentro del ZIP
            currentAppRootPath.toAbsolutePath().toString(), // %s para ruta raíz de la app actual
            downloadedZipPath.toAbsolutePath().toString(), // %s para Path al ZIP descargado
            tempUnzipDir.toAbsolutePath().toString(), // %s para directorio temporal de descompresión
            appImageFolderName, // %s para nombre de la carpeta de la app dentro del ZIP
            tempUnzipDir.toAbsolutePath().toString(), // %s para directorio temporal de descompresión
            currentAppRootPath.toAbsolutePath().toString(), // %s para ruta raíz de la app actual
            launcherExeName, // %s para nombre del ejecutable del lanzador
            updateScriptFile.toAbsolutePath().toString() // %s para ruta del script de actualización
        );

        Files.writeString(updateScriptFile, scriptContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        logger.info("Script de actualización generado en: {}", updateScriptFile);
        return updateScriptFile;
    }

    public static Path getUpdateScriptPath() {
        return UPDATE_SCRIPT_DIR.resolve(UPDATE_SCRIPT_NAME);
    }

    public DoubleProperty downloadProgressProperty() { return downloadProgress; }
    public BooleanProperty downloadActiveProperty() { return downloadActive; }
}
