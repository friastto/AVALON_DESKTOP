package org.avalon.desktop.core.devices;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.avalon.desktop.core.devices.printer.PrinterService;
import org.avalon.desktop.core.devices.scale.ScaleService;

@Singleton
public class DeviceManager {
    private final PrinterService printerService;
    private final ScaleService scaleService;

    @Inject
    public DeviceManager(PrinterService printerService, ScaleService scaleService) {
        this.printerService = printerService;
        this.scaleService = scaleService;
    }

    public PrinterService getPrinterService() { return printerService; }
    public ScaleService getScaleService() { return scaleService; }
}
