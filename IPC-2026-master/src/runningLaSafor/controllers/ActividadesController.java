package runningLaSafor.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import upv.ipc.sportlib.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.Scene;

public class ActividadesController implements Initializable {

    // ─── Backend ─────────────────────────────────────────────────────────
    private final SportActivityApp app = SportActivityApp.getInstance();

    // ─── Formatters ──────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "ES"));
    private static final DateTimeFormatter DT_SHORT
            = DateTimeFormatter.ofPattern("d MMM yyyy 'a las' HH:mm", new Locale("es", "ES"));

    // ─── Pagination ───────────────────────────────────────────────────────
    private static final int PAGE_SIZE = 5;
    private int currentPage = 0;
    private List<Activity> filteredList = new ArrayList<>();

    // ─── Selected activity & map state ───────────────────────────────────
    private Activity selectedActivity = null;
    private MapProjection currentProjection = null;
    private double currentZoom = 1.0;
    private static final double ZOOM_STEP = 1.4;
    private static final double ZOOM_MIN = 0.1;
    private static final double ZOOM_MAX = 8.0;

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

    @FXML
    private Label distanciaLabel;
    @FXML
    private Label tiempoLabel;
    @FXML
    private Label ascensoLabel;
    @FXML
    private Button importGpxBtn;

    @FXML
    private TextField searchField;
    @FXML
    private VBox actListContainer;
    @FXML
    private Label paginationInfoLabel;
    @FXML
    private Label pageNumberLabel;
    @FXML
    private Button prevPageBtn;
    @FXML
    private Button nextPageBtn;

    @FXML
    private VBox emptyDetail;
    @FXML
    private VBox detailContent;
    @FXML
    private Label detailName;
    @FXML
    private Label detailDate;
    @FXML
    private Button detailMenuBtn;

    // Map (mapPane and mapGroup are created dynamically like MapaDemo's buildMap)
    @FXML
    private ScrollPane mapScrollPane;
    @FXML
    private Pane mapScrollContent; // placeholder injected from FXML; replaced at runtime
    private Group mapGroup;
    private Group mapContentGroup;
    private Pane mapPane;
    @FXML
    private Label mapRegionLabel;
    @FXML
    private Pane elevationChartPane;
    @FXML
    private Label profileHoverLabel;

    @FXML
    private Label sDistancia;
    @FXML
    private Label sDuracion;
    @FXML
    private Label sVelocidad;
    @FXML
    private Label sRitmo;
    @FXML
    private Label sAscenso;
    @FXML
    private Label sDescenso;
    @FXML
    private Label sAltMin;
    @FXML
    private Label sAltMax;

    @FXML
    private Label annotTitle;
    @FXML
    private Label cntPunto;
    @FXML
    private Label cntTexto;
    @FXML
    private Label cntLinea;
    @FXML
    private Label cntCirculo;
    @FXML
    private VBox annotContainer;

    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_EXPANDED = 240.0;
    private static final double SIDEBAR_COLLAPSED = 68.0;
    private List<TrackPoint> currentProfileTrackPoints = List.of();
    private Circle mapHoverMarker;
    private final double[] panDragStart = new double[4]; // [screenX, screenY, hValue, vValue]
    private boolean annotationPlacementMode = false;
    private ContextMenu activeAnnotationMenu = null;
    // Route center in mapPane pixel coordinates — updated on every render, used to keep the
    // route centered when zooming in/out.
    private double routeCenterXInPane = -1;
    private double routeCenterYInPane = -1;
    // When true the next renderMap() call preserves the current scroll+zoom instead of
    // calling fitMapToRoute (used after annotation add/delete operations).
    private boolean preserveScrollOnRender = false;
    private static Long pendingOpenActivityId = null;

