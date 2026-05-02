package org.softfriascorporations.avalondesktop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.avalon.desktop.config.AppModule;
import org.avalon.desktop.core.update.UpdateService;
import org.avalon.desktop.core.update.github.ReleaseInfo;
import org.avalon.desktop.ui.navigation.ViewLoader;

import java.io.IOException;
import java.util.Optional;

public class HelloApplication extends Application {
  /*  @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

   */
  @Override
  public void start(Stage primaryStage) {
      System.out.println("✅ Entrando a start()");
      Injector injector = Guice.createInjector(new AppModule());

      ViewLoader viewLoader = injector.getInstance(ViewLoader.class);
      viewLoader.setPrimaryStage(primaryStage);

      UpdateService updateService = injector.getInstance(UpdateService.class);

      // Ejecutar verificación en hilo separado para no bloquear el inicio
      new Thread(() -> {

          Optional<ReleaseInfo> newRelease = updateService.checkForUpdates();

          System.out.println("entro en modo buscar release de git ");
          Platform.runLater(() -> {
              if (newRelease.isPresent()) {
                  Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                  alert.setTitle("Actualización Disponible");
                  alert.setHeaderText("Nueva versión " + newRelease.get().tagName() + " disponible.");
                  alert.setContentText("¿Desea descargar e instalar la actualización ahora?");

                  alert.showAndWait().ifPresent(response -> {
                      if (response == ButtonType.OK) {
                          updateService.downloadAndApply(newRelease.get());
                      } else {
                          viewLoader.loadView("/views/login.fxml", "Avalon Desktop - Login");
                      }
                  });
              } else {
                  viewLoader.loadView("/views/login.fxml", "Avalon Desktop - Login");
              }
          });
      }).start();
  }

}
