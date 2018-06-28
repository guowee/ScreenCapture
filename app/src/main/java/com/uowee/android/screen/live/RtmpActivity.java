package com.uowee.android.screen.live;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.uowee.android.screen.R;
import com.uowee.android.screen.live.core.RESCoreParameters;
import com.uowee.android.screen.live.rtmp.RESFlvData;
import com.uowee.android.screen.live.rtmp.RESFlvDataCollecter;
import com.uowee.android.screen.live.task.LiveRecorder;
import com.uowee.android.screen.live.task.RtmpStreamingSender;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by GuoWee on 2018/6/28.
 */

public class RtmpActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_M = 2;
    private Button mButton;
    private EditText mRtmpAddET;
    private MediaProjectionManager mMediaProjectionManager;
    private LiveRecorder mLiveRecorder;
    private String rtmpAddr;
    private RtmpStreamingSender streamingSender;
    private ExecutorService executorService;
    private RESCoreParameters coreParameters;

    static {
        System.loadLibrary("screenrecorderrtmp");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        InitView();
        InitMediaProjectionManager();
    }


    private void InitView() {
        mButton = (Button) findViewById(R.id.button);
        mRtmpAddET = (EditText) findViewById(R.id.et_rtmp_address);
        mButton.setOnClickListener(this);
    }


    private void InitMediaProjectionManager() {
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button) {
            if (mLiveRecorder != null) {
                stopScreenRecord();
            } else {
                createScreenCapture();
            }
        }

    }

    private void stopScreenRecord() {
        mLiveRecorder.quit();
        mLiveRecorder = null;
        if (streamingSender != null) {
            streamingSender.sendStop();
            streamingSender.quit();
            streamingSender = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        mButton.setText("Restart recorder");
    }

    private void createScreenCapture() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_M);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLiveRecorder != null) {
            stopScreenRecord();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("TAG", "media projection is null");
            return;
        }
        rtmpAddr = mRtmpAddET.getText().toString().trim();
        if (TextUtils.isEmpty(rtmpAddr)) {
            Toast.makeText(this, "rtmp address cannot be null", Toast.LENGTH_SHORT).show();
            return;
        }

        streamingSender = new RtmpStreamingSender();
        streamingSender.sendStart(rtmpAddr);
        RESFlvDataCollecter collecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                streamingSender.sendFood(flvData, type);
            }
        };
        coreParameters = new RESCoreParameters();

        mLiveRecorder = new LiveRecorder(collecter, RESFlvData.VIDEO_WIDTH, RESFlvData.VIDEO_HEIGHT, RESFlvData.VIDEO_BITRATE, 2, mediaProjection);
        mLiveRecorder.start();

        executorService = Executors.newCachedThreadPool();
        executorService.execute(streamingSender);

        mButton.setText("Stop Recorder");
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);

    }
}
