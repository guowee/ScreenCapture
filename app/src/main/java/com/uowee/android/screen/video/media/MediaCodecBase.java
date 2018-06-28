package com.uowee.android.screen.video.media;

import android.media.MediaCodec;

/**
 * Created by GuoWee on 2018/6/27.
 */

public abstract class MediaCodecBase {

    protected MediaCodec mEncoder;
    protected boolean isRun = false;

    public abstract void prepare();

    public abstract void record();

    public abstract void release();
}
