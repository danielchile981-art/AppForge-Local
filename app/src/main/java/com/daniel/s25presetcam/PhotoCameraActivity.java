package com.daniel.s25presetcam;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoCameraActivity extends Activity {
    public static final String EXTRA_PRESET_ID = "photo_preset_id";

    private TextureView textureView;
    private View flashView;
    private TextView statusText;
    private TextView shutterButton;
    private PhotoPreset preset;
    private CameraManager cameraManager;
    private CameraCharacteristics characteristics;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private ImageReader imageReader;
    private Size photoSize;
    private boolean opening;
    private boolean captureInProgress;
    private boolean active;
    private int actualIso;
    private long actualExposureNs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String id = getIntent().getStringExtra(EXTRA_PRESET_ID);
        preset = PhotoPresetRepository.find(id == null ? "photo_daily" : id);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        setContentView(buildUi());
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(surfaceListener);
        root.addView(textureView, new FrameLayout.LayoutParams(-1, -1));

        flashView = new View(this);
        flashView.setBackgroundColor(Color.WHITE);
        flashView.setAlpha(0f);
        root.addView(flashView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(18), dp(18), dp(18), dp(15));
        top.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(225, 5, 8, 16), Color.argb(120, 5, 8, 16), Color.TRANSPARENT}
        ));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Color.WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        titleRow.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        titleColumn.addView(text(preset.name, 21, Color.WHITE, true));
        titleColumn.addView(text(preset.family, 11, Color.rgb(190, 199, 220), false));
        titleRow.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView badge = text(preset.lensLabel(), 13, Color.rgb(9, 14, 28), true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(11), dp(5), dp(11), dp(5));
        badge.setBackground(rounded(preset.accentColor, preset.accentColor, dp(20)));
        titleRow.addView(badge);
        top.addView(titleRow);

        TextView settings = text(preset.shortSettings(), 12, preset.accentColor, true);
        settings.setPadding(dp(48), dp(7), 0, 0);
        top.addView(settings);
        root.addView(top, new FrameLayout.LayoutParams(-1, dp(135), Gravity.TOP));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        bottom.setPadding(dp(20), dp(12), dp(20), dp(28));
        bottom.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.argb(240, 0, 0, 0), Color.argb(150, 0, 0, 0), Color.TRANSPARENT}
        ));

        statusText = text("Preparando câmera…", 12, Color.WHITE, false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(12), dp(8), dp(12), dp(8));
        bottom.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        shutterButton = text("●", 61, Color.WHITE, false);
        shutterButton.setGravity(Gravity.CENTER);
        shutterButton.setEnabled(false);
        shutterButton.setBackground(rounded(Color.argb(125, 255, 255, 255), preset.accentColor, dp(47)));
        shutterButton.setOnClickListener(v -> beginCountdown(preset.timerSeconds));
        LinearLayout.LayoutParams shutterParams = new LinearLayout.LayoutParams(dp(94), dp(94));
        shutterParams.topMargin = dp(10);
        bottom.addView(shutterButton, shutterParams);

        TextView hint = text("Toque para fotografar", 11, Color.rgb(190, 199, 220), false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(4);
        bottom.addView(hint, hintParams);
        root.addView(bottom, new FrameLayout.LayoutParams(-1, dp(225), Gravity.BOTTOM));
        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();
        active = true;
        startCameraThread();
        if (textureView.isAvailable()) openCamera();
    }

    @Override
    protected void onPause() {
        active = false;
        closeCamera();
        stopCameraThread();
        super.onPause();
    }

    private final TextureView.SurfaceTextureListener surfaceListener = new TextureView.SurfaceTextureListener() {
        @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { openCamera(); }
        @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { configureTransform(); }
        @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
        @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void startCameraThread() {
        cameraThread = new HandlerThread("PresetPhotoCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCameraThread() {
        if (cameraThread == null) return;
        cameraThread.quitSafely();
        try { cameraThread.join(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        cameraThread = null;
        cameraHandler = null;
    }

    private void openCamera() {
        if (opening || cameraDevice != null || !textureView.isAvailable()) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            finish();
            return;
        }
        opening = true;
        try {
            String cameraId = chooseCameraId(preset.frontCamera);
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
            prepareImageReader();
            cameraManager.openCamera(cameraId, stateCallback, cameraHandler);
        } catch (Exception error) {
            opening = false;
            showError("Não foi possível preparar a foto: " + error.getMessage());
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(CameraDevice camera) {
            opening = false;
            cameraDevice = camera;
            createPreviewSession();
        }
        @Override public void onDisconnected(CameraDevice camera) {
            opening = false;
            camera.close();
            cameraDevice = null;
        }
        @Override public void onError(CameraDevice camera, int error) {
            opening = false;
            camera.close();
            cameraDevice = null;
            showError("Erro da câmera: " + error);
        }
    };

    private String chooseCameraId(boolean front) throws CameraAccessException {
        String fallback = null;
        String bestLogical = null;
        int bestPhysicalCount = -1;
        for (String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            if (front && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) return id;
            if (!front && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                if (fallback == null) fallback = id;
                int count = c.getPhysicalCameraIds().size();
                if (count > bestPhysicalCount) {
                    bestPhysicalCount = count;
                    bestLogical = id;
                }
            }
        }
        if (front) throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Câmera frontal não encontrada");
        if (bestLogical != null) return bestLogical;
        if (fallback != null) return fallback;
        throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Câmera traseira não encontrada");
    }

    private void prepareImageReader() {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) throw new IllegalStateException("Configurações fotográficas indisponíveis");
        photoSize = closestPhotoSize(map.getOutputSizes(ImageFormat.JPEG), preset.megapixels);
        imageReader = ImageReader.newInstance(photoSize.getWidth(), photoSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(this::saveImage, cameraHandler);
        runOnUiThread(this::configureTransform);
    }

    private Size closestPhotoSize(Size[] choices, int targetMegapixels) {
        if (choices == null || choices.length == 0) return new Size(4000, 3000);
        long targetArea = targetMegapixels * 1_000_000L;
        Size best = choices[0];
        double bestScore = Double.MAX_VALUE;
        for (Size size : choices) {
            long area = (long) size.getWidth() * size.getHeight();
            double ratio = size.getWidth() / (double) size.getHeight();
            double ratioPenalty = Math.abs(ratio - 4d / 3d) * targetArea * 4;
            double score = Math.abs(area - targetArea) + ratioPenalty;
            if (score < bestScore) {
                bestScore = score;
                best = size;
            }
        }
        return best;
    }

    private void createPreviewSession() {
        if (cameraDevice == null || imageReader == null || !textureView.isAvailable()) return;
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(photoSize.getWidth(), photoSize.getHeight());
            Surface preview = new Surface(texture);
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(preview);
            applyPreset(previewBuilder);
            List<Surface> outputs = Arrays.asList(preview, imageReader.getSurface());
            cameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    try {
                        session.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
                        showReady();
                    } catch (CameraAccessException error) {
                        showError("Falha ao aplicar o preset: " + error.getMessage());
                    }
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {
                    showError("Esta combinação de foto não foi aceita pelo aparelho.");
                }
            }, cameraHandler);
        } catch (CameraAccessException error) {
            showError("Falha na câmera fotográfica: " + error.getMessage());
        }
    }

    private void applyPreset(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        Range<Float> zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
        if (zoomRange != null) {
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clamp(preset.zoom, zoomRange.getLower(), zoomRange.getUpper()));
        }

        if (preset.infinityFocus) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);
        } else {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        }

        if (preset.usesManualExposure()) {
            Range<Integer> isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            Range<Long> exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            if (isoRange != null && exposureRange != null) {
                actualIso = clamp(preset.iso, isoRange.getLower(), isoRange.getUpper());
                actualExposureNs = clamp(preset.exposureNs, exposureRange.getLower(), exposureRange.getUpper());
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, actualIso);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, actualExposureNs);
                builder.set(CaptureRequest.SENSOR_FRAME_DURATION, actualExposureNs);
            }
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            applyExposureCompensation(builder);
        }

        if (preset.whiteBalanceKelvin != null) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, kelvinToGains(preset.whiteBalanceKelvin));
        } else {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }

    private void applyExposureCompensation(CaptureRequest.Builder builder) {
        Range<Integer> range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        Rational step = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if (range == null || step == null || step.floatValue() == 0f) return;
        int value = Math.round(preset.exposureCompensation / step.floatValue());
        builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, clamp(value, range.getLower(), range.getUpper()));
    }

    private void beginCountdown(int remaining) {
        if (!active || captureInProgress || captureSession == null) return;
        captureInProgress = true;
        shutterButton.setEnabled(false);
        if (remaining <= 0) {
            capturePhoto();
            return;
        }
        runCountdownTick(remaining);
    }

    private void runCountdownTick(int remaining) {
        if (!active) {
            captureInProgress = false;
            return;
        }
        if (remaining <= 0) {
            capturePhoto();
            return;
        }
        statusText.setText("Foto em " + remaining + "…");
        shutterButton.setText(String.valueOf(remaining));
        shutterButton.setTextSize(34);
        shutterButton.animate().scaleX(.82f).scaleY(.82f).setDuration(160)
                .withEndAction(() -> shutterButton.animate().scaleX(1f).scaleY(1f).setDuration(220).start()).start();
        shutterButton.postDelayed(() -> runCountdownTick(remaining - 1), 1000);
    }

    private void capturePhoto() {
        if (cameraDevice == null || captureSession == null || imageReader == null) {
            finishCapture(false, "Câmera indisponível.");
            return;
        }
        try {
            CaptureRequest.Builder still = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            still.addTarget(imageReader.getSurface());
            applyPreset(still);
            still.set(CaptureRequest.JPEG_ORIENTATION, photoOrientation());
            statusText.setText("Capturando… mantenha o aparelho firme");
            animateShutter();
            captureSession.capture(still.build(), new CameraCaptureSession.CaptureCallback() {
                @Override public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    try {
                        if (previewBuilder != null) session.setRepeatingRequest(previewBuilder.build(), null, cameraHandler);
                    } catch (CameraAccessException ignored) {}
                }
            }, cameraHandler);
        } catch (CameraAccessException error) {
            finishCapture(false, "Falha ao fotografar: " + error.getMessage());
        }
    }

    private void saveImage(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image == null) return;
        Uri uri = null;
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "S25Preset_" + preset.id + "_" + stamp + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/S25 Preset Cam");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Não foi possível criar o arquivo");
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new IOException("Arquivo indisponível");
                output.write(bytes);
            }
            ContentValues complete = new ContentValues();
            complete.put(MediaStore.Images.Media.IS_PENDING, 0);
            getContentResolver().update(uri, complete, null, null);
            finishCapture(true, "Foto salva • " + actualDescription());
        } catch (Exception error) {
            if (uri != null) getContentResolver().delete(uri, null, null);
            finishCapture(false, "Não foi possível salvar a foto.");
        } finally {
            image.close();
        }
    }

    private void finishCapture(boolean success, String message) {
        runOnUiThread(() -> {
            captureInProgress = false;
            shutterButton.setEnabled(true);
            shutterButton.setText("●");
            shutterButton.setTextSize(61);
            statusText.setText(message);
            if (success) Toast.makeText(this, "Foto salva na Galeria.", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void animateShutter() {
        runOnUiThread(() -> {
            flashView.setAlpha(.78f);
            flashView.animate().alpha(0f).setDuration(260).start();
            shutterButton.animate().scaleX(.82f).scaleY(.82f).setDuration(90)
                    .withEndAction(() -> shutterButton.animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
        });
    }

    private int photoOrientation() {
        Integer sensor = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int deviceDegrees;
        switch (rotation) {
            case Surface.ROTATION_90: deviceDegrees = 90; break;
            case Surface.ROTATION_180: deviceDegrees = 180; break;
            case Surface.ROTATION_270: deviceDegrees = 270; break;
            default: deviceDegrees = 0;
        }
        return preset.frontCamera
                ? ((sensor == null ? 0 : sensor) + deviceDegrees) % 360
                : ((sensor == null ? 0 : sensor) - deviceDegrees + 360) % 360;
    }

    private void configureTransform() {
        if (textureView == null || photoSize == null || textureView.getWidth() == 0) return;
        float viewWidth = textureView.getWidth();
        float viewHeight = textureView.getHeight();
        float bufferWidth = photoSize.getHeight();
        float bufferHeight = photoSize.getWidth();
        float scale = Math.max(viewWidth / bufferWidth, viewHeight / bufferHeight);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale, viewWidth / 2f, viewHeight / 2f);
        textureView.setTransform(matrix);
    }

    private void showReady() {
        runOnUiThread(() -> {
            statusText.setAlpha(0f);
            statusText.setText("Pronto • " + actualDescription());
            statusText.animate().alpha(1f).setDuration(300).start();
            shutterButton.setEnabled(true);
            shutterButton.setScaleX(.72f);
            shutterButton.setScaleY(.72f);
            shutterButton.setAlpha(0f);
            shutterButton.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(450)
                    .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        });
    }

    private String actualDescription() {
        int mp = Math.round((photoSize.getWidth() * (float) photoSize.getHeight()) / 1_000_000f);
        String adjusted = Math.abs(mp - preset.megapixels) > 3 ? " • resolução compatível" : "";
        return mp + " MP • " + preset.lensLabel() + adjusted;
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        opening = false;
        captureInProgress = false;
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            shutterButton.setEnabled(false);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private RggbChannelVector kelvinToGains(int kelvin) {
        double temp = kelvin / 100.0;
        double red;
        double green;
        double blue;
        if (temp <= 66) {
            red = 255;
            green = 99.4708025861 * Math.log(temp) - 161.1195681661;
            blue = temp <= 19 ? 0 : 138.5177312231 * Math.log(temp - 10) - 305.0447927307;
        } else {
            red = 329.698727446 * Math.pow(temp - 60, -0.1332047592);
            green = 288.1221695283 * Math.pow(temp - 60, -0.0755148492);
            blue = 255;
        }
        red = clamp(red, 1, 255);
        green = clamp(green, 1, 255);
        blue = clamp(blue, 1, 255);
        float g = (float) (green / 255.0);
        return new RggbChannelVector((float) (red / 255.0 / g), 1f, 1f, (float) (blue / 255.0 / g));
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return text;
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        bg.setStroke(dp(2), stroke);
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static long clamp(long value, long min, long max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
