package com.example.sensorinventory;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SensorInventory";

    private SensorManager sensorManager;
    private RecyclerView sensorRecyclerView;
    private SensorAdapter sensorAdapter;
    private TextView deviceInfoText;
    private TextView sensorCountText;
    private TextView emptyStateText;
    private TextView comparisonText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        populateDeviceInfo();
        populateSensorInventory();
        populateComparison();
    }

    private void initializeViews() {
        sensorRecyclerView = findViewById(R.id.sensorRecyclerView);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        sensorCountText = findViewById(R.id.sensorCountText);
        emptyStateText = findViewById(R.id.emptyStateText);
        comparisonText = findViewById(R.id.comparisonText);

        sensorRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sensorAdapter = new SensorAdapter();
        sensorRecyclerView.setAdapter(sensorAdapter);
    }

    private void populateDeviceInfo() {
        StringBuilder info = new StringBuilder();
        
        // Device model
        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        info.append(String.format(Locale.getDefault(), "%s %s", manufacturer, model));
        info.append("\n");

        // Android version
        String androidVersion = Build.VERSION.RELEASE;
        int sdkInt = Build.VERSION.SDK_INT;
        info.append(String.format(Locale.getDefault(), "Android %s (API %d)", androidVersion, sdkInt));
        info.append("\n");

        // Environment detection
        boolean isEmulator = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
                || "goldfish".equals(Build.HARDWARE)
                || "ranchu".equals(Build.HARDWARE);

        String environment = isEmulator ? getString(R.string.environment_emulator) : getString(R.string.environment_real);
        info.append(String.format(Locale.getDefault(), "Environment: %s", environment));
        info.append("\n");

        // Fingerprint for reference
        info.append(String.format(Locale.getDefault(), "Build Fingerprint: %s", Build.FINGERPRINT));

        deviceInfoText.setText(info.toString());
    }

    private void populateSensorInventory() {
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        
        if (sensors == null || sensors.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            sensorRecyclerView.setVisibility(View.GONE);
            sensorCountText.setText("0");
            return;
        }

        emptyStateText.setVisibility(View.GONE);
        sensorRecyclerView.setVisibility(View.VISIBLE);
        sensorCountText.setText(String.valueOf(sensors.size()));

        sensorAdapter.updateSensors(sensors);
    }

    private void populateComparison() {
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder comparison = new StringBuilder();

        if (sensors == null || sensors.size() < 2) {
            comparison.append("Insufficient sensors for comparison (need at least 2).\n");
            comparison.append("Emulators typically provide limited sensor sets.\n");
            comparison.append("Physical devices offer richer sensor ecosystems for MavunoLink features.");
            comparisonText.setText(comparison.toString());
            return;
        }

        // Find accelerometer and gyroscope for comparison (common MavunoLink use case)
        Sensor accelerometer = null;
        Sensor gyroscope = null;
        Sensor magnetometer = null;
        Sensor light = null;
        Sensor pressure = null;

        for (Sensor sensor : sensors) {
            int type = sensor.getType();
            if (type == Sensor.TYPE_ACCELEROMETER && accelerometer == null) {
                accelerometer = sensor;
            } else if (type == Sensor.TYPE_GYROSCOPE && gyroscope == null) {
                gyroscope = sensor;
            } else if (type == Sensor.TYPE_MAGNETIC_FIELD && magnetometer == null) {
                magnetometer = sensor;
            } else if (type == Sensor.TYPE_LIGHT && light == null) {
                light = sensor;
            } else if (type == Sensor.TYPE_PRESSURE && pressure == null) {
                pressure = sensor;
            }
        }

        // Primary comparison: Accelerometer vs Gyroscope for motion tracking in MavunoLink
        comparison.append("Comparison: Accelerometer vs Gyroscope for MavunoLink Motion Tracking\n\n");

        if (accelerometer != null && gyroscope != null) {
            comparison.append(formatSensorDetails("Accelerometer", accelerometer));
            comparison.append("\n");
            comparison.append(formatSensorDetails("Gyroscope", gyroscope));
            comparison.append("\n");

            comparison.append("Justification for MavunoLink:\n");
            comparison.append("• Accelerometer: Low power (~0.5-2 mA), always-on capable, detects linear motion, orientation, step counting. Essential for baseline activity recognition.\n");
            comparison.append("• Gyroscope: Higher power (~3-6 mA), measures rotational velocity, enables precise gesture detection and dead-reckoning. Complements accelerometer for 6-DoF tracking.\n");
            comparison.append("• Combined: Sensor fusion (accelerometer + gyroscope) provides robust motion context for MavunoLink's agricultural tracking (equipment movement, vehicle vibration, field navigation).\n");
            comparison.append("• Battery cost: Using both continuously ~5-8 mA. Duty-cycling and batching (FIFO) reduce average draw.\n");
            comparison.append("• Measurement quality: Accelerometer noise ~100-400 µg/√Hz; Gyroscope noise ~0.01-0.05 °/s/√Hz. Calibration and temperature compensation needed for field accuracy.\n");
            comparison.append("• Device availability: Both present on virtually all modern Android devices (API 3+). Emulators may lack gyroscope or provide synthetic data.\n");
        } else if (accelerometer != null && magnetometer != null) {
            comparison.append("Gyroscope unavailable. Comparing Accelerometer vs Magnetometer:\n\n");
            comparison.append(formatSensorDetails("Accelerometer", accelerometer));
            comparison.append("\n");
            comparison.append(formatSensorDetails("Magnetometer", magnetometer));
            comparison.append("\n");
            comparison.append("Note: Magnetometer provides heading but is susceptible to magnetic interference (equipment, power lines). Less reliable for precision motion tracking in agricultural environments.");
        } else if (accelerometer != null) {
            comparison.append("Only accelerometer available for motion tracking.\n");
            comparison.append(formatSensorDetails("Accelerometer", accelerometer));
            comparison.append("\n");
            comparison.append("Limitation: Without gyroscope, cannot distinguish rotation from linear acceleration accurately. Orientation estimates drift. Step counting and gross motion detection still viable for basic MavunoLink features.");
        } else {
            comparison.append("Core motion sensors unavailable. Environment may be emulator with limited sensor support.\n");
            comparison.append("Physical device testing required for meaningful MavunoLink sensor integration analysis.");
        }

        // Add note about three sensors requirement
        comparison.append("\n\n---\nThree-Sensor Metadata Summary:\n");
        int count = 0;
        for (Sensor sensor : sensors) {
            if (count >= 3) break;
            comparison.append(formatSensorDetails(getSensorTypeName(sensor.getType()), sensor));
            comparison.append("\n");
            count++;
        }

        comparisonText.setText(comparison.toString());
    }

    private String formatSensorDetails(String label, Sensor sensor) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(":\n");
        sb.append(String.format(Locale.getDefault(), "  %s%d (%s)", getString(R.string.sensor_type_prefix), sensor.getType(), getSensorTypeName(sensor.getType()))).append("\n");
        sb.append(String.format(Locale.getDefault(), "  %s%s", getString(R.string.sensor_vendor_prefix), sensor.getVendor() != null ? sensor.getVendor() : getString(R.string.na))).append("\n");
        sb.append(String.format(Locale.getDefault(), "  %s%.2f %s", getString(R.string.sensor_range_prefix), sensor.getMaximumRange(), getUnitForSensorType(sensor.getType()))).append("\n");
        sb.append(String.format(Locale.getDefault(), "  %s%.6f %s", getString(R.string.sensor_resolution_prefix), sensor.getResolution(), getUnitForSensorType(sensor.getType()))).append("\n");
        sb.append(String.format(Locale.getDefault(), "  %s%.2f %s", getString(R.string.sensor_power_prefix), sensor.getPower(), getString(R.string.unit_microampere))).append("\n");
        sb.append(String.format(Locale.getDefault(), "  %s%d %s", getString(R.string.sensor_min_delay_prefix), sensor.getMinDelay(), getString(R.string.unit_microsecond))).append("\n");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sb.append(String.format(Locale.getDefault(), "  %s%d %s", getString(R.string.sensor_max_delay_prefix), sensor.getMaxDelay(), getString(R.string.unit_microsecond))).append("\n");
            sb.append(String.format(Locale.getDefault(), "  %s%d", getString(R.string.sensor_fifo_reserved_prefix), sensor.getFifoReservedEventCount())).append("\n");
            sb.append(String.format(Locale.getDefault(), "  %s%d", getString(R.string.sensor_fifo_max_prefix), sensor.getFifoMaxEventCount())).append("\n");
            sb.append(String.format(Locale.getDefault(), "  %s%s", getString(R.string.sensor_string_type_prefix), sensor.getStringType() != null ? sensor.getStringType() : getString(R.string.na))).append("\n");
            sb.append(String.format(Locale.getDefault(), "  %s%s", getString(R.string.sensor_reporting_mode_prefix), getReportingModeName(sensor.getReportingMode()))).append("\n");
            sb.append(String.format(Locale.getDefault(), "  %s", sensor.isWakeUpSensor() ? getString(R.string.sensor_is_wakeup) : getString(R.string.sensor_not_wakeup))).append("\n");
        }
        
        return sb.toString();
    }

    private String getSensorTypeName(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER: return "Accelerometer";
            case Sensor.TYPE_GYROSCOPE: return "Gyroscope";
            case Sensor.TYPE_MAGNETIC_FIELD: return "Magnetometer";
            case Sensor.TYPE_ORIENTATION: return "Orientation (deprecated)";
            case Sensor.TYPE_LIGHT: return "Light";
            case Sensor.TYPE_PRESSURE: return "Pressure";
            case Sensor.TYPE_TEMPERATURE: return "Temperature (deprecated)";
            case Sensor.TYPE_PROXIMITY: return "Proximity";
            case Sensor.TYPE_GRAVITY: return "Gravity";
            case Sensor.TYPE_LINEAR_ACCELERATION: return "Linear Acceleration";
            case Sensor.TYPE_ROTATION_VECTOR: return "Rotation Vector";
            case Sensor.TYPE_RELATIVE_HUMIDITY: return "Relative Humidity";
            case Sensor.TYPE_AMBIENT_TEMPERATURE: return "Ambient Temperature";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "Magnetometer Uncalibrated";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: return "Gyroscope Uncalibrated";
            case Sensor.TYPE_SIGNIFICANT_MOTION: return "Significant Motion";
            case Sensor.TYPE_STEP_DETECTOR: return "Step Detector";
            case Sensor.TYPE_STEP_COUNTER: return "Step Counter";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR: return "Geomagnetic Rotation Vector";
            case Sensor.TYPE_HEART_RATE: return "Heart Rate";
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED: return "Accelerometer Uncalibrated";
            default: 
                // Handle newer sensor types with version checks
                if (Build.VERSION.SDK_INT >= 28) {
                    // API 28+
                    try {
                        if (type == Sensor.class.getField("TYPE_TILT_DETECTOR").getInt(null)) return "Tilt Detector";
                        if (type == Sensor.class.getField("TYPE_WAKE_GESTURE").getInt(null)) return "Wake Gesture";
                        if (type == Sensor.class.getField("TYPE_GLANCE_GESTURE").getInt(null)) return "Glance Gesture";
                        if (type == Sensor.class.getField("TYPE_PICK_UP_GESTURE").getInt(null)) return "Pick Up Gesture";
                        if (type == Sensor.class.getField("TYPE_WRIST_TILT_GESTURE").getInt(null)) return "Wrist Tilt Gesture";
                        if (type == Sensor.class.getField("TYPE_DEVICE_ORIENTATION").getInt(null)) return "Device Orientation";
                        if (type == Sensor.class.getField("TYPE_POSE_6DOF").getInt(null)) return "Pose 6DoF";
                        if (type == Sensor.class.getField("TYPE_STATIONARY_DETECT").getInt(null)) return "Stationary Detect";
                        if (type == Sensor.class.getField("TYPE_MOTION_DETECT").getInt(null)) return "Motion Detect";
                        if (type == Sensor.class.getField("TYPE_HEART_BEAT").getInt(null)) return "Heart Beat";
                        if (type == Sensor.class.getField("TYPE_DYNAMIC_SENSOR_META").getInt(null)) return "Dynamic Sensor Meta";
                        if (type == Sensor.class.getField("TYPE_ADDITIONAL_INFO").getInt(null)) return "Additional Info";
                        if (type == Sensor.class.getField("TYPE_LOW_LATENCY_OFFBODY_DETECT").getInt(null)) return "Low Latency Off-body Detect";
                        if (type == Sensor.class.getField("TYPE_HINGE_ANGLE").getInt(null)) return "Hinge Angle";
                        if (type == Sensor.class.getField("TYPE_HEAD_TRACKER").getInt(null)) return "Head Tracker";
                        if (type == Sensor.class.getField("TYPE_ACCELEROMETER_LIMITED_AXES").getInt(null)) return "Accelerometer Limited Axes";
                        if (type == Sensor.class.getField("TYPE_GYROSCOPE_LIMITED_AXES").getInt(null)) return "Gyroscope Limited Axes";
                    } catch (Exception e) {
                        // Ignore reflection errors
                    }
                }
                return "Unknown (" + type + ")";
        }
    }

    private String getUnitForSensorType(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return getString(R.string.unit_m_s2);
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return getString(R.string.unit_rad_s);
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return getString(R.string.unit_ut);
            case Sensor.TYPE_LIGHT:
                return getString(R.string.unit_lux);
            case Sensor.TYPE_PRESSURE:
                return getString(R.string.unit_pa);
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return getString(R.string.unit_celsius);
            case Sensor.TYPE_PROXIMITY:
                return "cm";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "%";
            case Sensor.TYPE_ORIENTATION:
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "radians";
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_STEP_DETECTOR:
                return "steps";
            case Sensor.TYPE_HEART_RATE:
            case Sensor.TYPE_HEART_BEAT:
                return "bpm";
            default:
                return getString(R.string.unit_si);
        }
    }

    private String getReportingModeName(int mode) {
        switch (mode) {
            case Sensor.REPORTING_MODE_CONTINUOUS: return getString(R.string.reporting_mode_continuous);
            case Sensor.REPORTING_MODE_ON_CHANGE: return getString(R.string.reporting_mode_on_change);
            case Sensor.REPORTING_MODE_ONE_SHOT: return getString(R.string.reporting_mode_one_shot);
            default: 
                if (Build.VERSION.SDK_INT >= 28 && mode == 4) { // REPORTING_MODE_SPECIAL = 4 (API 28+)
                    return getString(R.string.reporting_mode_special);
                }
                return "Unknown (" + mode + ")";
        }
    }

    // Adapter for RecyclerView
    private static class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {
        private List<Sensor> sensors = new ArrayList<>();

        void updateSensors(List<Sensor> newSensors) {
            sensors = newSensors != null ? newSensors : new ArrayList<>();
            notifyDataSetChanged();
        }

        @Override
        public SensorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sensor, parent, false);
            return new SensorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SensorViewHolder holder, int position) {
            Sensor sensor = sensors.get(position);
            holder.bind(sensor);
        }

        @Override
        public int getItemCount() {
            return sensors.size();
        }

        static class SensorViewHolder extends RecyclerView.ViewHolder {
            TextView sensorNameText;
            TextView sensorTypeText;
            View detailsContainer;
            TextView sensorVendorText;
            TextView sensorRangeText;
            TextView sensorResolutionText;
            TextView sensorPowerText;
            TextView sensorMinDelayText;
            TextView sensorMaxDelayText;
            TextView sensorFifoReservedText;
            TextView sensorFifoMaxText;
            TextView sensorStringTypeText;
            TextView sensorReportingModeText;
            TextView sensorWakeupText;
            TextView toggleDetailsText;

            SensorViewHolder(View itemView) {
                super(itemView);
                sensorNameText = itemView.findViewById(R.id.sensorNameText);
                sensorTypeText = itemView.findViewById(R.id.sensorTypeText);
                detailsContainer = itemView.findViewById(R.id.detailsContainer);
                sensorVendorText = itemView.findViewById(R.id.sensorVendorText);
                sensorRangeText = itemView.findViewById(R.id.sensorRangeText);
                sensorResolutionText = itemView.findViewById(R.id.sensorResolutionText);
                sensorPowerText = itemView.findViewById(R.id.sensorPowerText);
                sensorMinDelayText = itemView.findViewById(R.id.sensorMinDelayText);
                sensorMaxDelayText = itemView.findViewById(R.id.sensorMaxDelayText);
                sensorFifoReservedText = itemView.findViewById(R.id.sensorFifoReservedText);
                sensorFifoMaxText = itemView.findViewById(R.id.sensorFifoMaxText);
                sensorStringTypeText = itemView.findViewById(R.id.sensorStringTypeText);
                sensorReportingModeText = itemView.findViewById(R.id.sensorReportingModeText);
                sensorWakeupText = itemView.findViewById(R.id.sensorWakeupText);
                toggleDetailsText = itemView.findViewById(R.id.toggleDetailsText);
            }

            void bind(Sensor sensor) {
                Context context = itemView.getContext();
                sensorNameText.setText(sensor.getName());
                sensorTypeText.setText(String.format(Locale.getDefault(), "%s %d (%s)",
                        context.getString(R.string.sensor_type_prefix),
                        sensor.getType(),
                        getSensorTypeNameStatic(sensor.getType())));

                boolean isExpanded = detailsContainer.getVisibility() == View.VISIBLE;

                // Populate details
                sensorVendorText.setText(String.format(Locale.getDefault(), "%s %s",
                        context.getString(R.string.sensor_vendor_prefix),
                        sensor.getVendor() != null ? sensor.getVendor() : context.getString(R.string.na)));
                sensorRangeText.setText(String.format(Locale.getDefault(), "%s %.2f %s",
                        context.getString(R.string.sensor_range_prefix),
                        sensor.getMaximumRange(),
                        getUnitForSensorTypeStatic(context, sensor.getType())));
                sensorResolutionText.setText(String.format(Locale.getDefault(), "%s %.6f %s",
                        context.getString(R.string.sensor_resolution_prefix),
                        sensor.getResolution(),
                        getUnitForSensorTypeStatic(context, sensor.getType())));
                sensorPowerText.setText(String.format(Locale.getDefault(), "%s %.2f %s",
                        context.getString(R.string.sensor_power_prefix),
                        sensor.getPower(),
                        context.getString(R.string.unit_microampere)));
                sensorMinDelayText.setText(String.format(Locale.getDefault(), "%s %d %s",
                        context.getString(R.string.sensor_min_delay_prefix),
                        sensor.getMinDelay(),
                        context.getString(R.string.unit_microsecond)));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sensorMaxDelayText.setText(String.format(Locale.getDefault(), "%s %d %s",
                            context.getString(R.string.sensor_max_delay_prefix),
                            sensor.getMaxDelay(),
                            context.getString(R.string.unit_microsecond)));
                    sensorFifoReservedText.setText(String.format(Locale.getDefault(), "%s %d",
                            context.getString(R.string.sensor_fifo_reserved_prefix),
                            sensor.getFifoReservedEventCount()));
                    sensorFifoMaxText.setText(String.format(Locale.getDefault(), "%s %d",
                            context.getString(R.string.sensor_fifo_max_prefix),
                            sensor.getFifoMaxEventCount()));
                    sensorStringTypeText.setText(String.format(Locale.getDefault(), "%s %s",
                            context.getString(R.string.sensor_string_type_prefix),
                            sensor.getStringType() != null ? sensor.getStringType() : context.getString(R.string.na)));
                    sensorReportingModeText.setText(String.format(Locale.getDefault(), "%s %s",
                            context.getString(R.string.sensor_reporting_mode_prefix),
                            getReportingModeNameStatic(context, sensor.getReportingMode())));
                    sensorWakeupText.setText(sensor.isWakeUpSensor() ?
                            context.getString(R.string.sensor_is_wakeup) :
                            context.getString(R.string.sensor_not_wakeup));
                } else {
                    sensorMaxDelayText.setVisibility(View.GONE);
                    sensorFifoReservedText.setVisibility(View.GONE);
                    sensorFifoMaxText.setVisibility(View.GONE);
                    sensorStringTypeText.setVisibility(View.GONE);
                    sensorReportingModeText.setVisibility(View.GONE);
                    sensorWakeupText.setVisibility(View.GONE);
                }

                // Toggle details on click
                itemView.setOnClickListener(v -> {
                    if (detailsContainer.getVisibility() == View.VISIBLE) {
                        detailsContainer.setVisibility(View.GONE);
                        toggleDetailsText.setText(context.getString(R.string.show_details));
                    } else {
                        detailsContainer.setVisibility(View.VISIBLE);
                        toggleDetailsText.setText(context.getString(R.string.hide_details));
                    }
                });

                // Set initial state
                if (isExpanded) {
                    detailsContainer.setVisibility(View.VISIBLE);
                    toggleDetailsText.setText(context.getString(R.string.hide_details));
                } else {
                    detailsContainer.setVisibility(View.GONE);
                    toggleDetailsText.setText(context.getString(R.string.show_details));
                }
            }

            static String getSensorTypeNameStatic(int type) {
                switch (type) {
                    case Sensor.TYPE_ACCELEROMETER: return "Accelerometer";
                    case Sensor.TYPE_GYROSCOPE: return "Gyroscope";
                    case Sensor.TYPE_MAGNETIC_FIELD: return "Magnetometer";
                    case Sensor.TYPE_ORIENTATION: return "Orientation (deprecated)";
                    case Sensor.TYPE_LIGHT: return "Light";
                    case Sensor.TYPE_PRESSURE: return "Pressure";
                    case Sensor.TYPE_TEMPERATURE: return "Temperature (deprecated)";
                    case Sensor.TYPE_PROXIMITY: return "Proximity";
                    case Sensor.TYPE_GRAVITY: return "Gravity";
                    case Sensor.TYPE_LINEAR_ACCELERATION: return "Linear Acceleration";
                    case Sensor.TYPE_ROTATION_VECTOR: return "Rotation Vector";
                    case Sensor.TYPE_RELATIVE_HUMIDITY: return "Relative Humidity";
                    case Sensor.TYPE_AMBIENT_TEMPERATURE: return "Ambient Temperature";
                    case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "Magnetometer Uncalibrated";
                    case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: return "Gyroscope Uncalibrated";
                    case Sensor.TYPE_SIGNIFICANT_MOTION: return "Significant Motion";
                    case Sensor.TYPE_STEP_DETECTOR: return "Step Detector";
                    case Sensor.TYPE_STEP_COUNTER: return "Step Counter";
                    case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR: return "Geomagnetic Rotation Vector";
                    case Sensor.TYPE_HEART_RATE: return "Heart Rate";
                    case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED: return "Accelerometer Uncalibrated";
                    default: 
                        // Handle newer sensor types with reflection (API 28+)
                        if (Build.VERSION.SDK_INT >= 28) {
                            try {
                                if (type == Sensor.class.getField("TYPE_TILT_DETECTOR").getInt(null)) return "Tilt Detector";
                                if (type == Sensor.class.getField("TYPE_WAKE_GESTURE").getInt(null)) return "Wake Gesture";
                                if (type == Sensor.class.getField("TYPE_GLANCE_GESTURE").getInt(null)) return "Glance Gesture";
                                if (type == Sensor.class.getField("TYPE_PICK_UP_GESTURE").getInt(null)) return "Pick Up Gesture";
                                if (type == Sensor.class.getField("TYPE_WRIST_TILT_GESTURE").getInt(null)) return "Wrist Tilt Gesture";
                                if (type == Sensor.class.getField("TYPE_DEVICE_ORIENTATION").getInt(null)) return "Device Orientation";
                                if (type == Sensor.class.getField("TYPE_POSE_6DOF").getInt(null)) return "Pose 6DoF";
                                if (type == Sensor.class.getField("TYPE_STATIONARY_DETECT").getInt(null)) return "Stationary Detect";
                                if (type == Sensor.class.getField("TYPE_MOTION_DETECT").getInt(null)) return "Motion Detect";
                                if (type == Sensor.class.getField("TYPE_HEART_BEAT").getInt(null)) return "Heart Beat";
                                if (type == Sensor.class.getField("TYPE_DYNAMIC_SENSOR_META").getInt(null)) return "Dynamic Sensor Meta";
                                if (type == Sensor.class.getField("TYPE_ADDITIONAL_INFO").getInt(null)) return "Additional Info";
                                if (type == Sensor.class.getField("TYPE_LOW_LATENCY_OFFBODY_DETECT").getInt(null)) return "Low Latency Off-body Detect";
                                if (type == Sensor.class.getField("TYPE_HINGE_ANGLE").getInt(null)) return "Hinge Angle";
                                if (type == Sensor.class.getField("TYPE_HEAD_TRACKER").getInt(null)) return "Head Tracker";
                                if (type == Sensor.class.getField("TYPE_ACCELEROMETER_LIMITED_AXES").getInt(null)) return "Accelerometer Limited Axes";
                                if (type == Sensor.class.getField("TYPE_GYROSCOPE_LIMITED_AXES").getInt(null)) return "Gyroscope Limited Axes";
                            } catch (Exception e) {
                                // Ignore reflection errors
                            }
                        }
                        return "Unknown (" + type + ")";
                }
            }

            static String getUnitForSensorTypeStatic(Context context, int type) {
                switch (type) {
                    case Sensor.TYPE_ACCELEROMETER:
                    case Sensor.TYPE_GRAVITY:
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        return context.getString(R.string.unit_m_s2);
                    case Sensor.TYPE_GYROSCOPE:
                    case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                        return context.getString(R.string.unit_rad_s);
                    case Sensor.TYPE_MAGNETIC_FIELD:
                    case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                        return context.getString(R.string.unit_ut);
                    case Sensor.TYPE_LIGHT:
                        return context.getString(R.string.unit_lux);
                    case Sensor.TYPE_PRESSURE:
                        return context.getString(R.string.unit_pa);
                    case Sensor.TYPE_TEMPERATURE:
                    case Sensor.TYPE_AMBIENT_TEMPERATURE:
                        return context.getString(R.string.unit_celsius);
                    case Sensor.TYPE_PROXIMITY:
                        return "cm";
                    case Sensor.TYPE_RELATIVE_HUMIDITY:
                        return "%";
                    case Sensor.TYPE_ORIENTATION:
                    case Sensor.TYPE_ROTATION_VECTOR:
                    case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                        return "radians";
                    case Sensor.TYPE_STEP_COUNTER:
                    case Sensor.TYPE_STEP_DETECTOR:
                        return "steps";
                    case Sensor.TYPE_HEART_RATE:
                    case Sensor.TYPE_HEART_BEAT:
                        return "bpm";
                    default:
                        return context.getString(R.string.unit_si);
                }
            }

            static String getReportingModeNameStatic(Context context, int mode) {
                switch (mode) {
                    case Sensor.REPORTING_MODE_CONTINUOUS: return context.getString(R.string.reporting_mode_continuous);
                    case Sensor.REPORTING_MODE_ON_CHANGE: return context.getString(R.string.reporting_mode_on_change);
                    case Sensor.REPORTING_MODE_ONE_SHOT: return context.getString(R.string.reporting_mode_one_shot);
                    default: 
                        if (Build.VERSION.SDK_INT >= 28 && mode == 4) { // REPORTING_MODE_SPECIAL = 4 (API 28+)
                            return context.getString(R.string.reporting_mode_special);
                        }
                        return "Unknown (" + mode + ")";
                }
            }
        }
    }
}