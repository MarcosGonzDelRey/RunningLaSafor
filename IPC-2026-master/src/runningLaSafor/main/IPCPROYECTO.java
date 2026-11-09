package runningLaSafor.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;

public class IPCPROYECTO extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Inicializamos la librería SportActivityApp. 
        // Esto creará el archivo 'sportactivity.db' automáticamente si no existe.
        try {
            SportActivityApp.getInstance();
        } catch (Exception e) {
            System.err.println("No se pudo inicializar la base de datos SQLite.");
            e.printStackTrace();
        }

        // 2. Cargamos el archivo FXML de la pantalla de Login.
        // Asegúrate de que el nombre del archivo coincida exactamente con el tuyo.
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/runningLaSafor/views/Login.fxml"));
        Parent root = loader.load();

        // 3. Configuramos la escena y el escenario (ventana).
        Scene scene = new Scene(root);
        stage.setTitle("Running al Safor - Inicio de Sesión");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setResizable(true); // Opcional: para que no cambien el tamaño del login
        stage.show();
    }

    public static void main(String[] args) {
        // 4. Lanzamos la aplicación JavaFX.
        launch(args);
    }
}