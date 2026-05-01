package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.avalon.desktop.ui.navigation.ViewLoader;

import java.io.IOException;

public class DashboardController {

    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;

    private final ViewLoader viewLoader;
    private final Injector injector;
    private boolean isSidebarVisible = true;
    private static final double SIDEBAR_WIDTH = 250;

    @Inject
    public DashboardController(ViewLoader viewLoader, Injector injector) {
        this.viewLoader = viewLoader;
        this.injector = injector;
    }

    @FXML
    private void toggleSidebar() {
        Timeline timeline = new Timeline();
        KeyValue kv;
        if (isSidebarVisible) kv = new KeyValue(sidebar.prefWidthProperty(), 0);
        else kv = new KeyValue(sidebar.prefWidthProperty(), SIDEBAR_WIDTH);
        
        KeyFrame kf = new KeyFrame(Duration.millis(300), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
        isSidebarVisible = !isSidebarVisible;
    }

    @FXML private void showPos() { loadInternalView("/views/pos.fxml"); }
    @FXML private void showSales() { loadInternalView("/views/sales_dashboard.fxml"); }
    @FXML private void showInventory() { loadInternalView("/views/inventory.fxml"); }
    @FXML private void showDevices() { loadInternalView("/views/devices.fxml"); }

    @FXML
    private void handleLogout() {
        viewLoader.loadView("/views/login.fxml", "Avalon Desktop - Login");
    }

    private void loadInternalView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(injector::getInstance);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
