package com.uowee.android.screen.test;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.uowee.android.screen.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestActivity extends AppCompatActivity implements Encoder.IEncoderListener, SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera camera;
    private boolean isPreview = false;
    private SurfaceView surfaceView;

    private static final String VLC_HOST = "192.168.8.210";
    private static final int VLC_PORT = 7878;

    private InetAddress address;
    private DatagramSocket socket;
    private ExecutorService executor;
    private Encoder encoder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        surfaceView = findViewById(R.id.camera_surfaceview);
        surfaceView.getHolder().addCallback(this);

        try {
            address = InetAddress.getByName(VLC_HOST);
            socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, android.hardware.Camera camera) {
        encoder.encoderYUV420(bytes);
        camera.addCallbackBuffer(bytes);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (isPreview) {
            stopPreview();
        }
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopPreview();
    }

    @Override
    public void onH264(final byte[] data) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, VLC_PORT);
                    socket.send(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startPreview() {
        if (camera == null) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);

            Camera.Size previewSize = parameters.getPreviewSize();
            int size = previewSize.width * previewSize.height;
            size = size * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;

            if (encoder == null) {
                encoder = new Encoder(previewSize.width, previewSize.height,
                        2000 * 1000, 15, this);
            }

            camera.addCallbackBuffer(new byte[size]);
            camera.setPreviewDisplay(surfaceView.getHolder());
            camera.setPreviewCallbackWithBuffer(this);
            camera.setParameters(parameters);
            camera.startPreview();
            isPreview = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (camera != null) {
            if (isPreview) {
                isPreview = false;
                camera.setPreviewCallbackWithBuffer(null);
                camera.stopPreview();
            }
            camera.release();
            camera = null;
        }
    }
}
