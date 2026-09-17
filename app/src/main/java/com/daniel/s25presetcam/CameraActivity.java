package com.daniel.s25presetcam;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Range;
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
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends Activity {
    public static final String EXTRA_PRESET_ID = "preset_id";

    private TextureView textureView;
    private TextView statusText;
    private TextView recordButton;
    private Preset preset;
    private CameraManager cameraManager;
    private String cameraId;
    private CameraCharacteristics characteristics;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder requestBuilder;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private MediaRecorder mediaRecorder;
    private ParcelFileDescriptor outputDescriptor;
    private Uri outputUri;
    private Size videoSize;
    private int actualFps;
    private boolean highSpeed;
    private Range<Integer> highSpeedFpsRange;
    private boolean recording;
    private boolean opening;
    private ObjectAnimator recordPulse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String id = getIntent().getStringExtra(EXTRA_PRESET_ID);
        preset = PresetRepository.find(id == null ? "daily" : id);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        setContentView(buildUi());
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(surfaceListener);
        root.addView(textureView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(18), dp(18), dp(18), dp(15));
        GradientDrawable topBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(225, 5, 8, 16), Color.argb(120, 5, 8, 16), Color.TRANSPARENT}
        );
        top.setBackground(topBg);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Color.WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        titleRow.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        titleColumn.addView(text(preset.name, 21, Color.WHITE, true));
        titleColumn.addView(text(preset.objective, 12, Color.rgb(190, 199, 220), false));
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

        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(-1, dp(135), Gravity.TOP);
        root.addView(top, topParams);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        bottom.setPadding(dp(20), dp(12), dp(20), dp(28));
        GradientDrawable bottomBg = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.argb(240, 0, 0, 0), Color.argb(150, 0, 0, 0), Color.TRANSPARENT}
        );
        bottom.setBackground(bottomBg);

        statusText = text("Preparando câmera…", 12, Color.WHITE, false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(12), dp(8), dp(12), dp(8));
        bottom.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        recordButton = text("●", 58, Color.rgb(255, 75, 91), false);
        recordButton.setGravity(Gravity.CENTER);
        recordButton.setBackground(rounded(Color.argb(120, 255, 255, 255), Color.WHITE, dp(44)));
        recordButton.setOnClickListener(v -> toggleRecording());
        LinearLayout.LayoutParams recordParams = new LinearLayout.LayoutParams(dp(88), dp(88));
        recordParams.topMargin = dp(10);
        bottom.addView(recordButton, recordParams);

        TextView hint = text("Toque para gravar", 11, Color.rgb(190, 199, 220), false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(4);
        bottom.addView(hint, hintParams);

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(-1, dp(225), Gravity.BOTTOM);
        root.addView(bottom, bottomParams);
        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraThread();
        if (textureView.isAvailable()) openCamera();
    }

    @Override
    protected void onPause() {
        if (recording) stopRecording(false);
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
        cameraThread = new HandlerThread("PresetCamera");
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
            cameraId = chooseCameraId(preset.frontCamera);
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
            resolveVideoConfiguration();
            prepareRecorder();
            cameraManager.openCamera(cameraId, stateCallback, cameraHandler);
        } catch (Exception error) {
            opening = false;
            showError("Não foi possível preparar a câmera: " + error.getMessage());
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override public void onOpened(CameraDevice camera) {
            opening = false;
            cameraDevice = camera;
            createCaptureSession();
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

    private void resolveVideoConfiguration() {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) throw new IllegalStateException("Configurações da câmera indisponíveis");
        Size requested = new Size(preset.width, preset.height);
        videoSize = closestSize(map.getOutputSizes(MediaRecorder.class), requested);
        actualFps = preset.fps;
        highSpeed = false;
        highSpeedFpsRange = null;

        if (preset.fps >= 120) {
            List<Size> highSizes = Arrays.asList(map.getHighSpeedVideoSizes());
            Size high = closestSize(highSizes.toArray(new Size[0]), requested);
            highSpeedFpsRange = chooseHighSpeedFpsRange(map, high, preset.fps);
            if (high != null && highSpeedFpsRange != null) {
                videoSize = high;
                highSpeed = true;
            } else {
                actualFps = 60;
            }
        } else if (!supportsFps(actualFps)) {
            actualFps = preset.fps > 30 ? 30 : preset.fps;
        }
        runOnUiThread(this::configureTransform);
    }

    private Range<Integer> chooseHighSpeedFpsRange(StreamConfigurationMap map, Size size, int fps) {
        if (size == null) return null;
        try {
            Range<Integer> best = null;
            for (Range<Integer> range : map.getHighSpeedVideoFpsRangesFor(size)) {
                if (range.contains(fps) && (best == null || range.getLower() > best.getLower())) best = range;
            }
            return best;
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    private boolean supportsFps(int fps) {
        Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (ranges == null) return fps <= 30;
        for (Range<Integer> range : ranges) if (range.getUpper() >= fps) return true;
        return fps <= 30;
    }

    private Size closestSize(Size[] choices, Size requested) {
        if (choices == null || choices.length == 0) return new Size(1920, 1080);
        Size best = choices[0];
        long targetArea = (long) requested.getWidth() * requested.getHeight();
        long bestScore = Long.MAX_VALUE;
        for (Size size : choices) {
            double ratio = size.getWidth() / (double) size.getHeight();
            double targetRatio = requested.getWidth() / (double) requested.getHeight();
            long area = (long) size.getWidth() * size.getHeight();
            long score = Math.abs(area - targetArea) + (long) (Math.abs(ratio - targetRatio) * targetArea * 3);
            if (area > targetArea * 1.25) score += targetArea;
            if (score < bestScore) {
                bestScore = score;
                best = size;
            }
        }
        return best;
    }

    private void prepareRecorder() throws IOException {
        releaseRecorder();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "S25Preset_" + preset.id + "_" + stamp + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/S25 Preset Cam");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        outputUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (outputUri == null) throw new IOException("Não foi possível criar o arquivo de vídeo");
        outputDescriptor = getContentResolver().openFileDescriptor(outputUri, "w");
        if (outputDescriptor == null) throw new IOException("Arquivo de vídeo indisponível");

        mediaRecorder = new MediaRecorder(this);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(outputDescriptor.getFileDescriptor());
        mediaRecorder.setVideoEncodingBitRate(videoBitrate());
        mediaRecorder.setVideoFrameRate(actualFps);
        if (highSpeed) mediaRecorder.setCaptureRate(actualFps);
        mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(192_000);
        mediaRecorder.setAudioSamplingRate(48_000);
        mediaRecorder.setOrientationHint(videoOrientation());
        mediaRecorder.prepare();
    }

    private int videoBitrate() {
        long pixels = (long) videoSize.getWidth() * videoSize.getHeight();
        if (pixels >= 8_000_000L) return actualFps >= 60 ? 80_000_000 : 55_000_000;
        return actualFps >= 120 ? 45_000_000 : actualFps >= 60 ? 28_000_000 : 18_000_000;
    }

    private int videoOrientation() {
        Integer sensor = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int deviceDegrees;
        switch (rotation) {
            case Surface.ROTATION_90: deviceDegrees = 90; break;
            case Surface.ROTATION_180: deviceDegrees = 180; break;
            case Surface.ROTATION_270: deviceDegrees = 270; break;
            default: deviceDegrees = 0;
        }
        boolean front = preset.frontCamera;
        return front ? ((sensor == null ? 0 : sensor) + deviceDegrees) % 360
                : ((sensor == null ? 0 : sensor) - deviceDegrees + 360) % 360;
    }

    private void createCaptureSession() {
        if (cameraDevice == null || mediaRecorder == null || !textureView.isAvailable()) return;
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(videoSize.getWidth(), videoSize.getHeight());
            Surface preview = new Surface(texture);
            Surface recorder = mediaRecorder.getSurface();
            List<Surface> outputs = Arrays.asList(preview, recorder);
            requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            requestBuilder.addTarget(preview);
            requestBuilder.addTarget(recorder);
            applyPreset(requestBuilder);

            CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    try {
                        if (highSpeed && session instanceof CameraConstrainedHighSpeedCaptureSession) {
                            List<CaptureRequest> burst = ((CameraConstrainedHighSpeedCaptureSession) session)
                                    .createHighSpeedRequestList(requestBuilder.build());
                            session.setRepeatingBurst(burst, null, cameraHandler);
                        } else {
                            session.setRepeatingRequest(requestBuilder.build(), null, cameraHandler);
                        }
                        showReady();
                    } catch (CameraAccessException error) {
                        showError("Falha ao aplicar o preset: " + error.getMessage());
                    }
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {
                    showError("Esta combinação de câmera e vídeo não foi aceita.");
                }
            };

            if (highSpeed) cameraDevice.createConstrainedHighSpeedCaptureSession(outputs, callback, cameraHandler);
            else cameraDevice.createCaptureSession(outputs, callback, cameraHandler);
        } catch (CameraAccessException error) {
            showError("Falha na sessão de câmera: " + error.getMessage());
        }
    }

    private void applyPreset(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

        if (highSpeed && highSpeedFpsRange != null) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFpsRange);
        }

        Range<Float> zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
        if (zoomRange != null) {
            builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clamp(preset.zoom, zoomRange.getLower(), zoomRange.getUpper()));
        }

        if (preset.usesManualExposure() && !highSpeed) {
            Range<Integer> isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            Range<Long> exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            if (isoRange != null && exposureRange != null) {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, clamp(preset.iso, isoRange.getLower(), isoRange.getUpper()));
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                        clamp(preset.exposureNs, exposureRange.getLower(), exposureRange.getUpper()));
                builder.set(CaptureRequest.SENSOR_FRAME_DURATION, Math.max(preset.exposureNs, 1_000_000_000L / actualFps));
            }
        } else if (!highSpeed) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            Range<Integer> fpsRange = chooseFpsRange(actualFps);
            if (fpsRange != null) builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        }

        if (preset.whiteBalanceKelvin != null && !highSpeed) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, kelvinToGains(preset.whiteBalanceKelvin));
        } else {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }

        if (preset.stabilization) {
            int[] ois = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            if (contains(ois, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)) {
                builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
            } else {
                int[] eis = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
                if (contains(eis, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)) {
                    builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
                }
            }
        }
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

    private Range<Integer> chooseFpsRange(int fps) {
        Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (ranges == null || ranges.length == 0) return null;
        Range<Integer> best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Range<Integer> range : ranges) {
            if (!range.contains(fps)) continue;
            int score = Math.abs(range.getUpper() - fps) * 10 + Math.abs(range.getLower() - fps);
            if (score < bestScore) {
                bestScore = score;
                best = range;
            }
        }
        return best;
    }

    private void toggleRecording() {
        if (mediaRecorder == null || captureSession == null) return;
        if (recording) stopRecording(true);
        else startRecording();
    }

    private void startRecording() {
        try {
            mediaRecorder.start();
            recording = true;
            runOnUiThread(() -> {
                recordButton.setText("■");
                recordButton.setTextSize(38);
                statusText.setText("Gravando • " + actualDescription());
                startRecordPulse();
            });
        } catch (RuntimeException error) {
            discardPendingVideo();
            showError("Não foi possível iniciar a gravação.");
        }
    }

    private void stopRecording(boolean reopen) {
        recording = false;
        try {
            mediaRecorder.stop();
            finalizeVideo();
            runOnUiThread(() -> Toast.makeText(this, "Vídeo salvo na Galeria.", Toast.LENGTH_SHORT).show());
        } catch (RuntimeException error) {
            discardPendingVideo();
            runOnUiThread(() -> Toast.makeText(this, "A gravação foi curta demais e não pôde ser salva.", Toast.LENGTH_LONG).show());
        }
        runOnUiThread(() -> {
            stopRecordPulse();
            recordButton.setText("●");
            recordButton.setTextSize(58);
            statusText.setText("Preparando próxima gravação…");
        });
        if (reopen && cameraHandler != null) cameraHandler.post(this::rebuildRecorderSession);
    }

    private void rebuildRecorderSession() {
        try {
            if (captureSession != null) captureSession.close();
            captureSession = null;
            prepareRecorder();
            createCaptureSession();
        } catch (Exception error) {
            showError("Falha ao preparar a próxima gravação: " + error.getMessage());
        }
    }

    private void finalizeVideo() {
        if (outputUri != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(outputUri, values, null, null);
        }
        releaseRecorder();
        outputUri = null;
    }

    private void discardPendingVideo() {
        if (outputUri != null) getContentResolver().delete(outputUri, null, null);
        releaseRecorder();
        outputUri = null;
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.reset(); } catch (RuntimeException ignored) {}
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (outputDescriptor != null) {
            try { outputDescriptor.close(); } catch (IOException ignored) {}
            outputDescriptor = null;
        }
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
        if (outputUri != null && !recording) discardPendingVideo();
        else releaseRecorder();
        opening = false;
    }

    private void configureTransform() {
        if (textureView == null || videoSize == null || textureView.getWidth() == 0) return;
        float viewWidth = textureView.getWidth();
        float viewHeight = textureView.getHeight();
        float bufferWidth = videoSize.getHeight();
        float bufferHeight = videoSize.getWidth();
        float scale = Math.max(viewWidth / bufferWidth, viewHeight / bufferHeight);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale, viewWidth / 2f, viewHeight / 2f);
        textureView.setTransform(matrix);
    }

    private void showReady() {
        runOnUiThread(() -> {
            statusText.setAlpha(0f);
            statusText.setTranslationY(dp(8));
            statusText.setText("Pronto • " + actualDescription());
            statusText.animate().alpha(1f).translationY(0f).setDuration(320).start();
            recordButton.setScaleX(.75f);
            recordButton.setScaleY(.75f);
            recordButton.setAlpha(0f);
            recordButton.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(450)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
        });
    }

    private void startRecordPulse() {
        stopRecordPulse();
        recordPulse = ObjectAnimator.ofFloat(recordButton, View.ALPHA, 1f, .58f, 1f);
        recordPulse.setDuration(1050);
        recordPulse.setRepeatCount(ValueAnimator.INFINITE);
        recordPulse.start();
    }

    private void stopRecordPulse() {
        if (recordPulse != null) {
            recordPulse.cancel();
            recordPulse = null;
        }
        recordButton.setAlpha(1f);
    }

    private String actualDescription() {
        String resolution = videoSize.getWidth() >= 3800 ? "4K" : "FHD";
        String fallback = actualFps == preset.fps ? "" : " • ajustado pelo aparelho";
        return resolution + " " + actualFps + " fps • " + preset.lensLabel() + fallback;
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
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

    private static boolean contains(int[] values, int target) {
        if (values == null) return false;
        for (int value : values) if (value == target) return true;
        return false;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static long clamp(long value, long min, long max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
