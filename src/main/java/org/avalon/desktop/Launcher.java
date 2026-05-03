package org.avalon.desktop;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        System.out.println("se inicio la app de escritorio con JAVA_FX");
        Application.launch(AvalonDesktopApp.class, args);
    }
}
