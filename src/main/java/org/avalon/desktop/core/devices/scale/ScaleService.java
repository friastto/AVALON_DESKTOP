package org.avalon.desktop.core.devices.scale;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ScaleService {
    private static final Logger logger = LoggerFactory.getLogger(ScaleService.class);
    private final StringProperty currentWeightStr = new SimpleStringProperty("Deshabilitado");
    private final DoubleProperty currentWeightValue = new SimpleDoubleProperty(0.0);
    private Object activePort = null;
    private boolean failedInitialization = false;

    public List<String> getAvailablePorts() {
        if (failedInitialization) return new ArrayList<>();
        List<String> ports = new ArrayList<>();
        try {
            com.fazecast.jSerialComm.SerialPort[] commPorts = com.fazecast.jSerialComm.SerialPort.getCommPorts();
            for (var port : commPorts) ports.add(port.getSystemPortName());
            if (currentWeightStr.get().equals("Deshabilitado")) currentWeightStr.set("0.000 kg");
        } catch (Throwable e) {
            failedInitialization = true;
            currentWeightStr.set("Error de Driver");
        }
        return ports;
    }

    public void connect(String portName) {
        if (failedInitialization) return;
        try {
            com.fazecast.jSerialComm.SerialPort port = com.fazecast.jSerialComm.SerialPort.getCommPort(portName);
            if (port.openPort()) {
                activePort = port;
                new Thread(() -> {
                    try {
                        while (port.isOpen()) {
                            if (port.bytesAvailable() > 0) {
                                byte[] buffer = new byte[port.bytesAvailable()];
                                port.readBytes(buffer, buffer.length);
                                String raw = new String(buffer).trim();
                                try {
                                    double weight = Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
                                    Platform.runLater(() -> {
                                        currentWeightValue.set(weight);
                                        currentWeightStr.set(String.format("%.3f kg", weight));
                                    });
                                } catch (Exception ignored) {}
                            }
                            Thread.sleep(200);
                        }
                    } catch (Exception ignored) {}
                }).start();
            }
        } catch (Throwable ignored) {}
    }

    public StringProperty weightStrProperty() { return currentWeightStr; }
    public DoubleProperty weightValueProperty() { return currentWeightValue; }
    public double getCurrentWeight() { return currentWeightValue.get(); }
}
