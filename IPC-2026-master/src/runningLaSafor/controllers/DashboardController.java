package runningLaSafor.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;

import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

/**
 * Controller for Dashboard.fxml Manages the main dashboard view of the Running
 * Safor application.
 */
public class DashboardController implements Initializable {

    // ─── Backend singleton ───────────────────────────────────────────────
    private final SportActivityApp app = SportActivityApp.getInstance();

    // ─── FXML injected nodes ─────────────────────────────────────────────
    // Top bar
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

    // Sidebar
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

    // Sidebar nav buttons
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

    // Sidebar text labels (hidden when collapsed)
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

    // Dashboard stats
    @FXML
    private Label greetingLabel;
    @FXML
    private Label distanciaLabel;
    @FXML
    private Label tiempoLabel;
    @FXML
    private Label ascensoLabel;

    // Activities section
    @FXML
    private VBox activitiesContainer;
    @FXML
    private Label noActivitiesLabel;

    // ─── Internal state ──────────────────────────────────────────────────
    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_EXPANDED_WIDTH = 240.0;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 68.0;

    // ─── DateTimeFormatter ───────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "ES"));
    @FXML
    private HBox logoBox;
    @FXML
    private VBox mainContent;

    // ═════════════════════════════════════════════════════════════════════
    //  INITIALIZER
    // ═════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateTopBar();
        populateDashboardStats();
        populateRecentActivities();
    }

    // ─── Top bar ─────────────────────────────────────────────────────────
    private void populateTopBar() {
        // Date
        String formattedDate = LocalDate.now()
                .format(DATE_FORMATTER);
        // Capitalise first letter
        dateLabel.setText(formattedDate.substring(0, 1).toUpperCase()
                + formattedDate.substring(1));

        // User info
        User user = app.getCurrentUser();
        if (user != null) {
            usernameLabel.setText(user.getNickName());
            // Show initials on avatar button if no image
            String initials = user.getNickName().length() >= 2
                    ? user.getNickName().substring(0, 2).toUpperCase()
                    : user.getNickName().toUpperCase();
            avatarBtn.setText(initials);
            loadAvatarIntoBtn();
            greetingLabel.setText("Hola de nuevo, " + user.getNickName() + " 👋");
        } else {
            usernameLabel.setText("—");
            greetingLabel.setText("Hola 👋");
        }
    }

    private void loadAvatarIntoBtn() {
        if (avatarBtn == null) return;
        User user = app.getCurrentUser();
        if (user == null) return;
        try {
            Image img = null;
            String path = user.getAvatarPath();
            if (path != null && !path.trim().isEmpty()) {
                java.io.File f;
                try { f = new java.io.File(new java.net.URI(path)); }
                catch (Exception ex) { f = new java.io.File(path); }
                if (f.exists()) img = ActividadesController.loadImageSafe(f);
            }
            if (img == null) {
                java.io.InputStream s = getClass().getResourceAsStream("/runningLaSafor/images/avatar_default.png");
                if (s != null) img = ActividadesController.loadImageSafe(s);
            }
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(34); iv.setFitHeight(34); iv.setPreserveRatio(false);
                iv.setClip(new Circle(17, 17, 17));
                avatarBtn.setText("");
                avatarBtn.setGraphic(iv);
            }
        } catch (Exception ignored) {}
    }

    // ─── Stats ───────────────────────────────────────────────────────────
    private void populateDashboardStats() {
        List<Activity> activities = app.getUserActivities();

        if (activities == null || activities.isEmpty()) {
            distanciaLabel.setText("0.0 km");
            tiempoLabel.setText("00:00:00");
            ascensoLabel.setText("0 m");
            return;
        }

        // Filter to current month
        LocalDateTime now = LocalDateTime.now();
        double totalDistanceKm = 0.0;
        long totalSeconds = 0L;
        double totalElevation = 0.0;

        for (Activity a : activities) {
            LocalDateTime start = a.getStartTime();
            if (start != null
                    && start.getMonth() == now.getMonth()
                    && start.getYear() == now.getYear()) {

                totalDistanceKm += a.getTotalDistance() / 1000.0;
                if (a.getDuration() != null) {
                    totalSeconds += a.getDuration().getSeconds();
                }
                totalElevation += a.getElevationGain();
            }
        }

        distanciaLabel.setText(String.format("%.1f km", totalDistanceKm));
        tiempoLabel.setText(formatDuration(totalSeconds));
        ascensoLabel.setText(String.format("%.0f m", totalElevation));
    }

    /**
     * Format total seconds as HH:MM:SS
     */
    private String formatDuration(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    // ─── Recent activities list ───────────────────────────────────────────
    private void populateRecentActivities() {
        activitiesContainer.getChildren().clear();
        List<Activity> activities = app.getUserActivities();

        if (activities == null || activities.isEmpty()) {
            noActivitiesLabel.setVisible(true);
            noActivitiesLabel.setManaged(true);
            return;
        }

        noActivitiesLabel.setVisible(false);
        noActivitiesLabel.setManaged(false);

        // Show the 5 most recent activities (sorted by startTime descending)
        activities.stream()
                .filter(a -> a.getStartTime() != null)
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(5)
                .forEach(this::addActivityRow);
    }

    private void addActivityRow(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/runningLaSafor/views/ActivityRow.fxml"));
            Node row = loader.load();
            ActivityRowController ctrl = loader.getController();
            ctrl.setActivity(activity, this::handleActivitySelected);
            activitiesContainer.getChildren().add(row);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleActivitySelected(Activity activity) {
        setActiveButton(activitiesBtn);
        navigateTo("/runningLaSafor/views/Actividades.fxml", activitiesBtn);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  SIDEBAR TOGGLE
    // ═════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSidebarToggle() {
        sidebarExpanded = !sidebarExpanded;

        // Animate width
        double targetWidth = sidebarExpanded
                ? SIDEBAR_EXPANDED_WIDTH
                : SIDEBAR_COLLAPSED_WIDTH;

        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(200),
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                        new KeyValue(sidebar.minWidthProperty(), targetWidth))
        );
        tl.play();

        // Toggle visibility of text labels and section headers
        boolean show = sidebarExpanded;
        setVisible(appNameLabel, show);
        setVisible(generalLabel, show);
        setVisible(explorarLabel, show);
        setVisible(usuarioLabel, show);
        setVisible(dashboardLabel, show);
        setVisible(actividadesLabel, show);
        setVisible(historialLabel, show);
        setVisible(mapasLabel, show);
        setVisible(estadisticasLabel, show);
        setVisible(perfilLabel, show);

        // Flip toggle arrow
        sidebarToggleBtn.setText(sidebarExpanded ? "◀" : "▶");
    }

    private void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NAV BUTTON HANDLERS
    // ═════════════════════════════════════════════════════════════════════
    /**
     * Deactivate all nav buttons, then activate the selected one
     */
    private void setActiveButton(Button active) {
        Button[] navBtns = {dashboardBtn, activitiesBtn, historialBtn,
            mapasBtn, estadisticasBtn, perfilBtn};
        for (Button btn : navBtns) {
            btn.getStyleClass().remove("sidebar-btn-active");
        }
        active.getStyleClass().add("sidebar-btn-active");
    }

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        // Already on dashboard — just refresh stats
        populateDashboardStats();
        populateRecentActivities();
    }

    private void navigateTo(String fxmlPath, Button ref) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            ref.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleActividades() {
        setActiveButton(activitiesBtn);
        navigateTo("/runningLaSafor/views/Actividades.fxml", activitiesBtn);
    }

    @FXML
    private void handleHistorial() {
        setActiveButton(historialBtn);
        navigateTo("/runningLaSafor/views/Historial.fxml", historialBtn);
    }

    @FXML
    private void handleMapas() {
        setActiveButton(mapasBtn);
        navigateTo("/runningLaSafor/views/GestionMapas.fxml", mapasBtn);
    }

    @FXML
    private void handleEstadisticas() {
        setActiveButton(estadisticasBtn);
        navigateTo("/runningLaSafor/views/EstadisticasMes.fxml", estadisticasBtn);
    }

    @FXML
    private void handlePerfil() {
        setActiveButton(perfilBtn);
        navigateTo("/runningLaSafor/views/configuracion.fxml", perfilBtn);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TOP BAR ACTIONS
    // ═════════════════════════════════════════════════════════════════════
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
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

    // ═════════════════════════════════════════════════════════════════════
    //  GPX IMPORT
    // ═════════════════════════════════════════════════════════════════════
    @FXML
    private void handleImportGPX() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona fichero GPX");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("GPX Files", "*.gpx"));

        Stage stage = (Stage) importGpxBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            Activity imported = app.importActivity(selectedFile);
            if (imported != null) {
                System.out.println("Actividad importada: " + imported.getName());
                // Refresh dashboard stats and recent list
                populateDashboardStats();
                populateRecentActivities();
            } else {
                System.err.println("Error al importar la actividad GPX.");
                // TODO: show user-facing error alert
            }
        }
    }

    // ─── Import button injected field needed for stage retrieval ─────────
    @FXML
    private Button importGpxBtn;

    // ═════════════════════════════════════════════════════════════════════
    //  PUBLIC REFRESH (called from other controllers)
    // ═════════════════════════════════════════════════════════════════════
    /**
     * Force a full refresh of all dashboard data
     */
    public void refresh() {
        populateTopBar();
        populateDashboardStats();
        populateRecentActivities();
    }
}
