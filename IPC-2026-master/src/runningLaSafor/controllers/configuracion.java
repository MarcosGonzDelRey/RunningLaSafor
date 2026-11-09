package runningLaSafor.controllers;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D; // Importación necesaria para el recorte
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class configuracion {

    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "ES"));

    private static final double SIDEBAR_W_EXPANDED = 240;
    private static final double SIDEBAR_W_COLLAPSED = 68;
    private boolean sidebarExpanded = true;

    // ── Top bar ────────────────────────────────────────────────────────
    @FXML
    private Button closeBtn;
    @FXML
    private Button avatarBtn;
    @FXML
    private Label dateLabel;
    @FXML
    private Label usernameLabel;

    // ── Sidebar ────────────────────────────────────────────────────────
    @FXML
    private VBox sidebar;
    @FXML
    private Button sidebarToggleBtn;
    @FXML
    private Label appNameLabel;
    @FXML
    private Label generalLabel;
    @FXML
    private Label explorarLabel;
    @FXML
    private Label usuarioLabel;
    @FXML
    private Button dashboardBtn;
    @FXML
    private Button activitiesBtn;
    @FXML
    private Button historialBtn;
    @FXML
    private Button mapasBtn;
    @FXML
    private Button estadisticasBtn;
    @FXML
    private Button perfilBtn;
    private Button logoutBtn;
    @FXML
    private Label dashboardLabel;
    @FXML
    private Label actividadesLabel;
    @FXML
    private Label historialLabel;
    @FXML
    private Label mapasLabel;
    @FXML
    private Label estadisticasLabel;
    @FXML
    private Label perfilLabel;

    // ── Profile form ───────────────────────────────────────────────────
    @FXML
    private Button btnguardar;
    @FXML
    private HBox panelcambiarcontraseña;
    @FXML
    private HBox seccioneliminar;
    @FXML
    private TextField panelnickname;
    @FXML
    private TextField correoelectronico;
    @FXML
    private DatePicker fechadenacimiento;

    private final SportActivityApp app = SportActivityApp.getInstance();
    @FXML
    private Button cerrar;
    @FXML
    private HBox logoBox;
    @FXML
    private ImageView imageViewPerfil;

    // ── Variables para la imagen ───────────────────────────────────────
    private Image imagenActual;
    private String rutaImagenNueva = null; 

    // ════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════
    public void initialize() {
        populateTopBar();

        User user = app.getCurrentUser();
        if (user == null) {
            return;
        }

        panelnickname.setText(user.getNickName());
        panelnickname.setEditable(false);
        correoelectronico.setText(user.getEmail());
        fechadenacimiento.setValue(user.getBirthDate());

        // 3. CARGA DE LA IMAGEN DE USUARIO CON CONTROL DE ERRORES EXPLÍCITO
        try {
            if (user.getAvatarPath() != null && !user.getAvatarPath().trim().isEmpty()) {
                java.io.File avatarFile;
                try { avatarFile = new java.io.File(new java.net.URI(user.getAvatarPath())); }
                catch (Exception ex) { avatarFile = new java.io.File(user.getAvatarPath()); }
                if (avatarFile.exists()) {
                    imagenActual = ActividadesController.loadImageSafe(avatarFile);
                }
            }
            if (imagenActual == null) {
                String rutaPorDefecto = "/runningLaSafor/images/avatar_default.png";
                java.io.InputStream streamImagen = getClass().getResourceAsStream(rutaPorDefecto);
                if (streamImagen == null) {
                    System.err.println("❌ ERROR: No se encuentra la imagen en la ruta de recursos: " + rutaPorDefecto);
                } else {
                    imagenActual = ActividadesController.loadImageSafe(streamImagen);
                }
            }

            // === APLICAMOS EL NUEVO RECORTE PERFECTO (MÁS GRANDE: RADIO 80.0) ===
            aplicarImagenCircular(imageViewPerfil, imagenActual, 80.0);

            // ACTUALIZAMOS LA IMAGEN DEL BOTÓN SUPERIOR
            actualizarAvatarTopBar(imagenActual);

        } catch (Exception e) {
            System.err.println("❌ Error crítico al intentar cargar la imagen:");
            e.printStackTrace();
        }
    }

    private void populateTopBar() {
        if (dateLabel != null) {
            String formatted = LocalDate.now().format(DATE_FMT);
            dateLabel.setText(formatted.substring(0, 1).toUpperCase() + formatted.substring(1));
        }
        User user = app.getCurrentUser();
        if (user != null) {
            if (usernameLabel != null) {
                usernameLabel.setText(user.getNickName());
            }
            if (avatarBtn != null) {
                String nick = user.getNickName();
                avatarBtn.setText(nick != null && nick.length() >= 2
                        ? nick.substring(0, 2).toUpperCase()
                        : (nick != null ? nick.toUpperCase() : "?"));
            }
        } else {
            if (usernameLabel != null) {
                usernameLabel.setText("—");
            }
            if (avatarBtn != null) {
                avatarBtn.setText("?");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  PROFILE ACTIONS
    // ════════════════════════════════════════════════════════════════════
    @FXML
    private void seleccionarImagen(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen de Perfil");
        
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) imageViewPerfil.getScene().getWindow();
        File archivoSeleccionado = fileChooser.showOpenDialog(stage);

        if (archivoSeleccionado != null) {
            rutaImagenNueva = archivoSeleccionado.toURI().toString();
            try {
                imagenActual = ActividadesController.loadImageSafe(archivoSeleccionado);
                
                // === APLICAMOS EL NUEVO RECORTE PERFECTO (MÁS GRANDE: RADIO 80.0) ===
                aplicarImagenCircular(imageViewPerfil, imagenActual, 80.0);
                
                actualizarAvatarTopBar(imagenActual);
            } catch (Exception e) {
                System.err.println("❌ Error al cargar imagen seleccionada: " + e.getMessage());
            }
        }
    }

    @FXML
    private void guardar(ActionEvent event) {
        User user = app.getCurrentUser();
        if (user == null) {
            return;
        }

        String nuevoCorreo = correoelectronico.getText();
        LocalDate nuevaFecha = fechadenacimiento.getValue();

        if (!User.checkEmail(nuevoCorreo)) {
            mostrarAlerta("Error de validación", "El correo no tiene un formato válido.");
            return;
        }
        if (!User.isOlderThan(nuevaFecha, 12)) {
            mostrarAlerta("Error de validación", "Debes ser mayor de 12 años.");
            return;
        }

        String rutaParaGuardar = (rutaImagenNueva != null) ? rutaImagenNueva : user.getAvatarPath();

        boolean ok = app.updateCurrentUser(nuevoCorreo, user.getPassword(), nuevaFecha, rutaParaGuardar);
        if (ok) {
            mostrarAlerta("Éxito", "Perfil actualizado correctamente.");
        } else {
            mostrarAlerta("Error", "No se pudo actualizar el perfil.");
        }
    }

    @FXML
    private void cambiarcontraseña(MouseEvent event) {
        User user = app.getCurrentUser();
        if (user == null) {
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Contraseña");
        dialog.setHeaderText("Introduce tu nueva contraseña:");

        ButtonType guardarType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarType, ButtonType.CANCEL);

        PasswordField pwd = new PasswordField();
        pwd.setPromptText("Nueva contraseña");
        VBox vbox = new VBox(pwd);
        dialog.getDialogPane().setContent(vbox);
        Platform.runLater(pwd::requestFocus);

        dialog.setResultConverter(btn -> btn == guardarType ? pwd.getText() : null);

        dialog.showAndWait().ifPresent(nuevaPass -> {
            if (User.checkPassword(nuevaPass)) {
                boolean ok = app.updateCurrentUser(
                        user.getEmail(), nuevaPass, user.getBirthDate(), user.getAvatarPath());
                if (ok) {
                    mostrarAlerta("Éxito", "Contraseña cambiada correctamente.");
                } else {
                    mostrarAlerta("Error", "No se pudo actualizar la contraseña.");
                }
            } else {
                mostrarAlerta("Error de seguridad",
                        "La contraseña debe tener entre 8 y 20 caracteres, mayúscula, minúscula, número y símbolo.");
            }
        });
    }

    @FXML
    private void eliminar(MouseEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Cuenta");
        alert.setHeaderText("¿Estás completamente seguro?");
        alert.setContentText("Esta acción es irreversible y perderás todas tus actividades.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            app.logout();
            navigateTo("/runningLaSafor/views/Login.fxml");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════════════
    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            closeBtn.getScene().setRoot(root);
        } catch (IOException e) {
            mostrarAlerta("Error de navegación", "No se pudo cargar: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void cambiaractividades() {
        navigateTo("/runningLaSafor/views/Actividades.fxml");
    }

    @FXML
    private void cambiarhistorial() {
        navigateTo("/runningLaSafor/views/Historial.fxml");
    }

    @FXML
    private void cambiarmapas() {
        navigateTo("/runningLaSafor/views/GestionMapas.fxml");
    }

    @FXML
    private void cambiarestadistica() {
        navigateTo("/runningLaSafor/views/EstadisticasMes.fxml");
    }

    @FXML
    private void cambiarmenu() {
        navigateTo("/runningLaSafor/views/Dashboard.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cerrar sesión");
        alert.setHeaderText("¿Desea cerrar sesión?");
        alert.setContentText("Se guardarán automáticamente los datos de la sesión actual.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                app.logout();
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/runningLaSafor/views/Login.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) closeBtn.getScene().getWindow(); 
                    Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
                    stage.setScene(scene);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleClose(ActionEvent event) {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }

    @FXML
    private void handleAvatarClick(ActionEvent event) {
        // Already on profile — no-op
    }

    // ════════════════════════════════════════════════════════════════════
    //  SIDEBAR TOGGLE
    // ════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSidebarToggle() {
        sidebarExpanded = !sidebarExpanded;
        double targetW = sidebarExpanded ? SIDEBAR_W_EXPANDED : SIDEBAR_W_COLLAPSED;
        new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(sidebar.prefWidthProperty(), targetW),
                new KeyValue(sidebar.minWidthProperty(), targetW))).play();

        boolean show = sidebarExpanded;
        setNodeVisible(appNameLabel, show);
        setNodeVisible(generalLabel, show);
        setNodeVisible(explorarLabel, show);
        setNodeVisible(usuarioLabel, show);
        setNodeVisible(dashboardLabel, show);
        setNodeVisible(actividadesLabel, show);
        setNodeVisible(historialLabel, show);
        setNodeVisible(mapasLabel, show);
        setNodeVisible(estadisticasLabel, show);
        setNodeVisible(perfilLabel, show);
        sidebarToggleBtn.setText(sidebarExpanded ? "◀" : "▶");
    }

    private void setNodeVisible(Node node, boolean v) {
        if (node == null) {
            return;
        }
        node.setVisible(v);
        node.setManaged(v);
    }

    // ════════════════════════════════════════════════════════════════════
    //  UTILS
    // ════════════════════════════════════════════════════════════════════
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
    
    // ════════════════════════════════════════════════════════════════════
    //  ACTUALIZAR AVATAR TOP BAR
    // ════════════════════════════════════════════════════════════════════
    private void actualizarAvatarTopBar(Image img) {
        if (avatarBtn != null && img != null) {
            ImageView iconoAvatar = new ImageView();
            
            // Aplicamos la magia para el avatar pequeñito de la barra superior (Radio de 17px)
            aplicarImagenCircular(iconoAvatar, img, 17.0);

            // Quitamos el texto de las iniciales y ponemos la imagen
            avatarBtn.setText("");
            avatarBtn.setGraphic(iconoAvatar);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════
    //  MÉTODO MÁGICO PARA RECORTAR IMÁGENES EN CÍRCULO PERFECTO
    // ════════════════════════════════════════════════════════════════════
    private void aplicarImagenCircular(ImageView imageView, Image img, double radio) {
        if (img == null) return;

        // 1. Recortamos internamente la foto original para hacerla un cuadrado perfecto centrado
        double w = img.getWidth();
        double h = img.getHeight();
        double ladoFondo = Math.min(w, h);
        double x = (w - ladoFondo) / 2;
        double y = (h - ladoFondo) / 2;
        imageView.setViewport(new Rectangle2D(x, y, ladoFondo, ladoFondo));

        // 2. Colocamos la imagen
        imageView.setImage(img);

        // 3. Ajustamos el tamaño del ImageView al diámetro que queremos
        double diametro = radio * 2;
        imageView.setFitWidth(diametro);
        imageView.setFitHeight(diametro);
        imageView.setPreserveRatio(true); // Ahora podemos usar true sin deformar la foto

        // 4. Aplicamos la máscara circular exacta
        Circle clip = new Circle(radio, radio, radio);
        imageView.setClip(clip);
    }
}