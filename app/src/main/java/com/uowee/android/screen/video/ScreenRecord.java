package com.uowee.android.screen.video;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.view.Surface;

import com.uowee.android.screen.video.media.VideoMediaCodec;

/**
 * Created by GuoWee on 2018/6/27.
 */

public class ScreenRecord extends Thread {
    private final static String TAG = "ScreenRecord";
    private Context mContext;
    private MediaProjection mMediaProjection;
    private VideoMediaCodec mVideoMediaCodec;
    private Surface mSurface;
    private VirtualDisplay mVirtualDisplay;

    public ScreenRecord(Context context, MediaProjection mediaProjection) {
        this.mContext = context;
        this.mMediaProjection = mediaProjection;
        mVideoMediaCodec = new VideoMediaCodec();
    }

    @Override
    public void run() {
        mVideoMediaCodec.prepare();
        mSurface = mVideoMediaCodec.getSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT, Constant.VIDEO_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface, null, null);
        mVideoMediaCodec.isRun(true);
        mVideoMediaCodec.record();
    }

    public void release() {
        mVideoMediaCodec.release();
    }
}
