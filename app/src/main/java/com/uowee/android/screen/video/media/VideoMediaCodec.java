package com.uowee.android.screen.video.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.uowee.android.screen.video.Constant;
import com.uowee.android.screen.video.RtspActicity;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by GuoWee on 2018/6/27.
 */

public class VideoMediaCodec extends MediaCodecBase {

    private static final String TAG = "VideoMediaCodec";
    private Surface mSurface;
    public byte[] configbyte; // pps和sps


    public VideoMediaCodec() {

    }

    public Surface getSurface() {
        return mSurface;
    }

    public void isRun(boolean isRun) {
        this.isRun = isRun;
    }


    @Override
    public void prepare() {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(Constant.MIME_TYPE, Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constant.VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constant.VIDEO_FRAMERATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constant.VIDEO_IFRAME_INTER);
            mEncoder = MediaCodec.createEncoderByType(Constant.MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mEncoder.createInputSurface();
            mEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void release() {
        this.isRun = false;
    }

    @Override
    public void record() {
        try {
            while (isRun) {
                if (mEncoder == null) {
                    break;
                }
                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                int index = mEncoder.dequeueOutputBuffer(mBufferInfo, Constant.TIMEOUT_USEC);
                Log.i(TAG, "dequeue output buffer index=" + index);

                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once
                    Log.e("TAG", "======MediaCodec.INFO_OUTPUT_FORMAT_CHANGED======");
                } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    Log.e("TAG", "======MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED======");
                } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    //   Log.e("TAG", "======MediaCodec.INFO_TRY_AGAIN_LATER======");
                    try {
                        // wait 10ms
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                } else if (index >= 0) {
                    //获取每一个NALU,即每一帧
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffers()[index];
                    byte[] outData = new byte[mBufferInfo.size];
                    outputBuffer.get(outData);

                    Log.d("TAG", "got buffer, info: size=" + mBufferInfo.size
                            + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                            + ", offset=" + mBufferInfo.offset + ", flags=" + mBufferInfo.flags);
                    if (outputBuffer != null) {
                        int type = outputBuffer.get(4) & 0x1F;
                        Log.e("TAG", "Buffer.get(4): " + outputBuffer.get(4));
                        if (type == 7 || type == 8) {
                            //提取SPS 和 PPS
                            Log.e("TAG", "--------SPS & PPS FLAGS = " + mBufferInfo.flags);
                            configbyte = new byte[mBufferInfo.size];
                            configbyte = outData;
                        } else if (type == 5) {
                            // 关键帧，并在关键帧前面添加SPS 和 PPS
                            Log.e("TAG", "--------IDR FLAGS = " + mBufferInfo.flags);
                            byte[] keyframe = new byte[mBufferInfo.size + configbyte.length];
                            System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                            System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);

                            RtspActicity.putData(keyframe, 1, mBufferInfo.presentationTimeUs * 1000L);
                        } else {
                            Log.e("TAG", "--------FLAGS = " + mBufferInfo.flags);
                            RtspActicity.putData(outData, 2, mBufferInfo.presentationTimeUs * 1000L);
                        }

                    }
                    mEncoder.releaseOutputBuffer(index, false);
                }
            }
        } catch (Exception e) {

        } finally {
            if (mEncoder != null) {
                mEncoder.stop();
                mEncoder.release();
                mEncoder = null;
            }
        }
    }


}
