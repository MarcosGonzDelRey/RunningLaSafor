package runningLaSafor.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import upv.ipc.sportlib.Activity;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Controller for ActivityRow.fxml
 * Populates a single activity card row in the dashboard's "Actividades Recientes" list.
 */
public class ActivityRowController {

    // ─── FXML nodes ──────────────────────────────────────────────────────
    @FXML private Label  activityName;
    @FXML private Label  activityDate;
    @FXML private Label  activityDistance;
    @FXML private Label  activityDuration;
    @FXML private Label  activityElevation;
    @FXML private Button viewBtn;

    // ─── Internal state ──────────────────────────────────────────────────
    private Activity activity;
    private Consumer<Activity> onSelect;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm", new Locale("es", "ES"));

    // ─── Public API ──────────────────────────────────────────────────────

    /**
     * Bind an {@link Activity} to this row and register a callback
     * that fires when the user clicks the "›" button.
     *
     * @param activity the activity to display
     * @param onSelect callback receiving the clicked activity
     */
    public void setActivity(Activity activity, Consumer<Activity> onSelect) {
        this.activity = activity;
        this.onSelect = onSelect;
        populate();
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    private void populate() {
        // Name
        activityName.setText(activity.getName() != null
                ? activity.getName()
                : "Actividad sin nombre");

        // Date/time
        if (activity.getStartTime() != null) {
            activityDate.setText(activity.getStartTime().format(DT_FORMATTER));
        } else {
            activityDate.setText("—");
        }

        // Distance
        double km = activity.getTotalDistance() / 1000.0;
        activityDistance.setText(String.format("%.2f km", km));

        // Duration
        if (activity.getDuration() != null) {
            long secs = activity.getDuration().getSeconds();
            long h = secs / 3600;
            long m = (secs % 3600) / 60;
            long s = secs % 60;
            activityDuration.setText(String.format("%02d:%02d:%02d", h, m, s));
        } else {
            activityDuration.setText("—");
        }

        // Elevation gain
        activityElevation.setText(String.format("%.0f m", activity.getElevationGain()));
    }

    // ─── FXML actions ────────────────────────────────────────────────────

    @FXML
    private void handleView() {
        if (onSelect != null && activity != null) {
            onSelect.accept(activity);
        }
    }
}
