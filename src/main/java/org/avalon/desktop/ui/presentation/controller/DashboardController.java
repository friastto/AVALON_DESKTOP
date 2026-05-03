package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.avalon.desktop.core.update.UpdateService;
import org.avalon.desktop.ui.navigation.ViewLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private ProgressBar updateProgressBar;
    @FXML private Label lblDownloadStatus;

    private final ViewLoader viewLoader;
    private final Injector injector;
    private final UpdateService updateService; // Inyectar UpdateService
    private boolean isSidebarVisible = true;
    private static final double SIDEBAR_WIDTH = 250;

    @Inject
    public DashboardController(ViewLoader viewLoader, Injector injector, UpdateService updateService) {
        this.viewLoader = viewLoader;
        this.injector = injector;
        this.updateService = updateService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Enlazar la ProgressBar y el Label de estado a las propiedades del UpdateService
        updateProgressBar.progressProperty().bind(updateService.downloadProgressProperty());
        updateProgressBar.visibleProperty().bind(updateService.downloadActiveProperty());
        lblDownloadStatus.visibleProperty().bind(updateService.downloadActiveProperty());

        // Formatear el texto del porcentaje
        updateService.downloadProgressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 0) {
                lblDownloadStatus.setText(String.format("Descargando: %.0f%%", newVal.doubleValue() * 100));
            } else {
                lblDownloadStatus.setText("Error en descarga");
            }
        });
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
    @FXML private void showReturns() { loadInternalView("/views/returns.fxml"); }

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
