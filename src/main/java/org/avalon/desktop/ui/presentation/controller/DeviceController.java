package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.avalon.desktop.core.devices.DeviceManager;

import java.net.URL;
import java.util.ResourceBundle;

public class DeviceController implements Initializable {

    @FXML private ComboBox<String> printerCombo;
    @FXML private ComboBox<String> portCombo;
    @FXML private Label lblWeight;

    private final DeviceManager deviceManager;

    @Inject
    public DeviceController(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshDevices();
        
        // CORRECCIÓN: Usar weightStrProperty() en lugar de weightProperty()
        lblWeight.textProperty().bind(deviceManager.getScaleService().weightStrProperty());
    }

    @FXML
    private void refreshDevices() {
        printerCombo.getItems().setAll(deviceManager.getPrinterService().getAvailablePrinters());
        portCombo.getItems().setAll(deviceManager.getScaleService().getAvailablePorts());
    }

    @FXML
    private void connectScale() {
        String selectedPort = portCombo.getSelectionModel().getSelectedItem();
        if (selectedPort != null) {
            deviceManager.getScaleService().connect(selectedPort);
        }
    }

    @FXML
    private void testPrint() {
        String printer = printerCombo.getSelectionModel().getSelectedItem();
        if (printer != null) {
            deviceManager.getPrinterService().printText(printer, "AVALON DESKTOP\nPrueba de Impresión OK\n------------------\n");
        }
    }
}
