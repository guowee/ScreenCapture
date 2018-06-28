package com.uowee.android.screen.video;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.uowee.android.screen.R;
import com.uowee.android.screen.video.media.H264Data;
import com.uowee.android.screen.video.rtsp.RtspServer;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by GuoWee on 2018/6/27.
 */

public class RtspActicity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 1;
    private Button start_record, stop_record;
    private TextView line2;
    private MediaProjectionManager mMediaProjectionManager;

    private ScreenRecord mScreenRecord;

    private boolean isRecording = false;
    private String rtspAddress;
    private RtspServer mRtspServer;

    private static int queuesize = 30;
    public static ArrayBlockingQueue<H264Data> h264Queue = new ArrayBlockingQueue<>(queuesize);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        InitView();
        InitMediaProjectionManager();
        rtspAddress = displayIpAddress();
        if (rtspAddress != null) {
            line2.setText(rtspAddress);
        }
    }

    private ServiceConnection mRtspServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mRtspServer = ((RtspServer.LocalBinder) iBinder).getService();
            mRtspServer.addCallbackListener(mRtspCallbackListener);
            mRtspServer.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {
        @Override
        public void onError(RtspServer server, Exception e, int error) {
            if (error == RtspServer.ERROR_BIND_FAILED) {
                new AlertDialog.Builder(RtspActicity.this)
                        .setTitle("Port already in use !")
                        .setMessage("You need to choose another port for the RTSP server !")
                        .show();
            }
        }

        @Override
        public void onMessage(RtspServer server, int message) {
            Log.e("TAG", "-------------------message---" + message);
            if (message == RtspServer.MESSAGE_STREAMING_STARTED) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(RtspActicity.this, "RTSP STREAM STARTED", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (message == RtspServer.MESSAGE_STREAMING_STOPPED) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(RtspActicity.this, "RTSP STREAM STOPPED", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };


    private void InitView() {
        start_record = findViewById(R.id.start_record);
        start_record.setOnClickListener(this);
        stop_record = findViewById(R.id.stop_record);
        stop_record.setOnClickListener(this);
        line2 = findViewById(R.id.line2);
    }

    private void InitMediaProjectionManager() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_record:
                StartScreenCapture();
                break;
            case R.id.stop_record:
                StopScreenCapture();
                break;
        }
    }


    private void StartScreenCapture() {
        if (rtspAddress != null && !rtspAddress.isEmpty()) {
            isRecording = true;
            Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE);
            bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            Toast.makeText(this, "Network Error !", Toast.LENGTH_SHORT).show();
        }

    }

    private void StopScreenCapture() {
        isRecording = false;
        mScreenRecord.release();
        if (mRtspServer != null) {
            mRtspServer.removeCallbackListener(mRtspCallbackListener);
        }
        unbindService(mRtspServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {
                    Toast.makeText(this, "media projection is null", Toast.LENGTH_SHORT).show();
                    return;
                }
                mScreenRecord = new ScreenRecord(this, mediaProjection);
                mScreenRecord.start();
            }
        }


    }


    private String displayIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ipaddress = "";
        if (info != null && info.getNetworkId() > -1) {
            int i = info.getIpAddress();
            String ip = String.format(Locale.ENGLISH, "%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff, i >> 16 & 0xff, i >> 24 & 0xff);
            ipaddress += "rtsp://";
            ipaddress += ip;
            ipaddress += ":";
            ipaddress += RtspServer.DEFAULT_RTSP_PORT;
        }
        return ipaddress;
    }

    public static void putData(byte[] buffer, int type, long ts) {
        if (h264Queue.size() >= queuesize) {
            h264Queue.poll();
        }
        H264Data data = new H264Data();
        data.data = buffer;
        data.type = type;
        data.ts = ts;
        h264Queue.add(data);
    }
}