    public static void requestOpenActivity(Activity activity) {
        pendingOpenActivityId = activity != null ? activity.getId() : null;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateTopBar();
        populateMonthStats();
        loadAllActivities();
        setupMapPanCursor();
        elevationChartPane.widthProperty().addListener((obs, oldV, newV) -> renderElevationProfile());
        elevationChartPane.heightProperty().addListener((obs, oldV, newV) -> renderElevationProfile());
        mapScrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, keyEvt -> {
                    if (annotationPlacementMode
                            && (keyEvt.getCode() == javafx.scene.input.KeyCode.ESCAPE
                            || keyEvt.getCode() == javafx.scene.input.KeyCode.DELETE)) {
                        cancelPendingAnnotationMode();
                    }
                });
            }
        });
    }

    private void setupMapPanCursor() {
        mapScrollPane.setPannable(false);
        mapScrollPane.setCursor(Cursor.OPEN_HAND);
        mapScrollPane.setPickOnBounds(true);

        mapScrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            // Close the annotation context menu on any click and swallow left-clicks
            if (activeAnnotationMenu != null && activeAnnotationMenu.isShowing()) {
                activeAnnotationMenu.hide();
                activeAnnotationMenu = null;
                if (e.getButton() == MouseButton.PRIMARY) {
                    e.consume();
                    return;
                }
            }
            if (annotationPlacementMode) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                panDragStart[0] = e.getScreenX();
                panDragStart[1] = e.getScreenY();
                panDragStart[2] = mapScrollPane.getHvalue();
                panDragStart[3] = mapScrollPane.getVvalue();
                mapScrollPane.setCursor(Cursor.CLOSED_HAND);
            }
        });
        mapScrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (annotationPlacementMode) {
                return;
            }
            if (e.isPrimaryButtonDown() && mapPane != null) {
                double dx = e.getScreenX() - panDragStart[0];
                double dy = e.getScreenY() - panDragStart[1];
                double vpW = mapScrollPane.getViewportBounds().getWidth();
                double vpH = mapScrollPane.getViewportBounds().getHeight();
                double contentW = mapPane.getPrefWidth() * currentZoom;
                double contentH = mapPane.getPrefHeight() * currentZoom;
                double rangeW = contentW - vpW;
                double rangeH = contentH - vpH;
                if (rangeW > 0) {
                    mapScrollPane.setHvalue(Math.max(0, Math.min(1,
                            panDragStart[2] - dx / rangeW)));
                }
                if (rangeH > 0) {
                    mapScrollPane.setVvalue(Math.max(0, Math.min(1,
                            panDragStart[3] - dy / rangeH)));
                }
                e.consume();
            }
        });

        mapScrollPane.addEventFilter(MouseEvent.MOUSE_RELEASED, e
                -> {
                    if (!annotationPlacementMode) {
                        mapScrollPane.setCursor(Cursor.OPEN_HAND);
                    }
                });
        mapScrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            // Keep vertical wheel scrolling even with custom drag panning.
            double dy = e.getDeltaY();
            if (Math.abs(dy) > 0.01) {
                mapScrollPane.setVvalue(Math.max(0, Math.min(1,
                        mapScrollPane.getVvalue() - dy / 1200.0)));
                e.consume();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TOP BAR
    // ══════════════════════════════════════════════════════════════════════
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
                if (f.exists()) img = loadImageSafe(f);
            }
            if (img == null) {
                java.io.InputStream s = getClass().getResourceAsStream("/runningLaSafor/images/avatar_default.png");
                if (s != null) img = loadImageSafe(s);
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
    //  MONTH STATS
    // ══════════════════════════════════════════════════════════════════════
    private void populateMonthStats() {
        if (distanciaLabel == null || tiempoLabel == null || ascensoLabel == null) {
            return;
        }
        List<Activity> all = app.getUserActivities();
        if (all == null) {
            all = List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        double km = 0;
        long secs = 0;
        double elev = 0;

        for (Activity a : all) {
            if (a.getStartTime() != null
                    && a.getStartTime().getMonth() == now.getMonth()
                    && a.getStartTime().getYear() == now.getYear()) {
                km += a.getTotalDistance() / 1000.0;
                if (a.getDuration() != null) {
                    secs += a.getDuration().getSeconds();
                }
                elev += a.getElevationGain();
            }
        }
        distanciaLabel.setText(String.format("%.1f km", km));
        tiempoLabel.setText(formatHMS(secs));
        ascensoLabel.setText(String.format("%.0f m", elev));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ACTIVITY LIST
    // ══════════════════════════════════════════════════════════════════════
    private void loadAllActivities() {
        List<Activity> all = app.getUserActivities();
        if (all == null) {
            all = List.of();
        }

        filteredList = all.stream()
                .filter(a -> a.getStartTime() != null)
                .sorted(Comparator.comparing(Activity::getStartTime).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        currentPage = 0;
        renderPage();
        openPendingActivityIfRequested();
    }

    private void openPendingActivityIfRequested() {
        if (pendingOpenActivityId == null) {
            return;
        }
        for (Activity activity : filteredList) {
            if (activity.getId() == pendingOpenActivityId) {
                pendingOpenActivityId = null;
                selectActivity(activity);
                return;
            }
        }
        pendingOpenActivityId = null;
    }

    private void renderPage() {
        actListContainer.getChildren().clear();

        int total = filteredList.size();
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        for (int i = from; i < to; i++) {
            actListContainer.getChildren().add(buildListItem(filteredList.get(i)));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable(currentPage >= totalPages - 1);
        pageNumberLabel.setText(String.valueOf(currentPage + 1));

        int displayFrom = total == 0 ? 0 : from + 1;
        paginationInfoLabel.setText(
                String.format("Mostrando %d–%d de %d actividades", displayFrom, to, total));
    }

    private Node buildListItem(Activity activity) {
        Label icon = new Label("🏃");
        icon.getStyleClass().add("act-list-icon");

        Label name = new Label(activity.getName() != null ? activity.getName() : "Actividad sin nombre");
        name.getStyleClass().add("act-list-name");

        String datePart = activity.getStartTime() != null
                ? activity.getStartTime().format(DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es", "ES")))
                : "—";
        double distKm = activity.getTotalDistance() / 1000.0;
        String durStr = activity.getDuration() != null ? formatHMS(activity.getDuration().getSeconds()) : "—";
        Label meta = new Label(datePart + "  ·  " + String.format("%.1f km", distKm) + "  ·  " + durStr);
        meta.getStyleClass().add("act-list-meta");

        VBox textBox = new VBox(4, name, meta);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label arrow = new Label("›");
        arrow.getStyleClass().add("act-list-arrow");

        HBox row = new HBox(12, icon, textBox, arrow);
        row.getStyleClass().add("act-list-item");
        if (activity == selectedActivity) {
            row.getStyleClass().add("act-list-item-active");
        }
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> selectActivity(activity));
        return row;
    }

    private void selectActivity(Activity activity) {
        this.selectedActivity = activity;
        renderPage();
        showDetail(activity);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DETAIL PANEL
    // ══════════════════════════════════════════════════════════════════════
    private void showDetail(Activity activity) {
        emptyDetail.setVisible(false);
        emptyDetail.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);

        detailName.setText(activity.getName() != null ? activity.getName() : "Actividad sin nombre");
        detailDate.setText(activity.getStartTime() != null
                ? "📅  " + activity.getStartTime().format(DT_SHORT) : "—");

        // Statistics — getAverageSpeed() returns km/h per the library API
        sDistancia.setText(String.format("%.1f km", activity.getTotalDistance() / 1000.0));
        sDuracion.setText(activity.getDuration() != null
                ? formatHMS(activity.getDuration().getSeconds()) : "—");
        sVelocidad.setText(String.format("%.1f km/h", activity.getAverageSpeed()));

        double pace = activity.getAveragePace();   // min/km
        long paceMin = (long) pace;
        long paceSec = Math.round((pace - paceMin) * 60);
        sRitmo.setText(String.format("%d:%02d min/km", paceMin, paceSec));

        sAscenso.setText(String.format("%.0f m", activity.getElevationGain()));
        sDescenso.setText(String.format("%.0f m", activity.getElevationLoss()));
        sAltMin.setText(String.format("%.0f m", activity.getMinElevation()));
        sAltMax.setText(String.format("%.0f m", activity.getMaxElevation()));

        renderMap();
        currentProfileTrackPoints = activity.getTrackPoints() != null ? activity.getTrackPoints() : List.of();
        renderElevationProfile();
        populateAnnotations(activity);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAP RENDERING  (ScrollPane → Group → Pane approach)
    // ══════════════════════════════════════════════════════════════════════
    // ── buildMap: exact MapaDemo pattern ─────────────────────────────────
    private void renderMap() {
        if (selectedActivity == null) {
            return;
        }
        currentProjection = null;
        mapHoverMarker = null;
        routeCenterXInPane = -1;
        routeCenterYInPane = -1;

        // Capture current scroll/zoom BEFORE setContent() resets them.
        final double priorH = mapScrollPane.getHvalue();
        final double priorV = mapScrollPane.getVvalue();
        final double priorZoom = currentZoom;

        MapRegion region = selectedActivity.getSuggestedMap();
        if (region == null) {
            region = app.findMapForActivity(selectedActivity);
        }

        double imgW = 600, imgH = 400;

        // Fresh Pane sized to the image (MapaDemo: mapPane = new Pane())
        mapPane = new Pane();
        mapPane.setCursor(Cursor.OPEN_HAND);

        if (region != null) {
            File imgFile = new File(region.getImagePath());
            if (imgFile.exists()) {
                try {
                    Image mapImage = loadImageSafe(imgFile);
                    imgW = mapImage.getWidth();
                    imgH = mapImage.getHeight();

                    ImageView imgView = new ImageView(mapImage);
                    imgView.setFitWidth(imgW);
                    imgView.setFitHeight(imgH);
                    mapPane.getChildren().add(imgView);
                    currentProjection = new MapProjection(region, imgW, imgH);
                    mapRegionLabel.setText("📍 " + region.getName());
                } catch (Throwable t) {
                    System.err.println("Error cargando imagen del mapa: " + t);
                    drawFallbackBackground(imgW, imgH);
                    mapRegionLabel.setText("⚠ Error al cargar imagen");
                }
            } else {
                drawFallbackBackground(imgW, imgH);
                mapRegionLabel.setText("⚠ Imagen no encontrada: " + region.getImagePath());
            }
        } else {
            drawFallbackBackground(imgW, imgH);
            mapRegionLabel.setText("⚠ Sin mapa asignado");
        }

        // Size the pane exactly to the image (MapaDemo pattern)
        mapPane.setPrefSize(imgW, imgH);
        mapPane.setMinSize(imgW, imgH);
        mapPane.setMaxSize(imgW, imgH);

        // Draw route
        List<TrackPoint> pts = selectedActivity.getTrackPoints();
        if (pts != null && pts.size() >= 2) {
            if (currentProjection != null) {
                drawRouteWithProjection(pts);
            } else {
                drawFallbackRoute(pts, imgW, imgH);
            }
        }

        // MapaDemo double-Group structure: contentGroup > mapGroup (zoomGroup) > mapPane
        // contentGroup's layoutBounds reflect the scaled mapGroup, so the
        // ScrollPane computes correct scrollable area at every zoom level.
        mapGroup = new Group(mapPane);
        mapContentGroup = new Group(mapGroup);
        mapPane.setPickOnBounds(true);
        mapGroup.setPickOnBounds(true);
        mapContentGroup.setPickOnBounds(true);
        mapScrollPane.setContent(mapContentGroup);

        // Draw annotations on map
        drawAnnotationsOnMap();

        // Right-click → add annotation
        setupMapRightClick();

        // Decide whether to restore the previous position (annotation operations)
        // or to auto-fit the route (first render / activity change).
        final List<TrackPoint> routePts = selectedActivity.getTrackPoints();
        final double finalW = imgW, finalH = imgH;
        final boolean preserve = preserveScrollOnRender;
        preserveScrollOnRender = false;
        if (preserve) {
            Platform.runLater(() -> {
                applyZoom(priorZoom > 0 ? priorZoom : 1.0);
                mapScrollPane.setHvalue(priorH);
                mapScrollPane.setVvalue(priorV);
            });
        } else {
            Platform.runLater(() -> fitMapToRoute(routePts, finalW, finalH));
        }
    }

    private void drawFallbackBackground(double w, double h) {
        Rectangle bg = new Rectangle(w, h, Color.web("#1a2133"));
        mapPane.getChildren().add(bg);

        // Subtle grid
        for (int i = 1; i < 6; i++) {
            Line hl = new Line(0, h * i / 6, w, h * i / 6);
            hl.setStroke(Color.web("#1e2a3a"));
            hl.setStrokeWidth(1);
            Line vl = new Line(w * i / 6, 0, w * i / 6, h);
            vl.setStroke(Color.web("#1e2a3a"));
            vl.setStrokeWidth(1);
            mapPane.getChildren().addAll(hl, vl);
        }
    }

    private void drawRouteWithProjection(List<TrackPoint> pts) {
        List<Double> segmentSpeeds = computeSegmentSpeeds(pts);
        double minSpeed = segmentSpeeds.stream().mapToDouble(v -> v).min().orElse(0.0);
        double maxSpeed = segmentSpeeds.stream().mapToDouble(v -> v).max().orElse(1.0);

        for (int i = 0; i < pts.size() - 1; i++) {
            TrackPoint p1 = pts.get(i);
            TrackPoint p2 = pts.get(i + 1);
            Point2D a = currentProjection.project(p1);
            Point2D b = currentProjection.project(p2);
            double speed = segmentSpeeds.get(i);

            Line segment = new Line(a.getX(), a.getY(), b.getX(), b.getY());
            segment.setStroke(colorForSpeed(speed, minSpeed, maxSpeed));
            segment.setStrokeWidth(4.0);
            segment.setStrokeLineCap(StrokeLineCap.ROUND);
            segment.setMouseTransparent(true);

            // Wider transparent hit-area so the hover fires easily
            Line hitArea = new Line(a.getX(), a.getY(), b.getX(), b.getY());
            hitArea.setStroke(Color.color(1, 1, 1, 0.01));
            hitArea.setStrokeWidth(14.0);
            hitArea.setStrokeLineCap(StrokeLineCap.ROUND);
            double speedFinal = speed;
            hitArea.setOnMouseMoved(evt -> {
                if (!annotationPlacementMode) {
                    mapRegionLabel.setText(String.format("💨 %.1f km/h", speedFinal));
                }
            });
            hitArea.setOnMouseExited(evt -> {
                if (!annotationPlacementMode) {
                    restoreMapRegionLabel();
                }
            });
            mapPane.getChildren().addAll(segment, hitArea);
        }

        // Start marker (green ring)
        Point2D s = currentProjection.project(pts.get(0));
        Circle startOuter = new Circle(s.getX(), s.getY(), 8, Color.web("#4ade80"));
        Circle startInner = new Circle(s.getX(), s.getY(), 4, Color.web("#0f1a0f"));

        // End marker (red square)
        Point2D e = currentProjection.project(pts.get(pts.size() - 1));
        Rectangle endRect = new Rectangle(e.getX() - 8, e.getY() - 8, 16, 16);
        Rectangle endInner = new Rectangle(e.getX() - 3, e.getY() - 3, 6, 6);
        endRect.setFill(Color.web("#ef4444"));
        endInner.setFill(Color.WHITE);

        mapPane.getChildren().addAll(startOuter, startInner, endRect, endInner);
        mapHoverMarker = new Circle(s.getX(), s.getY(), 7, Color.web("#facc15"));
        mapHoverMarker.setStroke(Color.WHITE);
        mapHoverMarker.setStrokeWidth(2);
        mapHoverMarker.setVisible(false);
        mapPane.getChildren().add(mapHoverMarker);
    }

    private void drawFallbackRoute(List<TrackPoint> pts, double W, double H) {
        double latMin = pts.stream().mapToDouble(TrackPoint::getLatitude).min().orElse(0);
        double latMax = pts.stream().mapToDouble(TrackPoint::getLatitude).max().orElse(1);
        double lonMin = pts.stream().mapToDouble(TrackPoint::getLongitude).min().orElse(0);
        double lonMax = pts.stream().mapToDouble(TrackPoint::getLongitude).max().orElse(1);
        double latR = latMax - latMin;
        if (latR == 0) {
            latR = 0.001;
        }
        double lonR = lonMax - lonMin;
        if (lonR == 0) {
            lonR = 0.001;
        }
        double pad = 30;
        double scale = Math.min((W - pad * 2) / lonR, (H - pad * 2) / latR);
        double offX = pad + ((W - pad * 2) - lonR * scale) / 2;
        double offY = pad + ((H - pad * 2) - latR * scale) / 2;

        // Store route bounding-box center so zoom buttons can keep it centred.
        routeCenterXInPane = offX + lonR * scale / 2.0;
        routeCenterYInPane = offY + latR * scale / 2.0;

        List<Double> segmentSpeeds = computeSegmentSpeeds(pts);
        double minSpeed = segmentSpeeds.stream().mapToDouble(v -> v).min().orElse(0.0);
        double maxSpeed = segmentSpeeds.stream().mapToDouble(v -> v).max().orElse(1.0);
        for (int i = 0; i < pts.size() - 1; i++) {
            TrackPoint a = pts.get(i);
            TrackPoint b = pts.get(i + 1);
            double x1 = offX + (a.getLongitude() - lonMin) * scale;
            double y1 = offY + (latMax - a.getLatitude()) * scale;
            double x2 = offX + (b.getLongitude() - lonMin) * scale;
            double y2 = offY + (latMax - b.getLatitude()) * scale;
            Line segment = new Line(x1, y1, x2, y2);
            segment.setStroke(colorForSpeed(segmentSpeeds.get(i), minSpeed, maxSpeed));
            segment.setStrokeWidth(3.5);
            segment.setStrokeLineCap(StrokeLineCap.ROUND);
            segment.setMouseTransparent(true);

            Line hitArea = new Line(x1, y1, x2, y2);
            hitArea.setStroke(Color.color(1, 1, 1, 0.01));
            hitArea.setStrokeWidth(14.0);
            hitArea.setStrokeLineCap(StrokeLineCap.ROUND);
            double speedFinal = segmentSpeeds.get(i);
            hitArea.setOnMouseMoved(evt -> {
                if (!annotationPlacementMode) {
                    mapRegionLabel.setText(String.format("💨 %.1f km/h", speedFinal));
                }
            });
            hitArea.setOnMouseExited(evt -> {
                if (!annotationPlacementMode) {
                    restoreMapRegionLabel();
                }
            });
            mapPane.getChildren().addAll(segment, hitArea);
        }

        double[] sp = {offX + (pts.get(0).getLongitude() - lonMin) * scale,
            offY + (latMax - pts.get(0).getLatitude()) * scale};
        Circle sc = new Circle(sp[0], sp[1], 8, Color.web("#4ade80"));
        Circle si = new Circle(sp[0], sp[1], 4, Color.web("#0f1a0f"));
        TrackPoint last = pts.get(pts.size() - 1);
        double ex = offX + (last.getLongitude() - lonMin) * scale;
        double ey = offY + (latMax - last.getLatitude()) * scale;
        Rectangle er = new Rectangle(ex - 8, ey - 8, 16, 16);
        er.setFill(Color.web("#ef4444"));
        mapPane.getChildren().addAll(sc, si, er);

        mapHoverMarker = new Circle(sp[0], sp[1], 7, Color.web("#facc15"));
        mapHoverMarker.setStroke(Color.WHITE);
        mapHoverMarker.setStrokeWidth(2);
        mapHoverMarker.setVisible(false);
        mapPane.getChildren().add(mapHoverMarker);
    }

    private void drawAnnotationsOnMap() {
        if (selectedActivity == null) {
            return;
        }
        List<Annotation> anns = selectedActivity.getAnnotations();
        if (anns == null || anns.isEmpty()) {
            return;
        }

        for (Annotation ann : anns) {
            if (ann.getGeoPoints() == null || ann.getGeoPoints().isEmpty()) {
                continue;
            }
            GeoPoint gp = ann.getGeoPoints().get(0);

            double ax, ay;
            if (currentProjection != null) {
                Point2D pos = currentProjection.project(gp);
                ax = pos.getX();
                ay = pos.getY();
            } else {
                continue;
            }

            Color color;
            try {
                color = Color.web(ann.getColor() != null ? ann.getColor() : "#f59e0b");
            } catch (Exception ex) {
                color = Color.web("#f59e0b");
            }

            switch (ann.getType()) {
                case POINT -> {
                    Circle c = new Circle(ax, ay, 6, color);
                    c.setStroke(Color.WHITE);
                    c.setStrokeWidth(1.5);
                    c.setMouseTransparent(true);
                    mapPane.getChildren().add(c);
                    if (ann.getText() != null && !ann.getText().isBlank()) {
                        Text t = new Text(ax + 10, ay + 4, ann.getText());
                        t.setFill(Color.WHITE);
                        t.setFont(Font.font(11));
                        t.setStyle("-fx-effect: dropshadow(gaussian, #000000, 3, 0.5, 0, 0);");
                        t.setMouseTransparent(true);
                        mapPane.getChildren().add(t);
                    }
                }
                case TEXT -> {
                    Text t = new Text(ax, ay, ann.getText() != null ? ann.getText() : "");
                    t.setFill(color);
                    t.setFont(Font.font(12));
                    t.setStyle("-fx-effect: dropshadow(gaussian, #000000, 3, 0.5, 0, 0);");
                    t.setMouseTransparent(true);
                    mapPane.getChildren().add(t);
                }
                case LINE -> {
                    if (ann.getGeoPoints().size() >= 2 && currentProjection != null) {
                        Point2D p2 = currentProjection.project(ann.getGeoPoints().get(1));
                        Line line = new Line(ax, ay, p2.getX(), p2.getY());
                        line.setStroke(color);
                        line.setStrokeWidth(ann.getStrokeWidth() > 0 ? ann.getStrokeWidth() : 2);
                        line.setMouseTransparent(true);
                        mapPane.getChildren().add(line);
                    }
                }
                case CIRCLE -> {
                    if (ann.getGeoPoints().size() >= 2 && currentProjection != null) {
                        Point2D p2 = currentProjection.project(ann.getGeoPoints().get(1));
                        double r = Math.sqrt(Math.pow(p2.getX() - ax, 2) + Math.pow(p2.getY() - ay, 2));
                        Circle circle = new Circle(ax, ay, r);
                        circle.setFill(Color.TRANSPARENT);
                        circle.setStroke(color);
                        circle.setStrokeWidth(ann.getStrokeWidth() > 0 ? ann.getStrokeWidth() : 2);
                        circle.setMouseTransparent(true);
                        mapPane.getChildren().add(circle);
                    }
                }
            }
        }
    }

    private void setupMapRightClick() {
        // SECONDARY press on mapPane is the single right-click trigger.
        // Events from every child (route lines, annotation shapes, hit-areas) bubble up to mapPane.
        // ContextMenuEvent is NOT used because it does not reliably bubble through ScrollPane's internal skin.
        javafx.event.EventHandler<MouseEvent> rightClickHandler = evt -> {
            if (evt.getButton() != MouseButton.SECONDARY) return;
            if (selectedActivity == null) { evt.consume(); return; }
            if (annotationPlacementMode) {
                cancelPendingAnnotationMode();
                evt.consume();
                return;
            }
            GeoPoint geo = eventToGeo(evt);
            showAnnotationContextMenu(geo, evt.getScreenX(), evt.getScreenY());
            evt.consume();
        };
        if (mapPane != null) {
            mapPane.setOnMousePressed(rightClickHandler);
        }
        // Clear every other handler to prevent the menu from opening more than once
        mapScrollPane.setOnContextMenuRequested(null);
        mapScrollPane.setOnMousePressed(null);
        if (mapGroup != null) {
            mapGroup.setOnContextMenuRequested(null);
            mapGroup.setOnMousePressed(null);
        }
        if (mapContentGroup != null) {
            mapContentGroup.setOnContextMenuRequested(null);
            mapContentGroup.setOnMousePressed(null);
        }
    }

    private GeoPoint eventToGeo(ContextMenuEvent evt) {
        if (currentProjection == null || mapGroup == null || mapPane == null) {
            return new GeoPoint(0, 0);
        }
        Point2D scenePoint = new Point2D(evt.getSceneX(), evt.getSceneY());
        Point2D panePoint = mapPane.sceneToLocal(scenePoint);
        double x = Math.max(0, Math.min(mapPane.getPrefWidth(), panePoint.getX()));
        double y = Math.max(0, Math.min(mapPane.getPrefHeight(), panePoint.getY()));
        return currentProjection.unproject(x, y);
    }

    private GeoPoint eventToGeo(MouseEvent evt) {
        if (currentProjection == null || mapGroup == null || mapPane == null) {
            return new GeoPoint(0, 0);
        }
        Point2D scenePoint = new Point2D(evt.getSceneX(), evt.getSceneY());
        Point2D panePoint = mapPane.sceneToLocal(scenePoint);
        double x = Math.max(0, Math.min(mapPane.getPrefWidth(), panePoint.getX()));
        double y = Math.max(0, Math.min(mapPane.getPrefHeight(), panePoint.getY()));
        return currentProjection.unproject(x, y);
    }

    /**
     * Applies a zoom level and compensates the Group pivot so the map's
     * top-left corner stays at (0,0) in the contentGroup coordinate space. This
     * keeps the scroll math simple: a pixel at (px,py) in mapPane maps to
     * (px*scale, py*scale) in the scrollable content.
     */
    private void applyZoom(double scale) {
        if (mapGroup == null || mapPane == null) {
            return;
        }
        scale = Math.max(ZOOM_MIN, Math.min(scale, ZOOM_MAX));
        currentZoom = scale;
        mapGroup.setScaleX(scale);
        mapGroup.setScaleY(scale);
        // Shift the group so that its top-left aligns with (0,0) regardless of pivot
        double w = mapPane.getPrefWidth();
        double h = mapPane.getPrefHeight();
        mapGroup.setTranslateX(-w / 2.0 * (1 - scale));
        mapGroup.setTranslateY(-h / 2.0 * (1 - scale));
    }

    private void fitMapToViewport(double imgW, double imgH) {
        double vpW = mapScrollPane.getViewportBounds().getWidth();
        double vpH = mapScrollPane.getViewportBounds().getHeight();
        if (vpW <= 0 || vpH <= 0 || imgW <= 0 || imgH <= 0) {
            return;
        }
        applyZoom(Math.min(vpW / imgW, vpH / imgH) * 0.95);
        routeCenterXInPane = imgW / 2.0;
        routeCenterYInPane = imgH / 2.0;
    }

    /**
     * Pans the viewport so the route's bounding-box centre stays in the middle.
     * Called after every zoom-in / zoom-out button press.
     */
    private void centerViewportOnRoute() {
        if (routeCenterXInPane < 0 || mapPane == null) return;
        double vpW = mapScrollPane.getViewportBounds().getWidth();
        double vpH = mapScrollPane.getViewportBounds().getHeight();
        double contentW = mapPane.getPrefWidth() * currentZoom;
        double contentH = mapPane.getPrefHeight() * currentZoom;
        double rcx = routeCenterXInPane * currentZoom;
        double rcy = routeCenterYInPane * currentZoom;
        if (contentW > vpW) {
            mapScrollPane.setHvalue(Math.max(0, Math.min(1, (rcx - vpW / 2.0) / (contentW - vpW))));
        }
        if (contentH > vpH) {
            mapScrollPane.setVvalue(Math.max(0, Math.min(1, (rcy - vpH / 2.0) / (contentH - vpH))));
        }
    }

    /**
     * Zooms and pans so the activity's route fits neatly inside the viewport.
     * Falls back to fitMapToViewport when there are no projected points.
     */
    private void fitMapToRoute(List<TrackPoint> pts, double imgW, double imgH) {
        double vpW = mapScrollPane.getViewportBounds().getWidth();
        double vpH = mapScrollPane.getViewportBounds().getHeight();
        if (vpW <= 0 || vpH <= 0) {
            return;
        }

        if (pts == null || pts.size() < 2 || currentProjection == null) {
            fitMapToViewport(imgW, imgH);
            return;
        }

        // Bounding box of the route in image-pixel space
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (TrackPoint tp : pts) {
            Point2D p = currentProjection.project(tp);
            if (p.getX() < minX) {
                minX = p.getX();
            }
            if (p.getX() > maxX) {
                maxX = p.getX();
            }
            if (p.getY() < minY) {
                minY = p.getY();
            }
            if (p.getY() > maxY) {
                maxY = p.getY();
            }
        }

        double routeW = Math.max(maxX - minX, 50);
        double routeH = Math.max(maxY - minY, 50);

        // Store route bounding-box centre so zoom buttons can keep it centred.
        routeCenterXInPane = (minX + maxX) / 2.0;
        routeCenterYInPane = (minY + maxY) / 2.0;

        // Scale to make the route fill the viewport with padding
        double PAD = 80;
        double scale = Math.min((vpW - PAD) / routeW, (vpH - PAD) / routeH);
        applyZoom(scale);   // clamps to ZOOM_MIN/MAX internally

        // After applyZoom, a pixel at (px,py) in mapPane sits at (px*currentZoom, py*currentZoom)
        // in the content coordinate space (because applyZoom normalises the pivot).
        double rcx = (minX + maxX) / 2.0 * currentZoom;
        double rcy = (minY + maxY) / 2.0 * currentZoom;
        double contentW = imgW * currentZoom;
        double contentH = imgH * currentZoom;

        if (contentW > vpW) {
            double hv = (rcx - vpW / 2.0) / (contentW - vpW);
            mapScrollPane.setHvalue(Math.max(0, Math.min(1, hv)));
        }
        if (contentH > vpH) {
            double vv = (rcy - vpH / 2.0) / (contentH - vpH);
            mapScrollPane.setVvalue(Math.max(0, Math.min(1, vv)));
        }
    }

    private List<Double> computeSegmentSpeeds(List<TrackPoint> pts) {
        List<Double> speeds = new ArrayList<>();
        for (int i = 0; i < pts.size() - 1; i++) {
            double speed = pts.get(i).speedTo(pts.get(i + 1));
            if (Double.isNaN(speed) || Double.isInfinite(speed) || speed < 0) {
                speed = 0.0;
            }
            speeds.add(speed);
        }
        return speeds;
    }

    private Color colorForSpeed(double speed, double minSpeed, double maxSpeed) {
        double norm = (maxSpeed - minSpeed) <= 1e-6 ? 0.5 : (speed - minSpeed) / (maxSpeed - minSpeed);
        norm = Math.max(0, Math.min(1, norm));
        // Gradient red -> yellow -> green
        if (norm < 0.5) {
            double t = norm * 2.0;
            return Color.color(1.0, 0.2 + 0.8 * t, 0.2);
        }
        double t = (norm - 0.5) * 2.0;
        return Color.color(1.0 - 0.7 * t, 1.0, 0.2);
    }

    private void renderElevationProfile() {
        if (elevationChartPane == null) {
            return;
        }
        elevationChartPane.getChildren().clear();

        List<TrackPoint> pts = currentProfileTrackPoints;
        if (pts == null || pts.size() < 2) {
            profileHoverLabel.setText("Sin datos de perfil");
            elevationChartPane.setOnMouseMoved(null);
            elevationChartPane.setOnMouseExited(null);
            return;
        }

        double w = elevationChartPane.getWidth();
        double h = elevationChartPane.getHeight();
        if (w < 120 || h < 90) {
            return;
        }

        double left = 44, right = 12, top = 12, bottom = 26;
        double plotW = Math.max(1, w - left - right);
        double plotH = Math.max(1, h - top - bottom);

        double[] distKm = new double[pts.size()];
        double[] elev = new double[pts.size()];
        double totalKm = 0.0;
        double minElev = Double.MAX_VALUE;
        double maxElev = -Double.MAX_VALUE;

        for (int i = 0; i < pts.size(); i++) {
            if (i > 0) {
                totalKm += pts.get(i - 1).distanceTo(pts.get(i)) / 1000.0;
            }
            distKm[i] = totalKm;
            elev[i] = pts.get(i).getElevation();
            minElev = Math.min(minElev, elev[i]);
            maxElev = Math.max(maxElev, elev[i]);
        }
        if (Math.abs(maxElev - minElev) < 1e-6) {
            maxElev += 1;
            minElev -= 1;
        }
        double elevRange = maxElev - minElev;
        final double maxElevFinal = maxElev;
        final double elevRangeFinal = elevRange;

        Rectangle bg = new Rectangle(0, 0, w, h);
        bg.setFill(Color.web("#111722"));
        bg.setArcWidth(10);
        bg.setArcHeight(10);

        Line axisX = new Line(left, h - bottom, w - right, h - bottom);
        Line axisY = new Line(left, top, left, h - bottom);
        axisX.setStroke(Color.web("#3a4a63"));
        axisY.setStroke(Color.web("#3a4a63"));

        Polyline profileLine = new Polyline();
        profileLine.setStroke(Color.web("#60a5fa"));
        profileLine.setStrokeWidth(2.2);
        profileLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
        profileLine.setStrokeLineCap(StrokeLineCap.ROUND);

        for (int i = 0; i < pts.size(); i++) {
            double x = left + (distKm[i] / Math.max(totalKm, 0.001)) * plotW;
            double y = top + ((maxElevFinal - elev[i]) / elevRangeFinal) * plotH;
            profileLine.getPoints().addAll(x, y);
        }

        List<Node> ticks = new ArrayList<>();
        int yTicks = 4;
        for (int i = 0; i <= yTicks; i++) {
            double t = i / (double) yTicks;
            double y = top + t * plotH;
            double val = maxElev - t * elevRange;
            Line grid = new Line(left, y, w - right, y);
            grid.setStroke(Color.web("#223047"));
            grid.getStrokeDashArray().addAll(2.0, 4.0);
            Label lbl = new Label(String.format("%.0f", val));
            lbl.setTextFill(Color.web("#8a93a8"));
            lbl.setStyle("-fx-font-size: 10px;");
            lbl.relocate(4, y - 8);
            ticks.add(grid);
            ticks.add(lbl);
        }

        int xTicks = 5;
        for (int i = 0; i <= xTicks; i++) {
            double t = i / (double) xTicks;
            double x = left + t * plotW;
            double km = t * totalKm;
            Line tick = new Line(x, h - bottom, x, h - bottom + 4);
            tick.setStroke(Color.web("#3a4a63"));
            Label lbl = new Label(String.format("%.1f", km));
            lbl.setTextFill(Color.web("#8a93a8"));
            lbl.setStyle("-fx-font-size: 10px;");
            lbl.relocate(x - 12, h - bottom + 6);
            ticks.add(tick);
            ticks.add(lbl);
        }

        Label xAxisLabel = new Label("Distancia (km)");
        xAxisLabel.setTextFill(Color.web("#8a93a8"));
        xAxisLabel.setStyle("-fx-font-size: 10px;");
        xAxisLabel.relocate(left + plotW / 2 - 43, h - 16);

        Label yAxisLabel = new Label("Altitud (m)");
        yAxisLabel.setTextFill(Color.web("#8a93a8"));
        yAxisLabel.setStyle("-fx-font-size: 10px;");
        yAxisLabel.relocate(left + 4, top + 3);

        Line hoverLine = new Line(left, top, left, h - bottom);
        hoverLine.setStroke(Color.web("#facc15"));
        hoverLine.getStrokeDashArray().addAll(4.0, 3.0);
        hoverLine.setVisible(false);

        Circle hoverDot = new Circle(0, 0, 4.5, Color.web("#facc15"));
        hoverDot.setStroke(Color.WHITE);
        hoverDot.setStrokeWidth(1.5);
        hoverDot.setVisible(false);

        elevationChartPane.getChildren().add(bg);
        elevationChartPane.getChildren().addAll(ticks);
        elevationChartPane.getChildren().addAll(axisX, axisY, profileLine, hoverLine, hoverDot, xAxisLabel, yAxisLabel);

        final double totalKmFinal = Math.max(totalKm, 0.001);
        elevationChartPane.setOnMouseMoved(e -> {
            double mx = Math.max(left, Math.min(w - right, e.getX()));
            double targetKm = ((mx - left) / plotW) * totalKmFinal;
            int idx = findClosestTrackPointIndex(distKm, targetKm);
            double px = left + (distKm[idx] / totalKmFinal) * plotW;
            double py = top + ((maxElevFinal - elev[idx]) / elevRangeFinal) * plotH;
            hoverLine.setStartX(px);
            hoverLine.setEndX(px);
            hoverLine.setVisible(true);
            hoverDot.setCenterX(px);
            hoverDot.setCenterY(py);
            hoverDot.setVisible(true);
            updateMapHoverAndLabel(pts, idx, distKm[idx], elev[idx]);
        });
        elevationChartPane.setOnMouseExited(e -> {
            hoverLine.setVisible(false);
            hoverDot.setVisible(false);
            if (mapHoverMarker != null) {
                mapHoverMarker.setVisible(false);
            }
            profileHoverLabel.setText("Pasa el ratón por el perfil para resaltar el punto en el mapa");
        });
    }

    private void updateMapHoverAndLabel(List<TrackPoint> pts, int idx, double km, double elev) {
        profileHoverLabel.setText(String.format("Km %.2f  ·  %.0f m", km, elev));
        if (currentProjection == null || mapHoverMarker == null || idx < 0 || idx >= pts.size()) {
            return;
        }
        Point2D p = currentProjection.project(pts.get(idx));
        mapHoverMarker.setCenterX(p.getX());
        mapHoverMarker.setCenterY(p.getY());
        mapHoverMarker.setVisible(true);
    }

    private int findClosestTrackPointIndex(double[] distancesKm, double targetKm) {
        int pos = Arrays.binarySearch(distancesKm, targetKm);
        if (pos >= 0) {
            return pos;
        }
        int ins = -pos - 1;
        if (ins <= 0) {
            return 0;
        }
        if (ins >= distancesKm.length) {
            return distancesKm.length - 1;
        }
        double d1 = Math.abs(distancesKm[ins] - targetKm);
        double d0 = Math.abs(distancesKm[ins - 1] - targetKm);
        return d1 < d0 ? ins : ins - 1;
    }

    private void restoreMapRegionLabel() {
        if (selectedActivity == null) {
            mapRegionLabel.setText("");
            return;
        }
        MapRegion region = selectedActivity.getSuggestedMap();
        if (region == null) {
            region = app.findMapForActivity(selectedActivity);
        }
        mapRegionLabel.setText(region != null ? "📍 " + region.getName() : "⚠ Sin mapa asignado");
    }

    /**
     * Reloads {@code selectedActivity} from the database so that any changes
     * persisted by the library (e.g. added/removed annotations) are visible
     * in the in-memory object before re-rendering the map.
     */
    private void refreshSelectedActivity() {
        if (selectedActivity == null) return;
        long actId = selectedActivity.getId();
        List<Activity> all = app.getUserActivities();
        if (all == null) return;
        for (Activity a : all) {
            if (a.getId() == actId) {
                selectedActivity = a;
                return;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAP ZOOM CONTROLS
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleZoomIn() {
        if (mapGroup == null) {
            return;
        }
        applyZoom(currentZoom * ZOOM_STEP);
        centerViewportOnRoute();
    }

    @FXML
    private void handleZoomOut() {
        if (mapGroup == null) {
            return;
        }
        applyZoom(currentZoom / ZOOM_STEP);
        centerViewportOnRoute();
    }

    @FXML
    private void handleZoomFit() {
        if (selectedActivity == null || mapGroup == null || mapPane == null) {
            return;
        }
        double imgW = mapPane.getPrefWidth();
        double imgH = mapPane.getPrefHeight();
        if (imgW <= 0) {
            imgW = 600;
        }
        if (imgH <= 0) {
            imgH = 400;
        }
        List<TrackPoint> pts = selectedActivity.getTrackPoints();
        fitMapToRoute(pts, imgW, imgH);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANNOTATION — right-click context menu & dialog
    // ══════════════════════════════════════════════════════════════════════
    private void showAnnotationContextMenu(GeoPoint geo, double screenX, double screenY) {
        if (activeAnnotationMenu != null && activeAnnotationMenu.isShowing()) {
            activeAnnotationMenu.hide();
        }
        ContextMenu menu = new ContextMenu();

        MenuItem addPoint = new MenuItem("● Añadir Punto");
        MenuItem addText = new MenuItem("T  Añadir Texto");
        MenuItem addLine = new MenuItem("—  Añadir Línea");
        MenuItem addCircle = new MenuItem("○  Añadir Círculo");
        MenuItem cancel = new MenuItem("✕ Cancelar anotación");

        addPoint.setOnAction(e -> showAnnotationDialog(AnnotationType.POINT, geo, null));
        addText.setOnAction(e -> showAnnotationDialog(AnnotationType.TEXT, geo, null));
        addLine.setOnAction(e -> startTwoPointAnnotation(AnnotationType.LINE, geo));
        addCircle.setOnAction(e -> startTwoPointAnnotation(AnnotationType.CIRCLE, geo));
        cancel.setOnAction(e -> cancelPendingAnnotationMode());

        if (annotationPlacementMode) {
            menu.getItems().add(cancel);
        } else {
            menu.getItems().addAll(addPoint, addText, new SeparatorMenuItem(), addLine, addCircle);
        }

        activeAnnotationMenu = menu;
        menu.setOnHidden(e -> {
            if (activeAnnotationMenu == menu) {
                activeAnnotationMenu = null;
            }
        });
        // Use mapScrollPane as owner (stable across renderMap calls)
        menu.show(mapScrollPane, screenX, screenY);
    }

    private void showAnnotationDialog(AnnotationType type, GeoPoint point1, GeoPoint point2) {
        Dialog<Annotation> dialog = new Dialog<>();
        dialog.setTitle("Nueva anotación");
        dialog.setHeaderText("Tipo: " + typeName(type));
        if (mapScrollPane.getScene() != null && mapScrollPane.getScene().getWindow() != null) {
            dialog.initOwner(mapScrollPane.getScene().getWindow());
        }

        ButtonType saveBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField textField = new TextField();
        textField.setPromptText("Texto de la anotación (opcional)");
        ColorPicker colorPicker = new ColorPicker(Color.web("#f59e0b"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Texto:"), 0, 0);
        grid.add(textField, 1, 0);
        grid.add(new Label("Color:"), 0, 1);
        grid.add(colorPicker, 1, 1);
        GridPane.setHgrow(textField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(360);
        Platform.runLater(textField::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                Color c = colorPicker.getValue();
                String hex = String.format("#%02X%02X%02X",
                        (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
                List<GeoPoint> pts = new ArrayList<>();
                pts.add(point1);
                if (point2 != null) {
                    pts.add(point2);
                }
                return new Annotation(type, textField.getText(), hex, 2.0, pts);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(ann -> {
            app.addAnnotation(selectedActivity, ann);
            refreshSelectedActivity();
            populateAnnotations(selectedActivity);
            preserveScrollOnRender = true;
            renderMap();
        });
    }

    /**
     * For LINE/CIRCLE: inform user and wait for a second left-click on the map.
     */
    private void startTwoPointAnnotation(AnnotationType type, GeoPoint firstPoint) {
        annotationPlacementMode = true;
        // Apply CROSSHAIR cursor at every level so it shows regardless of which child the mouse is over
        mapScrollPane.setCursor(Cursor.CROSSHAIR);
        if (mapContentGroup != null) mapContentGroup.setCursor(Cursor.CROSSHAIR);
        if (mapGroup != null) mapGroup.setCursor(Cursor.CROSSHAIR);
        if (mapPane != null) mapPane.setCursor(Cursor.CROSSHAIR);
        mapRegionLabel.setText("📍 Clic izquierdo para fijar " + typeName(type) + " · clic derecho/ESC para cancelar");

        javafx.event.EventHandler<MouseEvent> secondPointPressHandler = evt -> {
            if (selectedActivity == null) {
                return;
            }
            if (evt.getButton() == MouseButton.PRIMARY) {
                GeoPoint secondPoint = eventToGeo(evt);
                annotationPlacementMode = false;
                setupMapRightClick();
                mapScrollPane.setCursor(Cursor.OPEN_HAND);
                if (mapContentGroup != null) mapContentGroup.setCursor(Cursor.OPEN_HAND);
                if (mapGroup != null) mapGroup.setCursor(Cursor.OPEN_HAND);
                if (mapPane != null) mapPane.setCursor(Cursor.OPEN_HAND);
                restoreMapRegionLabel();
                showAnnotationDialog(type, firstPoint, secondPoint);
                evt.consume();
            } else if (evt.getButton() == MouseButton.SECONDARY) {
                cancelPendingAnnotationMode();
                evt.consume();
            }
        };
        // Setting the handler only on mapPane is sufficient: events from all its children bubble up to it.
        // We also set it on mapScrollPane to catch clicks that land on the scroll-pane viewport but outside mapPane.
        if (mapPane != null) {
            mapPane.setOnMousePressed(secondPointPressHandler);
        }
        mapScrollPane.setOnMousePressed(secondPointPressHandler);
    }

    private void cancelPendingAnnotationMode() {
        annotationPlacementMode = false;
        mapScrollPane.setCursor(Cursor.OPEN_HAND);
        if (mapContentGroup != null) mapContentGroup.setCursor(Cursor.OPEN_HAND);
        if (mapGroup != null) mapGroup.setCursor(Cursor.OPEN_HAND);
        if (mapPane != null) mapPane.setCursor(Cursor.OPEN_HAND);
        restoreMapRegionLabel();
        setupMapRightClick();
    }


    private String typeName(AnnotationType t) {
        return switch (t) {
            case POINT ->
                "Punto";
            case TEXT ->
                "Texto";
            case LINE ->
                "Línea";
            case CIRCLE ->
                "Círculo";
        };
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ANNOTATION LIST
    // ══════════════════════════════════════════════════════════════════════
    private void populateAnnotations(Activity activity) {
        annotContainer.getChildren().clear();
        List<Annotation> anns = activity.getAnnotations();
        if (anns == null) {
            anns = List.of();
        }

        long nPunto = anns.stream().filter(a -> a.getType() == AnnotationType.POINT).count();
        long nTexto = anns.stream().filter(a -> a.getType() == AnnotationType.TEXT).count();
        long nLinea = anns.stream().filter(a -> a.getType() == AnnotationType.LINE).count();
        long nCirculo = anns.stream().filter(a -> a.getType() == AnnotationType.CIRCLE).count();

        annotTitle.setText("Anotaciones (" + anns.size() + ")");
        cntPunto.setText("● Punto (" + nPunto + ")");
        cntTexto.setText("T Texto (" + nTexto + ")");
        cntLinea.setText("— Línea (" + nLinea + ")");
        cntCirculo.setText("○ Círculo (" + nCirculo + ")");

        for (Annotation ann : anns) {
            annotContainer.getChildren().add(buildAnnotRow(ann, activity));
        }
    }

    private Node buildAnnotRow(Annotation ann, Activity activity) {
        Label dot = new Label("●");
        String color = ann.getColor() != null ? ann.getColor() : "#f59e0b";
        dot.setStyle("-fx-text-fill: " + color + ";");
        dot.getStyleClass().add("annot-row-dot");

        Label text = new Label(ann.getText() != null && !ann.getText().isBlank()
                ? ann.getText() : "(" + typeName(ann.getType()) + ")");
        text.getStyleClass().add("annot-row-text");

        String kmInfo = computeKmInfo(ann, activity);
        Label km = new Label(kmInfo);
        km.getStyleClass().add("annot-row-km");

        VBox info = new VBox(2, text, km);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button del = new Button("✕");
        del.getStyleClass().add("annot-row-menu-btn");
        del.setOnAction(e -> {
            app.removeAnnotation(ann);
            refreshSelectedActivity();
            populateAnnotations(selectedActivity);
            preserveScrollOnRender = true;
            renderMap();
        });

        HBox row = new HBox(10, dot, info, del);
        row.getStyleClass().add("annot-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private String computeKmInfo(Annotation ann, Activity activity) {
        if (ann.getGeoPoints() == null || ann.getGeoPoints().isEmpty()) {
            return "—";
        }
        if (activity.getTrackPoints() == null || activity.getTrackPoints().isEmpty()) {
            return "—";
        }
        GeoPoint gp = ann.getGeoPoints().get(0);
        double accDist = 0, bestDist = Double.MAX_VALUE, bestAcc = 0;
        List<TrackPoint> tps = activity.getTrackPoints();
        for (int i = 0; i < tps.size(); i++) {
            if (i > 0) {
                accDist += tps.get(i - 1).distanceTo(tps.get(i));
            }
            double d = Math.hypot(tps.get(i).getLatitude() - gp.getLatitude(),
                    tps.get(i).getLongitude() - gp.getLongitude());
            if (d < bestDist) {
                bestDist = d;
                bestAcc = accDist;
            }
        }
        return String.format("Km %.1f", bestAcc / 1000.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DETAIL MENU (rename / delete)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleDetailMenu() {
        if (selectedActivity == null) {
            return;
        }
        ContextMenu menu = new ContextMenu();

        MenuItem rename = new MenuItem("✏  Renombrar actividad");
        MenuItem delete = new MenuItem("🗑  Eliminar actividad");

        rename.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(selectedActivity.getName());
            dlg.setTitle("Renombrar actividad");
            dlg.setHeaderText(null);
            dlg.setContentText("Nuevo nombre:");
            dlg.showAndWait().ifPresent(newName -> {
                if (!newName.isBlank()) {
                    app.renameActivity(selectedActivity, newName);
                    detailName.setText(newName);
                    renderPage();
                }
            });
        });

        delete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar actividad");
            confirm.setHeaderText(null);
            confirm.setContentText("¿Eliminar \"" + selectedActivity.getName() + "\"?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    app.removeActivity(selectedActivity);
                    selectedActivity = null;
                    emptyDetail.setVisible(true);
                    emptyDetail.setManaged(true);
                    detailContent.setVisible(false);
                    detailContent.setManaged(false);
                    populateMonthStats();
                    loadAllActivities();
                }
            });
        });

        menu.getItems().addAll(rename, delete);
        double screenX = detailMenuBtn.localToScreen(detailMenuBtn.getBoundsInLocal()).getMinX();
        double screenY = detailMenuBtn.localToScreen(detailMenuBtn.getBoundsInLocal()).getMaxY() + 4;
        menu.show(detailMenuBtn, screenX, screenY);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  IMPORT GPX
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleImportGPX() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona fichero GPX");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GPX Files", "*.gpx"));
        Stage stage = (Stage) importGpxBtn.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            Activity imported = app.importActivity(file);
            if (imported != null) {
                populateMonthStats();
                loadAllActivities();
                selectActivity(imported);
            } else {
                new Alert(Alert.AlertType.ERROR, "No se pudo importar el fichero GPX.",
                        ButtonType.OK).showAndWait();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SEARCH + PAGINATION
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleSearch(KeyEvent event) {
        String q = searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Activity> all = app.getUserActivities();
        if (all == null) {
            all = List.of();
        }
        filteredList = all.stream()
                .filter(a -> a.getStartTime() != null)
                .filter(a -> q.isEmpty() || (a.getName() != null
                && a.getName().toLowerCase(Locale.ROOT).contains(q)))
                .sorted(Comparator.comparing(Activity::getStartTime).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
        currentPage = 0;
        renderPage();
    }

    @FXML
    private void handleFilter() {
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNextPage() {
        int total = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        if (currentPage < total - 1) {
            currentPage++;
            renderPage();
        }
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

    // ══════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════════
    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            activitiesBtn.getScene().setRoot(root);
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
    }

    @FXML
    private void handleHistorial() {
        setActiveBtn(historialBtn);
        navigateTo("/runningLaSafor/views/Historial.fxml");
    }

    @FXML
    private void handleMapas() {
        setActiveBtn(mapasBtn);
        navigateTo("/runningLaSafor/views/GestionMapas.fxml");
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

    // ══════════════════════════════════════════════════════════════════════
    //  TOP BAR ACTIONS
    // ══════════════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private String formatHMS(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * Loads an image file using Java's ImageIO instead of JavaFX's native image
     * loader, avoiding the javafx_iio native DLL dependency.
     */
    static Image loadImageSafe(File file) throws Exception {
        java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(file);
        if (bi == null) {
            throw new java.io.IOException("ImageIO could not read: " + file.getName());
        }
        return bufferedToFxImage(bi);
    }

    /** Overload that accepts an InputStream, e.g. for classpath resources. */
    static Image loadImageSafe(java.io.InputStream stream) throws Exception {
        java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(stream);
        if (bi == null) {
            throw new java.io.IOException("ImageIO could not read stream");
        }
        return bufferedToFxImage(bi);
    }

    private static Image bufferedToFxImage(java.awt.image.BufferedImage bi) {
        java.awt.image.BufferedImage argb = new java.awt.image.BufferedImage(
                bi.getWidth(), bi.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = argb.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        int w = argb.getWidth(), h = argb.getHeight();
        int[] pixels = new int[w * h];
        argb.getRGB(0, 0, w, h, pixels, 0, w);
        javafx.scene.image.WritableImage fxImg = new javafx.scene.image.WritableImage(w, h);
        fxImg.getPixelWriter().setPixels(
                0, 0, w, h,
                javafx.scene.image.PixelFormat.getIntArgbInstance(),
                pixels, 0, w);
        return fxImg;
    }
}
