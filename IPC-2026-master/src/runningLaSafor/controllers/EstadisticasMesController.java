/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package runningLaSafor.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author Mariulas
 */
public class EstadisticasMesController implements Initializable {

    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "ES"));
    private final SportActivityApp app = SportActivityApp.getInstance();

    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_W_EXPANDED = 240;
    private static final double SIDEBAR_W_COLLAPSED = 68;
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
    @FXML
    private VBox sidebar;
    @FXML
    private HBox logoBox;
    @FXML
    private Label appNameLabel;
    @FXML
    private Button sidebarToggleBtn;
    @FXML
    private Label generalLabel;
    @FXML
    private Button dashboardBtn;
    @FXML
    private Label dashboardLabel;
    @FXML
    private Button activitiesBtn;
    @FXML
    private Label actividadesLabel;
    @FXML
    private Button historialBtn;
    @FXML
    private Label historialLabel;
    @FXML
    private Label explorarLabel;
    @FXML
    private Button mapasBtn;
    @FXML
    private Label mapasLabel;
    @FXML
    private Button estadisticasBtn;
    @FXML
    private Label estadisticasLabel;
    @FXML
    private Label usuarioLabel;
    @FXML
    private Button perfilBtn;
    @FXML
    private Label perfilLabel;
    @FXML
    private VBox mainContent;
    @FXML
    private ComboBox<String> comboMes;
    @FXML
    private ComboBox<String> comboMapa;
    @FXML
    private ComboBox<Integer> comboAnio;
    @FXML
    private ComboBox<String> comboCategoria;
    @FXML
    private VBox listaActividadesContainer;
    @FXML
    private Label statTiempo;
    @FXML
    private Label statDistancia;
    @FXML
    private Label statAscenso;
    @FXML
    private Label statDescenso;
    @FXML
    private Label statActividades;
    @FXML
    private Label labelPeriodo;

    private static final String[] MESES_NOMBRES = {
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        populateTopBar();

        // ── Meses ──
        comboMes.setItems(FXCollections.observableArrayList(MESES_NOMBRES));
        int mesActual = LocalDate.now().getMonthValue(); // 1-based
        comboMes.getSelectionModel().select(mesActual - 1);

        // ── Años ──
        int anioActual = LocalDate.now().getYear();
        ObservableList<Integer> anios = FXCollections.observableArrayList();
        for (int y = anioActual - 3; y <= anioActual; y++) {
            anios.add(y);
        }
        comboAnio.setItems(anios);
        comboAnio.getSelectionModel().select(Integer.valueOf(anioActual));

        // ── Categorías (decorative; Activity has no category field) ──
        comboCategoria.setItems(FXCollections.observableArrayList("Todas"));
        comboCategoria.getSelectionModel().selectFirst();

        // ── Mapas ── populate dynamically from library
        populateMapCombo();

        // ── Reactive: refresh on any combo change ──
        comboMes.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> actualizarListaActividades());
        comboAnio.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> actualizarListaActividades());
        comboMapa.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> actualizarListaActividades());

        setActiveButton(estadisticasBtn);

        Platform.runLater(this::actualizarListaActividades);
    }

    private void populateMapCombo() {
        List<String> mapNames = new ArrayList<>();
        mapNames.add("Todos");
        try {
            List<MapRegion> regions = app.getMapRegions();
            if (regions != null) {
                for (MapRegion r : regions) {
                    mapNames.add(r.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading map regions for combo: " + e.getMessage());
        }
        comboMapa.setItems(FXCollections.observableArrayList(mapNames));
        comboMapa.getSelectionModel().selectFirst();
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

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            dashboardBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDashboard() {
        setActiveButton(dashboardBtn);
        navigateTo("/runningLaSafor/views/Dashboard.fxml");
    }

    @FXML
    private void handleActividades() {
        setActiveButton(activitiesBtn);
        navigateTo("/runningLaSafor/views/Actividades.fxml");
    }

    @FXML
    private void handleHistorial() {
        setActiveButton(historialBtn);
        navigateTo("/runningLaSafor/views/Historial.fxml");
    }

    @FXML
    private void handleMapas() {
        setActiveButton(mapasBtn);
        navigateTo("/runningLaSafor/views/GestionMapas.fxml");
    }

    @FXML
    private void handleEstadisticas() {
        setActiveButton(estadisticasBtn);
        navigateTo("/runningLaSafor/views/EstadisticasMes.fxml");
    }

    @FXML
    private void handlePerfil() {
        setActiveButton(perfilBtn);
        navigateTo("/runningLaSafor/views/configuracion.fxml");
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

    @FXML
    private void handleSidebarToggle() {
        sidebarExpanded = !sidebarExpanded;
        double targetW = sidebarExpanded ? SIDEBAR_W_EXPANDED : SIDEBAR_W_COLLAPSED;
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(200),
                new javafx.animation.KeyValue(sidebar.prefWidthProperty(), targetW),
                new javafx.animation.KeyValue(sidebar.minWidthProperty(), targetW))).play();

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
        sidebarToggleBtn.setText(sidebarExpanded ? "◀" : "▶");
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    private void populateTopBar() {
        String formattedDate = LocalDate.now().format(DATE_FORMATTER);
        dateLabel.setText(formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1));

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

    // ═════════════════════════════════════════════════════════════════════
    //  LISTA DE ACTIVIDADES (REUTILIZANDO ACTIVITYROW)
    // ═════════════════════════════════════════════════════════════════════
    private void actualizarListaActividades() {
        if (listaActividadesContainer == null) {
            return;
        }
        listaActividadesContainer.getChildren().clear();

        // ── resolve filters ──
        int mesIdx = comboMes.getSelectionModel().getSelectedIndex(); // 0-based
        int mes = mesIdx + 1; // 1-based month number
        Integer anio = comboAnio.getSelectionModel().getSelectedItem();
        String mapaFiltro = comboMapa.getSelectionModel().getSelectedItem();

        if (anio == null) {
            anio = LocalDate.now().getYear();
        }

        // ── update period label ──
        String mesNombre = (mesIdx >= 0 && mesIdx < 12) ? MESES_NOMBRES[mesIdx] : "?";
        if (labelPeriodo != null) {
            labelPeriodo.setText("Actividades de " + mesNombre + " " + anio);
        }

        List<Activity> todas = app.getUserActivities();
        if (todas == null) {
            todas = new ArrayList<>();
        }

        final int mesFinal = mes;
        final int anioFinal = anio;
        final String mapaFinal = mapaFiltro;

        List<Activity> filtradas = new ArrayList<>();
        for (Activity a : todas) {
            if (a.getStartTime() == null) {
                continue;
            }
            if (a.getStartTime().getMonthValue() != mesFinal) {
                continue;
            }
            if (a.getStartTime().getYear() != anioFinal) {
                continue;
            }
            if (mapaFinal != null && !mapaFinal.equals("Todos")) {
                MapRegion m = a.getSuggestedMap();
                if (m == null || !m.getName().equals(mapaFinal)) {
                    continue;
                }
            }
            filtradas.add(a);
        }

        // ── aggregate stats ──
        long totalSecs = 0;
        double totalDist = 0, totalGain = 0, totalLoss = 0;
        for (Activity a : filtradas) {
            Duration d = a.getDuration();
            if (d != null) {
                totalSecs += d.getSeconds();
            }
            totalDist += a.getTotalDistance();
            totalGain += a.getElevationGain();
            totalLoss += a.getElevationLoss();
        }

        if (statTiempo != null) {
            statTiempo.setText(formatDuration(totalSecs));
        }
        if (statDistancia != null) {
            statDistancia.setText(String.format("%.2f km", totalDist / 1000.0));
        }
        if (statAscenso != null) {
            statAscenso.setText(String.format("%.0f m", totalGain));
        }
        if (statDescenso != null) {
            statDescenso.setText(String.format("%.0f m", totalLoss));
        }
        if (statActividades != null) {
            statActividades.setText(String.valueOf(filtradas.size()));
        }

        // ── activity rows ──
        if (filtradas.isEmpty()) {
            Label vacio = new Label("No hay actividades para " + mesNombre + " " + anioFinal + ".");
            vacio.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-style: italic; -fx-padding: 10 0 0 0;");
            listaActividadesContainer.getChildren().add(vacio);
            return;
        }

        filtradas.stream()
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .forEach(this::addActivityRow);
    }

    private static String formatDuration(long totalSecs) {
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        if (h > 0) {
            return String.format("%dh %02dm", h, m);
        }
        return String.format("%dm %02ds", m, s);
    }

    /**
     * Carga el FXML de la fila, le pasa los datos y lo inyecta en el VBox.
     */
    private void addActivityRow(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/runningLaSafor/views/ActivityRow.fxml"));
            Node row = loader.load();

            ActivityRowController ctrl = loader.getController();
            ctrl.setActivity(activity, this::handleActivitySelected);

            listaActividadesContainer.getChildren().add(row);
        } catch (Exception e) {
            System.err.println("Error al cargar la fila de actividad: " + activity.getName());
            e.printStackTrace();
        }
    }

    /**
     * Lo que ocurre cuando el usuario hace clic en una fila específica.
     */
    private void handleActivitySelected(Activity activity) {
        ActividadesController.requestOpenActivity(activity);
        navigateTo("/runningLaSafor/views/Actividades.fxml");
    }

}
