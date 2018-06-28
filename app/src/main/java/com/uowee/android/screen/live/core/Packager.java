package com.uowee.android.screen.live.core;

import android.media.MediaFormat;

import com.uowee.android.screen.live.utils.ByteArrayTools;

import java.nio.ByteBuffer;

/**
 * Created by GuoWee on 2018/6/28.
 */

public class Packager {

    public static class H264Packager {
        public static byte[] generateAVCDecoderConfigurationRecord(MediaFormat mediaFormat) {
            ByteBuffer SPSByteBuff = mediaFormat.getByteBuffer("csd-0");
            SPSByteBuff.position(4);
            ByteBuffer PPSByteBuff = mediaFormat.getByteBuffer("csd-1");
            PPSByteBuff.position(4);

            int spsLength = SPSByteBuff.remaining();
            int ppsLength = PPSByteBuff.remaining();
            int length = 11 + spsLength + ppsLength;
            byte[] result = new byte[length];
            SPSByteBuff.get(result, 8, spsLength);
            PPSByteBuff.get(result, 8 + spsLength + 3, ppsLength);
            /**
             * UB[8]configurationVersion
             * UB[8]AVCProfileIndication
             * UB[8]profile_compatibility
             * UB[8]AVCLevelIndication
             * UB[8]lengthSizeMinusOne
             */
            result[0] = 0x01;
            result[1] = result[9];
            result[2] = result[10];
            result[3] = result[11];
            result[4] = (byte) 0xFF;
            /**
             * UB[8]numOfSequenceParameterSets
             * UB[16]sequenceParameterSetLength
             */
            result[5] = (byte) 0xE1;
            ByteArrayTools.intToByteArrayTwoByte(result, 6, spsLength);
            /**
             * UB[8]numOfPictureParameterSets
             * UB[16]pictureParameterSetLength
             */
            int pos = 8 + spsLength;
            result[pos] = (byte) 0x01;
            ByteArrayTools.intToByteArrayTwoByte(result, pos + 1, ppsLength);

            return result;
        }
    }

    public static class FLVPackager {
        public static final int FLV_TAG_LENGTH = 11;
        public static final int FLV_VIDEO_TAG_LENGTH = 5;
        public static final int FLV_AUDIO_TAG_LENGTH = 2;
        public static final int FLV_TAG_FOOTER_LENGTH = 4;
        public static final int NALU_HEADER_LENGTH = 4;

        public static void fillFlvVideoTag(byte[] dst, int pos, boolean isAVCSequenceHeader, boolean isIDR, int readDataLength) {
            //FrameType&CodecID
            dst[pos] = isIDR ? (byte) 0x17 : (byte) 0x27;
            //AVCPacketType
            dst[pos + 1] = isAVCSequenceHeader ? (byte) 0x00 : (byte) 0x01;
            // CompositionTime
            dst[pos + 2] = 0x00;
            dst[pos + 3] = 0x00;
            dst[pos + 4] = 0x00;
            if (!isAVCSequenceHeader) {
                //NALU HEADER
                ByteArrayTools.intToByteArrayFull(dst, pos + 5, readDataLength);
            }
        }
    }
}
