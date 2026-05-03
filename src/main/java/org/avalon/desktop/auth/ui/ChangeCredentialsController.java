package org.avalon.desktop.auth.ui;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.avalon.desktop.auth.domain.model.User;
import org.avalon.desktop.auth.domain.repository.UserRepository;
import org.avalon.desktop.auth.domain.service.PasswordHasher;
import org.avalon.desktop.ui.SceneManager; // Assuming a SceneManager for navigation

public class ChangeCredentialsController {

    @FXML
    private TextField newUsernameField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorMessageLabel;
    @FXML
    private Button updateButton;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SceneManager sceneManager; // For navigation
    private User currentUser; // The user whose credentials need to be updated

    @Inject
    public ChangeCredentialsController(UserRepository userRepository, PasswordHasher passwordHasher, SceneManager sceneManager) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.sceneManager = sceneManager;
    }

    public void setUser(User user) {
        this.currentUser = user;
        newUsernameField.setText(user.username());
    }

    @FXML
    private void handleUpdateCredentials() {
        errorMessageLabel.setText(""); // Clear previous errors

        String newUsername = newUsernameField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newUsername.isEmpty()) {
            errorMessageLabel.setText("El nombre de usuario no puede estar vacío.");
            return;
        }
        if (newPassword.isEmpty()) {
            errorMessageLabel.setText("La contraseña no puede estar vacía.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            errorMessageLabel.setText("Las contraseñas no coinciden.");
            return;
        }
        if (newPassword.length() < 6) { // Basic password strength check
            errorMessageLabel.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        // Hash the new password
        String hashedPassword = passwordHasher.hashPassword(newPassword);

        // Update the user object
        User updatedUser = currentUser
                .withUsernameAndPassword(newUsername, hashedPassword)
                .withIsFirstLogin(false); // Mark as not first login

        try {
            userRepository.updateUserCredentials(updatedUser);
            // Close the current stage (change credentials window)
            Stage stage = (Stage) updateButton.getScene().getWindow();
            stage.close();

            // Navigate to the main dashboard
            sceneManager.showMainDashboard(); // Assuming this method exists in SceneManager

        } catch (Exception e) {
            errorMessageLabel.setText("Error al actualizar las credenciales: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
