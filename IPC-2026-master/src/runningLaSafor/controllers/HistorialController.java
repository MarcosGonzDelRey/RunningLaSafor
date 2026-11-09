package runningLaSafor.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.scene.Scene;

public class HistorialController implements Initializable {

    private final SportActivityApp app = SportActivityApp.getInstance();

    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "ES"));
    private static final DateTimeFormatter DT_FMT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "ES"));

    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_W_EXPANDED = 240;
    private static final double SIDEBAR_W_COLLAPSED = 68;

    // ── Top bar ─────────────────────────────────────────────────────────
    @FXML
    private Button closeBtn;
    @FXML
    private Label dateLabel;
    @FXML
    private Label usernameLabel;
    @FXML
    private Button avatarBtn;
    @FXML
    private Button logoutBtn;

    // ── Sidebar ─────────────────────────────────────────────────────────
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

    // ── Session content ──────────────────────────────────────────────────
    @FXML
    private TableView<Session> tablaSesiones;
    @FXML
    private Label statTotalSesiones;
    @FXML
    private Label statImportadas;
    @FXML
    private Label statVistas;
    @FXML
    private Label statAnotaciones;
    @FXML
    private Label labelTotalHeader;
    @FXML
    private Label labelSumaImportadas;
    @FXML
    private Label labelSumaVistas;
    @FXML
    private Label labelSumaAnotaciones;

    // ════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        populateTopBar();
        setupTable();
        loadSessions();
        setActiveButton(historialBtn);
    }

    private void populateTopBar() {
        String formatted = LocalDate.now().format(DATE_FMT);
        dateLabel.setText(formatted.substring(0, 1).toUpperCase() + formatted.substring(1));
        User user = app.getCurrentUser();
        if (user != null) {
            usernameLabel.setText(user.getNickName());
            String initials = user.getNickName().length() >= 2
                    ? user.getNickName().substring(0, 2).toUpperCase()
                    : user.getNickName().toUpperCase();
            avatarBtn.setText(initials);
            loadAvatarIntoBtn();
        } else {
            usernameLabel.setText("—");
            avatarBtn.setText("?");
        }
    }

    private void loadAvatarIntoBtn() {
        if (avatarBtn == null) {
            return;
        }
        User user = app.getCurrentUser();
        if (user == null) {
            return;
        }
        try {
            Image img = null;
            String path = user.getAvatarPath();
            if (path != null && !path.trim().isEmpty()) {
                java.io.File f;
                try {
                    f = new java.io.File(new java.net.URI(path));
                } catch (Exception ex) {
                    f = new java.io.File(path);
                }
                if (f.exists()) {
                    img = ActividadesController.loadImageSafe(f);
                }
            }
            if (img == null) {
                java.io.InputStream s = getClass().getResourceAsStream("/runningLaSafor/images/avatar_default.png");
                if (s != null) {
                    img = ActividadesController.loadImageSafe(s);
                }
            }
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(34);
                iv.setFitHeight(34);
                iv.setPreserveRatio(false);
                iv.setClip(new Circle(17, 17, 17));
                avatarBtn.setText("");
                avatarBtn.setGraphic(iv);
            }
        } catch (Exception ignored) {
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TABLE SETUP
    // ════════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void setupTable() {
        TableColumn<Session, String> colFecha = (TableColumn<Session, String>) tablaSesiones.getColumns().get(0);
        TableColumn<Session, String> colDuracion = (TableColumn<Session, String>) tablaSesiones.getColumns().get(1);
        TableColumn<Session, Integer> colImportadas = (TableColumn<Session, Integer>) tablaSesiones.getColumns().get(2);
        TableColumn<Session, Integer> colVistas = (TableColumn<Session, Integer>) tablaSesiones.getColumns().get(3);
        TableColumn<Session, Integer> colAnotaciones = (TableColumn<Session, Integer>) tablaSesiones.getColumns().get(4);

        colFecha.setCellValueFactory(data -> {
            Session s = data.getValue();
            String text = s.getStartTime() != null ? s.getStartTime().format(DT_FMT) : "—";
            return new SimpleStringProperty(text);
        });

        colDuracion.setCellValueFactory(data -> {
            java.time.Duration d = data.getValue().getDuration();
            if (d == null) {
                return new SimpleStringProperty("—");
            }
            long secs = d.getSeconds();
            return new SimpleStringProperty(
                    String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60));
        });

        colImportadas.setCellValueFactory(data
                -> new SimpleIntegerProperty(data.getValue().getImportedActivities()).asObject());

        colVistas.setCellValueFactory(data
                -> new SimpleIntegerProperty(data.getValue().getViewedActivities()).asObject());

        colAnotaciones.setCellValueFactory(data
                -> new SimpleIntegerProperty(data.getValue().getAnnotationsCreated()).asObject());

        colFecha.setStyle("-fx-alignment: CENTER;");
        colDuracion.setStyle("-fx-alignment: CENTER;");
        colImportadas.setStyle("-fx-alignment: CENTER;");
        colVistas.setStyle("-fx-alignment: CENTER;");
        colAnotaciones.setStyle("-fx-alignment: CENTER;");

        tablaSesiones.setRowFactory(tv -> {
            TableRow<Session> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            return row;
        });

        tablaSesiones.setPlaceholder(new Label("No hay sesiones registradas."));
    }

    // ════════════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ════════════════════════════════════════════════════════════════════
    private void loadSessions() {
        List<Session> sessions;
        try {
            User user = app.getCurrentUser();
            sessions = (user != null) ? app.getSessionsByUser(user) : null;
            if (sessions == null) {
                sessions = java.util.Collections.emptyList();
            }
        } catch (Exception e) {
            System.err.println("Error loading sessions: " + e.getMessage());
            sessions = java.util.Collections.emptyList();
        }

        tablaSesiones.getItems().setAll(sessions);

        // Update placeholder to explain why it's empty
        if (sessions.isEmpty()) {
            tablaSesiones.setPlaceholder(new Label(
                    "No hay sesiones registradas aún.\n"
                    + "Las sesiones se guardan al cerrar sesión con el botón ⏏."));
        }

        int totalSesiones = sessions.size();
        int totalImportadas = sessions.stream().mapToInt(Session::getImportedActivities).sum();
        int totalVistas = sessions.stream().mapToInt(Session::getViewedActivities).sum();
        int totalAnotaciones = sessions.stream().mapToInt(Session::getAnnotationsCreated).sum();

        if (statTotalSesiones != null) {
            statTotalSesiones.setText(String.valueOf(totalSesiones));
        }
        if (statImportadas != null) {
            statImportadas.setText(String.valueOf(totalImportadas));
        }
        if (statVistas != null) {
            statVistas.setText(String.valueOf(totalVistas));
        }
        if (statAnotaciones != null) {
            statAnotaciones.setText(String.valueOf(totalAnotaciones));
        }

        if (labelSumaImportadas != null) {
            labelSumaImportadas.setText(String.valueOf(totalImportadas));
        }
        if (labelSumaVistas != null) {
            labelSumaVistas.setText(String.valueOf(totalVistas));
        }
        if (labelSumaAnotaciones != null) {
            labelSumaAnotaciones.setText(String.valueOf(totalAnotaciones));
        }

        if (labelTotalHeader != null) {
            labelTotalHeader.setText("Totales acumulados (" + totalSesiones + " sesiones)");
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
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button active) {
        Button[] all = {dashboardBtn, activitiesBtn, historialBtn, mapasBtn, estadisticasBtn, perfilBtn};
        for (Button b : all) {
            b.getStyleClass().remove("sidebar-btn-active");
        }
        active.getStyleClass().add("sidebar-btn-active");
    }

    @FXML
    private void handleDashboard() {
        navigateTo("/runningLaSafor/views/Dashboard.fxml");
    }

    @FXML
    private void handleActividades() {
        navigateTo("/runningLaSafor/views/Actividades.fxml");
    }

    @FXML
    private void handleHistorial() {
        loadSessions();
    }

    @FXML
    private void handleMapas() {
        navigateTo("/runningLaSafor/views/GestionMapas.fxml");
    }

    @FXML
    private void handleEstadisticas() {
        navigateTo("/runningLaSafor/views/EstadisticasMes.fxml");
    }

    @FXML
    private void handlePerfil() {
        navigateTo("/runningLaSafor/views/configuracion.fxml");
    }

    @FXML
    private void handleClose() {
        ((Stage) closeBtn.getScene().getWindow()).close();
    }

    @FXML
    private void handleLogout() {
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
                    Stage stage = (Stage) logoutBtn.getScene().getWindow();
                    Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
                    stage.setScene(scene);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleAvatarClick() {
        handlePerfil();
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
        node.setVisible(v);
        node.setManaged(v);
    }
}
