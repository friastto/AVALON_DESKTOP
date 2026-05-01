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

    opens org.avalon.desktop to com.google.guice, javafx.graphics;
    opens org.avalon.desktop.ui.navigation to com.google.guice;
    opens org.avalon.desktop.ui.presentation.controller to com.google.guice, javafx.fxml;
    opens org.avalon.desktop.config to com.google.guice;
    opens org.avalon.desktop.core.devices to com.google.guice;
    opens org.avalon.desktop.core.devices.printer to com.google.guice;
    opens org.avalon.desktop.core.devices.scale to com.google.guice;
    opens org.avalon.desktop.sales.application.service to com.google.guice;
    opens org.avalon.desktop.product.application.service to com.google.guice;
    
    // Abrir para Jackson (Reflexión)
    opens org.avalon.desktop.core.update.github to com.fasterxml.jackson.databind;
    opens org.avalon.desktop.core.update to com.google.guice;

    opens org.avalon.desktop.auth.infrastructure.persistence.sqlite to com.google.guice;
    opens org.avalon.desktop.product.infrastructure.persistence.sqlite to com.google.guice;
    opens org.avalon.desktop.sales.infrastructure.persistence.sqlite to com.google.guice;

    opens org.avalon.desktop.sales.domain.model to javafx.base;
    opens org.avalon.desktop.product.domain.model to javafx.base;
}
