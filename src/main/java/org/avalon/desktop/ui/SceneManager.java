package org.avalon.desktop.ui;

import com.google.inject.Inject;
import com.google.inject.Injector;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.avalon.desktop.auth.domain.model.User;
import org.avalon.desktop.auth.ui.ChangeCredentialsController;
import org.avalon.desktop.ui.navigation.ViewLoader;

import java.io.IOException;
import java.util.Objects;

public class SceneManager {

    private Stage primaryStage;
    private final Injector injector;
    private final ViewLoader viewLoader;

    @Inject
    public SceneManager(Injector injector, ViewLoader viewLoader) {
        this.injector = injector;
        this.viewLoader = viewLoader;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.viewLoader.setPrimaryStage(primaryStage); // Ensure ViewLoader also has the primary stage
    }

    public void showLoginScreen() {
        // Delegate to ViewLoader for primary stage navigation
        viewLoader.loadView("/views/login.fxml", "Avalon Desktop - Iniciar Sesión");
    }

    public void showMainDashboard() {
        // Corrected FXML path: use dashboard.fxml instead of main-dashboard.fxml
        viewLoader.loadView("/views/dashboard.fxml", "Avalon Desktop - Dashboard");
    }

    public void showChangeCredentialsDialog(User user) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/views/change-credentials.fxml")));
            fxmlLoader.setControllerFactory(injector::getInstance); // Use Guice for controller injection
            Parent parent = fxmlLoader.load();

            ChangeCredentialsController controller = fxmlLoader.getController();
            controller.setUser(user);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Actualizar Credenciales");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setScene(new Scene(parent));
            dialogStage.setResizable(false);
            dialogStage.initStyle(StageStyle.UNDECORATED); // Prevent closing without action
            dialogStage.showAndWait(); // Block until credentials are updated
        } catch (IOException e) {
            System.err.println("Error loading change credentials dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
