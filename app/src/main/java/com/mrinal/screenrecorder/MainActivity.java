package com.mrinal.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button button;
    private boolean RECORDING;
    private MediaRecorder mediaRecorder;
    private MediaProjection mediaProjection;
    private MediaProjectionCallback callback;
    private MediaProjectionManager projectionManager;
    private VirtualDisplay virtualDisplay;
    private int DISPLAY_WIDTH;
    private int DISPLAY_HEIGHT;
    private int SCREEN_DENSITY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void startRecorder(Intent data) {
        callback = new MediaProjectionCallback();
        mediaProjection = projectionManager.getMediaProjection(RESULT_OK, data);
        mediaProjection.registerCallback(callback, null);
        initDimens();
        initRecorder();
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            virtualDisplay = getVirtualDisplay();
            button.setText(R.string.stop_recording);
            RECORDING = true;
        } catch (Exception e) {
            Toast.makeText(this, "Unable to start recorder:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initDimens() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindow().getDecorView().getDisplay().getRealMetrics(metrics);
        DISPLAY_WIDTH = metrics.widthPixels;
        DISPLAY_HEIGHT = metrics.heightPixels;
        SCREEN_DENSITY = metrics.densityDpi;
    }

    public void initRecorder() {
        String file = getFileName();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mediaRecorder.setVideoEncodingBitRate(1000000);
        mediaRecorder.setOutputFile(file);
    }

    private String getFileName() {
        String fileName = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss", Locale.ENGLISH)
                .format(System.currentTimeMillis()) + ".mp4";
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ScreenRecordings/");
        if (dir.exists() || dir.mkdirs())
            return new File(dir, fileName).getAbsolutePath();
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startRecorder(data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onClickedRecorder(View view) {
        if (RECORDING)
            mediaProjection.stop();
        else
            startActivityForResult(projectionManager.createScreenCaptureIntent(), 100);
    }

    private VirtualDisplay getVirtualDisplay() {
        return mediaProjection.createVirtualDisplay(this.getClass().getSimpleName(),
                DISPLAY_WIDTH, DISPLAY_HEIGHT, SCREEN_DENSITY,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }

    public class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            button.setText(R.string.start_recording);
            RECORDING = false;
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            virtualDisplay.release();
            virtualDisplay = null;
            mediaProjection.unregisterCallback(callback);
            mediaProjection = null;
        }
    }
}