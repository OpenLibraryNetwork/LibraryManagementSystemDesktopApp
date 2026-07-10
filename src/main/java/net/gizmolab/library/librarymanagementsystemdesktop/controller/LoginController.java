package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("prototype")
public class LoginController extends BaseController {

    @Autowired private AuthService authService;

    @FXML private TextField urlField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loginProgress;

    private Runnable onLoginSuccess;

    public void initialize() {
        urlField.setText("http://localhost:1337");
        loginButton.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        loginProgress.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String url = urlField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Συμπληρώστε όλα τα πεδία");
            errorLabel.setVisible(true);
            return;
        }

        loginButton.setDisable(true);
        loginProgress.setVisible(true);
        errorLabel.setVisible(false);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.login(url, username, password);
            }
        };

        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            loginProgress.setVisible(false);
            if (task.getValue()) {
                if (onLoginSuccess != null) onLoginSuccess.run();
            } else {
                errorLabel.setText("Λάθος στοιχεία σύνδεσης");
                errorLabel.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            loginProgress.setVisible(false);
            errorLabel.setText("Σφάλμα σύνδεσης: " + task.getException().getMessage());
            errorLabel.setVisible(true);
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }
}
