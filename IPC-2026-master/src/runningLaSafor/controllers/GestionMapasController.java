package runningLaSafor.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.scene.Scene;

public class GestionMapasController implements Initializable {

    private final SportActivityApp app = SportActivityApp.getInstance();

    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "ES"));

    private MapRegion selectedMap = null;
    private File selectedImageFile = null;

    // ── FXML nodes ──────────────────────────────────────────────────────
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

    // Map list
    @FXML
    private Label mapCountLabel;
    @FXML
    private VBox mapListContainer;

    // Preview
    @FXML
    private Label previewTitle;
    @FXML
    private VBox previewEmpty;
    @FXML
    private ImageView previewImage;
    @FXML
    private HBox previewInfoBox;
    @FXML
    private Label previewLat;
    @FXML
    private Label previewLon;
    @FXML
    private Label previewStatus;
    @FXML
    private Button deleteMapBtn;

    // Add form
    @FXML
    private TextField newMapName;
    @FXML
    private Label selectedFileLabel;
    @FXML
    private TextField newLatMin;
    @FXML
    private TextField newLatMax;
    @FXML
    private TextField newLonMin;
    @FXML
    private TextField newLonMax;
    @FXML
    private Label formErrorLabel;

    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_EXPANDED = 240.0;
    private static final double SIDEBAR_COLLAPSED = 68.0;

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateTopBar();
        loadMapList();
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

    // ══════════════════════════════════════════════════════════════════════
    //  MAP LIST
    // ══════════════════════════════════════════════════════════════════════
    private void loadMapList() {
        mapListContainer.getChildren().clear();
        List<MapRegion> maps = app.getMapRegions();
        if (maps == null) {
            maps = List.of();
        }

        mapCountLabel.setText(maps.size() + " mapa" + (maps.size() != 1 ? "s" : "") + " registrado" + (maps.size() != 1 ? "s" : ""));

        List<MapRegion> unused = app.getUnusedMapRegions();

        for (MapRegion r : maps) {
            mapListContainer.getChildren().add(buildMapCard(r, unused != null && unused.contains(r)));
        }

        if (maps.isEmpty()) {
            Label empty = new Label("No hay mapas registrados");
            empty.getStyleClass().add("act-empty-text");
            empty.setStyle("-fx-padding: 20;");
            mapListContainer.getChildren().add(empty);
        }
    }

    private Node buildMapCard(MapRegion region, boolean unused) {
        Label icon = new Label("🗺");
        icon.getStyleClass().add("act-list-icon");

        Label name = new Label(region.getName());
        name.getStyleClass().add("act-list-name");

        Label bounds = new Label(String.format("Lat %.3f–%.3f  Lon %.3f–%.3f",
                region.getLatMin(), region.getLatMax(),
                region.getLonMin(), region.getLonMax()));
        bounds.getStyleClass().add("act-list-meta");

        VBox text = new VBox(3, name, bounds);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label statusDot = new Label(unused ? "●" : "○");
        statusDot.setStyle("-fx-text-fill: " + (unused ? "#4a5568" : "#4ade80") + "; -fx-font-size: 10px;");
        Tooltip.install(statusDot, new Tooltip(unused ? "Sin actividades — se puede eliminar" : "En uso por actividades"));

        HBox row = new HBox(12, icon, text, statusDot);
        row.getStyleClass().add("act-list-item");
        if (region == selectedMap) {
            row.getStyleClass().add("act-list-item-active");
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> selectMap(region, unused));
        return row;
    }

    private void selectMap(MapRegion region, boolean unused) {
        this.selectedMap = region;
        loadMapList();  // re-render to show highlight

        previewTitle.setText(region.getName());

        // Try to load preview image
        File imgFile = new File(region.getImagePath());
        if (imgFile.exists()) {
            try {
                previewImage.setImage(ActividadesController.loadImageSafe(imgFile));
                previewEmpty.setVisible(false);
                previewEmpty.setManaged(false);
                previewImage.setVisible(true);
                previewImage.setManaged(true);
            } catch (Throwable t) {
                showNoPreview();
            }
        } else {
            showNoPreview();
        }

        previewLat.setText(String.format("%.6f / %.6f", region.getLatMin(), region.getLatMax()));
        previewLon.setText(String.format("%.6f / %.6f", region.getLonMin(), region.getLonMax()));
        previewStatus.setText(unused ? "Sin uso (eliminable)" : "En uso por actividades");
        previewStatus.setStyle("-fx-text-fill: " + (unused ? "#4a5568" : "#4ade80") + ";");

        previewInfoBox.setVisible(true);
        previewInfoBox.setManaged(true);
        deleteMapBtn.setDisable(!unused);
    }

    private void showNoPreview() {
        previewEmpty.setVisible(true);
        previewEmpty.setManaged(true);
        previewImage.setVisible(false);
        previewImage.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DELETE MAP
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleDeleteMap() {
        if (selectedMap == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar mapa");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Eliminar el mapa \"" + selectedMap.getName() + "\"?\n"
                + "Se borrará también la imagen del disco.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = app.removeMapRegion(selectedMap);
                if (ok) {
                    selectedMap = null;
                    showNoPreview();
                    previewInfoBox.setVisible(false);
                    previewInfoBox.setManaged(false);
                    deleteMapBtn.setDisable(true);
                    previewTitle.setText("Vista previa");
                    loadMapList();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "No se pudo eliminar el mapa. Puede que tenga actividades asociadas.",
                            ButtonType.OK).showAndWait();
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ADD MAP
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleBrowseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona imagen del mapa");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        File chosen = fc.showOpenDialog(stage);
        if (chosen != null) {
            selectedImageFile = chosen;
            selectedFileLabel.setText(chosen.getName());
        }
    }

    @FXML
    private void handleAddMap() {
        hideFormError();

        String name = newMapName.getText().trim();
        if (name.isBlank()) {
            showFormError("El nombre no puede estar vacío.");
            return;
        }

        if (selectedImageFile == null || !selectedImageFile.exists()) {
            showFormError("Selecciona una imagen JPG válida.");
            return;
        }

        double latMin, latMax, lonMin, lonMax;
        try {
            latMin = Double.parseDouble(newLatMin.getText().trim());
            latMax = Double.parseDouble(newLatMax.getText().trim());
            lonMin = Double.parseDouble(newLonMin.getText().trim());
            lonMax = Double.parseDouble(newLonMax.getText().trim());
        } catch (NumberFormatException e) {
            showFormError("Las coordenadas deben ser números decimales (ej: 39.334297).");
            return;
        }

        if (latMin >= latMax) {
            showFormError("La latitud mínima debe ser menor que la máxima.");
            return;
        }
        if (lonMin >= lonMax) {
            showFormError("La longitud mínima debe ser menor que la máxima.");
            return;
        }

        MapRegion created = app.addMapRegion(name, selectedImageFile, latMin, latMax, lonMin, lonMax);
        if (created != null) {
            clearForm();
            loadMapList();
            selectMap(created, true);
        } else {
            showFormError("No se pudo añadir el mapa. Comprueba que el nombre no está en uso.");
        }
    }

    private void showFormError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private void clearForm() {
        newMapName.clear();
        newLatMin.clear();
        newLatMax.clear();
        newLonMin.clear();
        newLonMax.clear();
        selectedImageFile = null;
        selectedFileLabel.setText("Sin fichero seleccionado");
        hideFormError();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIDEBAR TOGGLE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSidebarToggle() {
        sidebarExpanded = !sidebarExpanded;
        double target = sidebarExpanded ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED;
        new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(sidebar.prefWidthProperty(), target),
                new KeyValue(sidebar.minWidthProperty(), target))).play();
        boolean show = sidebarExpanded;
        setV(appNameLabel, show);
        setV(generalLabel, show);
        setV(explorarLabel, show);
        setV(usuarioLabel, show);
        setV(dashboardLabel, show);
        setV(actividadesLabel, show);
        setV(historialLabel, show);
        setV(mapasLabel, show);
        setV(estadisticasLabel, show);
        setV(perfilLabel, show);
        sidebarToggleBtn.setText(sidebarExpanded ? "◀" : "▶");
    }

    private void setV(Node n, boolean v) {
        n.setVisible(v);
        n.setManaged(v);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════════
    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            closeBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveBtn(Button active) {
        for (Button b : new Button[]{dashboardBtn, activitiesBtn, historialBtn,
            mapasBtn, estadisticasBtn, perfilBtn}) {
            b.getStyleClass().remove("sidebar-btn-active");
        }
        active.getStyleClass().add("sidebar-btn-active");
    }

    @FXML
    private void handleDashboard() {
        setActiveBtn(dashboardBtn);
        navigateTo("/runningLaSafor/views/Dashboard.fxml");
    }

    @FXML
    private void handleActividades() {
        setActiveBtn(activitiesBtn);
        navigateTo("/runningLaSafor/views/Actividades.fxml");
    }

    @FXML
    private void handleHistorial() {
        setActiveBtn(historialBtn);
        navigateTo("/runningLaSafor/views/Historial.fxml");
    }

    @FXML
    private void handleMapas() {
        setActiveBtn(mapasBtn);
    }

    @FXML
    private void handleEstadisticas() {
        setActiveBtn(estadisticasBtn);
        navigateTo("/runningLaSafor/views/EstadisticasMes.fxml");
    }

    @FXML
    private void handlePerfil() {
        setActiveBtn(perfilBtn);
        navigateTo("/runningLaSafor/views/configuracion.fxml");
    }

    @FXML
    private void handleAvatarClick() {
        handlePerfil();
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
}
