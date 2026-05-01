package org.avalon.desktop.core.devices.printer;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PrinterService {

    public List<String> getAvailablePrinters() {
        List<String> printers = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            printers.add(service.getName());
        }
        return printers;
    }

    public void printText(String printerName, String text) {
        try {
            PrintService selectedService = null;
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) {
                if (service.getName().equals(printerName)) {
                    selectedService = service;
                    break;
                }
            }

            if (selectedService != null) {
                InputStream is = new ByteArrayInputStream(text.getBytes());
                DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
                Doc doc = new SimpleDoc(is, flavor, null);
                DocPrintJob job = selectedService.createPrintJob();
                job.print(doc, new HashPrintRequestAttributeSet());
            }
        } catch (PrintException e) {
            e.printStackTrace();
        }
    }
}
