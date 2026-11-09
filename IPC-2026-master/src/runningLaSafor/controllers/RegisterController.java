package runningLaSafor.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

// Imports de tu librería IPC
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class RegisterController implements Initializable {

    @FXML private TextField nickField;
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private DatePicker birthDatePicker;

    private SportActivityApp app;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Obtenemos la instancia de la base de datos
            app = SportActivityApp.getInstance();
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo conectar con la base de datos.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Lógica del Escenario 3.1: Registrarse en la aplicación
     */
    @FXML
    private void handleRegister(ActionEvent event) {
        String nick = nickField.getText();
        String email = emailField.getText();
        String pass = passField.getText();
        LocalDate birth = birthDatePicker.getValue();

        // 1. VALIDACIONES (Nota de implementación: usar métodos estáticos de User)
        
        if (!User.checkNickName(nick)) {
            mostrarAlerta("Nickname inválido", 
                "Debe tener entre 6 y 15 caracteres (letras, dígitos, guion o subguion).", 
                Alert.AlertType.WARNING);
            return;
        }

        if (!User.checkEmail(email)) {
            mostrarAlerta("Email inválido", 
                "El formato del correo electrónico no es correcto.", 
                Alert.AlertType.WARNING);
            return;
        }

        if (!User.checkPassword(pass)) {
            mostrarAlerta("Contraseña inválida", 
                "Debe tener 8-20 caracteres, una mayúscula, una minúscula, un dígito y un símbolo (!@#$%&*()-+=).", 
                Alert.AlertType.WARNING);
            return;
        }

        if (birth == null || !User.isOlderThan(birth, 12)) {
            mostrarAlerta("Edad insuficiente", 
                "Debes tener al menos 12 años para registrarte.", 
                Alert.AlertType.WARNING);
            return;
        }

        // 2. REGISTRO EN LA BASE DE DATOS
        // Juan no sube avatar, por lo que pasamos null como String (avatarPath)
        // El método registerUser devuelve false si el nickname ya existe (Unique constraint)
        boolean registrado = app.registerUser(nick, email, pass, birth, (String) null);

        if (registrado) {
            mostrarAlerta("Éxito", "Usuario '" + nick + "' registrado correctamente.", Alert.AlertType.INFORMATION);
            // Tras el éxito, volvemos a la pantalla de Login
            volverAlLogin(event);
        } else {
            // Escenario Juan: "jgarcia" ya está en uso
            mostrarAlerta("Nickname en uso", 
                "El nickname '" + nick + "' ya está siendo utilizado por otro usuario.", 
                Alert.AlertType.ERROR);
        }
    }

    /**
     * Cambia la ventana actual para volver al Login
     */
    @FXML
    private void volverAlLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/runningLaSafor/views/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}