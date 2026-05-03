module org.avalon.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires org.slf4j;
    requires org.xerial.sqlitejdbc;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires java.naming; // Añadido para resolver ClassNotFoundException: javax.naming.NamingException
    
    // Inyección
    requires com.google.guice;
    requires jakarta.inject;

    // Librerías externas
    requires com.fazecast.jSerialComm;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    exports org.avalon.desktop;
    exports org.avalon.desktop.ui.navigation;
    exports org.avalon.desktop.ui.presentation.controller;
    exports org.avalon.desktop.config;
    exports org.avalon.desktop.core.devices;
    exports org.avalon.desktop.core.devices.printer;
    exports org.avalon.desktop.core.devices.scale;
    exports org.avalon.desktop.sales.application.service;
    exports org.avalon.desktop.product.application.service;
    exports org.avalon.desktop.core.update.github;
    exports org.avalon.desktop.core.update;
    exports org.avalon.desktop.sales.application.usecase;
    exports org.avalon.desktop.sales.domain.model;
    exports org.avalon.desktop.product.domain.model;

    // Exportaciones para los nuevos módulos de autenticación
    exports org.avalon.desktop.auth.domain.model;
    exports org.avalon.desktop.auth.domain.repository;
    exports org.avalon.desktop.auth.domain.service;
    exports org.avalon.desktop.auth.infrastructure.security; // Exportar este paquete

    opens org.avalon.desktop to com.google.guice, javafx.graphics;
    opens org.avalon.desktop.ui.navigation to com.google.guice;
    opens org.avalon.desktop.ui.presentation.controller to com.google.guice, javafx.fxml;
    opens org.avalon.desktop.config to com.google.guice;
    opens org.avalon.desktop.core.devices to com.google.guice;
    opens org.avalon.desktop.core.devices.printer to com.google.guice;
    opens org.avalon.desktop.core.devices.scale to com.google.guice;
    opens org.avalon.desktop.sales.application.service to com.google.guice;
    opens org.avalon.desktop.product.application.service to com.google.guice;
    opens org.avalon.desktop.core.update.github to com.fasterxml.jackson.databind, com.google.guice;
    opens org.avalon.desktop.core.update to com.google.guice, javafx.graphics;
    opens org.avalon.desktop.sales.application.usecase to com.google.guice;
    opens org.avalon.desktop.sales.domain.model to com.google.guice;
    opens org.avalon.desktop.product.domain.model to com.google.guice;

    opens org.avalon.desktop.auth.infrastructure.persistence.sqlite to com.google.guice;
    opens org.avalon.desktop.product.infrastructure.persistence.sqlite to com.google.guice;
    opens org.avalon.desktop.sales.infrastructure.persistence.sqlite to com.google.guice;
    
    // Abrir paquetes para JavaFX y Guice
    opens org.avalon.desktop.auth.ui to com.google.guice, javafx.fxml; // Abrir para FXML y Guice
    opens org.avalon.desktop.ui to com.google.guice; // Abrir SceneManager para Guice
}
