package org.avalon.desktop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.avalon.desktop.config.AppModule;
import org.avalon.desktop.core.update.UpdateService;
import org.avalon.desktop.core.update.github.ReleaseInfo;
import org.avalon.desktop.ui.SceneManager; // Usar SceneManager para la navegación principal
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AvalonDesktopApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(AvalonDesktopApp.class);

    @Override
    public void start(Stage primaryStage) {
        // --- NUEVO FLUJO DE ACTUALIZACIÓN AL INICIO ---
        // Verificar si hay un script de actualización pendiente y ejecutarlo
        Path updateScriptPath = UpdateService.getUpdateScriptPath(); // Acceder al método estático
        if (Files.exists(updateScriptPath)) {
            try {
                logger.info("Script de actualización pendiente encontrado: {}. Ejecutando y cerrando la aplicación actual.", updateScriptPath);
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", updateScriptPath.toAbsolutePath().toString());
                pb.start();

                Platform.exit(); // Cerrar la aplicación actual para que el script tome el control
                System.exit(0);
                return; // Salir del método start()
            } catch (IOException e) {
                logger.error("Error al ejecutar el script de actualización pendiente: {}", updateScriptPath, e);
                // Si falla, la aplicación continúa con el inicio normal, pero con un error.
                // Podrías mostrar un Alert aquí si lo deseas.
            }
        }
        // --- FIN DEL NUEVO FLUJO DE ACTUALIZACIÓN AL INICIO ---


        logger.info("✅ Entrando a start()");
        Injector injector = Guice.createInjector(new AppModule());
        
        // Usar SceneManager para la navegación principal
        SceneManager sceneManager = injector.getInstance(SceneManager.class);
        sceneManager.setPrimaryStage(primaryStage);
        
        UpdateService updateService = injector.getInstance(UpdateService.class);
        
        new Thread(() -> {
            Optional<ReleaseInfo> newRelease = updateService.checkForUpdates();

            logger.info("entro en modo buscar release de git ");
            Platform.runLater(() -> {
                if (newRelease.isPresent()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Actualización Disponible");
                    alert.setHeaderText("Nueva versión " + newRelease.get().tagName() + " disponible.");
                    alert.setContentText("¿Desea descargar la actualización ahora? Se aplicará la próxima vez que inicie la aplicación.");
                    
                    Optional<ButtonType> response = alert.showAndWait();

                    if (response.isPresent() && response.get() == ButtonType.OK) {
                        CompletableFuture<Path> downloadFuture = updateService.downloadAndApply(newRelease.get());
                        downloadFuture.whenComplete((downloadedPath, throwable) -> {
                            Platform.runLater(() -> {
                                if (throwable == null) {
                                    // Descarga exitosa, crear el script de actualización
                                    try {
                                        updateService.createUpdateScript(downloadedPath);
                                        
                                        // --- MODIFICACIÓN AQUÍ: Preguntar al usuario si desea reiniciar ahora ---
                                        Alert restartAlert = new Alert(Alert.AlertType.CONFIRMATION);
                                        restartAlert.setTitle("Actualización Descargada");
                                        restartAlert.setHeaderText("Actualización lista para instalar.");
                                        restartAlert.setContentText("La nueva versión se ha descargado y se aplicará. ¿Desea reiniciar la aplicación AHORA para aplicar la actualización?");
                                        
                                        Optional<ButtonType> restartResponse = restartAlert.showAndWait();

                                        if (restartResponse.isPresent() && restartResponse.get() == ButtonType.OK) {
                                            logger.info("Usuario eligió reiniciar ahora para aplicar la actualización.");
                                            Platform.exit(); 
                                            System.exit(0);
                                        } else {
                                            logger.info("Usuario eligió reiniciar más tarde. La actualización se aplicará en el próximo inicio.");
                                            Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                                            infoAlert.setTitle("Actualización Pendiente");
                                            infoAlert.setHeaderText("Actualización programada.");
                                            infoAlert.setContentText("La actualización se aplicará la próxima vez que inicie la aplicación.");
                                            infoAlert.showAndWait();
                                        }
                                        // --- FIN MODIFICACIÓN ---

                                    } catch (IOException | URISyntaxException | IllegalStateException e) {
                                        logger.error("Error al crear el script de actualización: {}", e.getMessage(), e);
                                        Alert errorApplyAlert = new Alert(Alert.AlertType.ERROR);
                                        errorApplyAlert.setTitle("Error de Actualización");
                                        errorApplyAlert.setHeaderText("No se pudo preparar la actualización.");
                                        errorApplyAlert.setContentText("Ocurrió un error: " + e.getMessage() + "\nPor favor, contacte a soporte.");
                                        errorApplyAlert.showAndWait();
                                    }
                                } else {
                                    // Error en la descarga
                                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                    errorAlert.setTitle("Error de Descarga");
                                    errorAlert.setHeaderText("No se pudo descargar la actualización.");
                                    errorAlert.setContentText("Ocurrió un error: " + throwable.getMessage());
                                    errorAlert.showAndWait();
                                }
                            });
                        });
                    } 
                    // Mover la carga del login aquí, fuera del if/else del response, pero dentro del Platform.runLater
                    // Esto asegura que el login se carga después de que el usuario interactúa con el alert inicial
                    sceneManager.showLoginScreen(); // Usar SceneManager
                } else {
                    // No hay actualizaciones, cargar la vista de login directamente
                    sceneManager.showLoginScreen(); // Usar SceneManager
                }
            });
        }, "UpdateCheckThread").start();
    }

    public static void main(String[] args) {
        PrintStream consoleLogStream = null;
        try {
            // Configurar el directorio de logs
            Path logDir = Paths.get(System.getProperty("user.home"), ".avalon-desktop", "logs");
            Files.createDirectories(logDir); // Asegurarse de que el directorio exista

            // Redirigir System.out y System.err a un archivo
            Path consoleLogFile = logDir.resolve("console-output.log");
            consoleLogStream = new PrintStream(new FileOutputStream(consoleLogFile.toFile(), true)); // 'true' para modo append
            System.setOut(consoleLogStream);
            System.setErr(consoleLogStream);

            // Ahora, cualquier System.out.println o System.err.println irá a este archivo
            System.setProperty("jssc.tmpdir", System.getProperty("user.home") + "/AppData/Local/AvalonDesktop/jSerialComm_native");
            logger.info("🚀 Iniciando AvalonDesktop...");
            launch(args);
        } catch (Exception e) {
            // Capturar cualquier excepción que ocurra antes o durante launch()
            logger.error("❌ Error fatal en la aplicación:", e);
            // También imprimir al System.err original si es posible, o al nuevo System.err (el archivo)
            if (System.err != null) { // Asegurarse de que System.err no sea null si la redirección falló
                e.printStackTrace(System.err);
            }
        } finally {
            if (consoleLogStream != null) {
                consoleLogStream.close();
            }
        }
    }
}
