package org.avalon.desktop.ui.navigation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton // <--- AÑADE ESTO
public class ViewLoader {
    private static final Logger logger = LoggerFactory.getLogger(ViewLoader.class);
    private final Injector injector;
    private Stage primaryStage;

    @Inject
    public ViewLoader(Injector injector) {
        this.injector = injector;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void loadView(String fxmlPath, String title) {
        if (primaryStage == null) {
            logger.error("PrimaryStage is null in ViewLoader! Cannot load view: {}", fxmlPath);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(injector::getInstance);
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
            logger.info("Loaded view: {}", fxmlPath);
        } catch (IOException e) {
            logger.error("Could not load FXML: " + fxmlPath, e);
        }
    }
}
