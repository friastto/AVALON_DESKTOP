package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;
import org.avalon.desktop.auth.domain.repository.UserRepository;
import org.avalon.desktop.ui.navigation.ViewLoader;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private VBox loginBox;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Canvas backgroundCanvas;

    private final UserRepository userRepository;
    private final ViewLoader viewLoader;
    private double time = 0;

    @Inject
    public LoginController(UserRepository userRepository, ViewLoader viewLoader) {
        this.userRepository = userRepository;
        this.viewLoader = viewLoader;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Asegurar que el canvas cambie de tamaño con la ventana
        Platform.runLater(() -> {
            backgroundCanvas.widthProperty().bind(rootPane.widthProperty());
            backgroundCanvas.heightProperty().bind(rootPane.heightProperty());
            startBackgroundAnimation();
        });
        
        // Animación de entrada del formulario
        loginBox.setOpacity(0);
        loginBox.setTranslateY(50);

        FadeTransition fade = new FadeTransition(Duration.millis(1500), loginBox);
        fade.setToValue(1);
        
        TranslateTransition translate = new TranslateTransition(Duration.millis(1200), loginBox);
        translate.setToY(0);

        fade.play();
        translate.play();
    }

    private void startBackgroundAnimation() {
        GraphicsContext gc = backgroundCanvas.getGraphicsContext2D();
        
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                time += 0.02; // Velocidad del agua
                drawWaterEffect(gc);
            }
        };
        timer.start();
    }

    private void drawWaterEffect(GraphicsContext gc) {
        double w = backgroundCanvas.getWidth();
        double h = backgroundCanvas.getHeight();

        if (w <= 0 || h <= 0) return;

        // Fondo base negro profundo
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.web("#0A0A0A"));
        gc.fillRect(0, 0, w, h);

        // Dibujar 4 capas de ondas para efecto de profundidad
        // Capa 1: Gris oscuro (Fondo)
        drawWave(gc, w, h, Color.web("#1A1A1A"), 0.4, 30, time * 0.5, h * 0.6);
        
        // Capa 2: Verde Neón sutil (Reflejo)
        drawWave(gc, w, h, Color.web("#00FF88"), 0.15, 50, time * 0.8, h * 0.65);
        
        // Capa 3: Gris medio
        drawWave(gc, w, h, Color.web("#252525"), 0.3, 40, time, h * 0.7);
        
        // Capa 4: Verde Neón más fuerte en la superficie
        drawWave(gc, w, h, Color.web("#00FF88"), 0.1, 20, time * 1.5, h * 0.75);
    }

    private void drawWave(GraphicsContext gc, double w, double h, Color color, double opacity, double amplitude, double t, double baseHeight) {
        gc.setGlobalAlpha(opacity);
        gc.beginPath();
        
        // Degradado para que el agua se pierda hacia abajo
        LinearGradient grad = new LinearGradient(0, baseHeight - amplitude, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, color),
                new Stop(1, Color.TRANSPARENT));
        gc.setFill(grad);
        
        gc.moveTo(0, h);
        for (double x = 0; x <= w; x += 5) {
            // Combinación de Senos para oleaje irregular
            double y = baseHeight + 
                       Math.sin(x * 0.005 + t) * amplitude + 
                       Math.sin(x * 0.01 - t * 0.7) * (amplitude * 0.4);
            gc.lineTo(x, y);
        }
        gc.lineTo(w, h);
        gc.closePath();
        gc.fill();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        userRepository.findByUsername(username).ifPresentOrElse(user -> {
            if (user.password().equals(password)) {
                viewLoader.loadView("/views/dashboard.fxml", "Avalon POS - Dashboard");
            } else {
                errorLabel.setText("Contraseña incorrecta");
            }
        }, () -> errorLabel.setText("Usuario no encontrado"));
    }
}
