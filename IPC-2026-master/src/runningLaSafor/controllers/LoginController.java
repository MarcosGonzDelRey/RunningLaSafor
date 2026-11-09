package runningLaSafor.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Hyperlink;

// Imports correctos de tu librería
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class LoginController implements Initializable {

    @FXML private TextField nickField;
    @FXML private PasswordField passField;

    private SportActivityApp app;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            // Inicializamos la conexión con la base de datos SQLite
            app = SportActivityApp.getInstance();
        } catch (Exception e) {
            mostrarAlerta("Error Crítico", "No se pudo conectar con la base de datos.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Este método debe estar enlazado al On Action del botón de login en Scene Builder
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String nick = nickField.getText();
        String pass = passField.getText();

        // 1. Verificación simple de que los campos no estén vacíos
        if (nick.isEmpty() || pass.isEmpty()) {
            mostrarAlerta("Campos vacíos", "Introduce tu nickname y contraseña.", Alert.AlertType.WARNING);
            return;
        }

        // 2. Ejecutar el login (Escenario 3.2)
        // El método .login de SportActivityApp devuelve true si nick y pass coinciden
        boolean loginCorrecto = app.login(nick, pass);

        if (loginCorrecto) {
            // Juan ha entrado con éxito. Obtenemos el usuario para el mensaje.
            User usuarioSesion = app.getCurrentUser();
            
            System.out.println("Login exitoso: " + usuarioSesion.getNickName());
            
            // 3. Cambiar de ventana (Navegar a la aplicación principal)
            irAMain(event);
        } else {
            // Fallo de credenciales
            mostrarAlerta("Error de Autenticación", "Nickname o contraseña incorrectos.", Alert.AlertType.ERROR);
            passField.clear(); // Limpiamos la contraseña por seguridad
        }
    }

    /**
     * Método para cambiar de escena tras el éxito
     */
    private void irAMain(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/runningLaSafor/views/Dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            mostrarAlerta("Error de Navegación", "No se pudo cargar la vista principal.", Alert.AlertType.ERROR);
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
    public void handleGoToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/runningLaSafor/views/Register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Error al cargar la ventana de Registro");
            e.printStackTrace();
        }
    }
}