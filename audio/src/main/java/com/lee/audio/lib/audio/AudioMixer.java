package com.lee.audio.lib.audio;

import android.util.Log;

/**
 * 音轨混合器
 *
 * @author lee
 * @date 2021/5/17
 */
public abstract class AudioMixer {
    public static final String TAG = "AudioMixer";

    /**
     * 默认创建一个极值合成器
     */
    public static AudioMixer createDefaultAudioMixer() {
        return createExtremeMixer();
    }

    /**
     * 极值合成器,将音轨数据完全重合混在一起
     */
    public static AudioMixer createExtremeMixer() {
        return new ExtremeMixer();
    }

    /**
     * 叠加合成器,需要考虑爆音的问题,各个音轨数据直接叠加
     */
    public static AudioMixer createAddAudioMixer() {
        return new AddAudioMixer();
    }

    /**
     * 完全平均值合成器,各个音轨的数据将叠加后取平均值
     */
    public static AudioMixer createAverageAudioMixer() {
        return new AverageAudioMixer();
    }

    /**
     * 权值合成器
     *
     * @param weights {0.1,0.2,0.7}  权值数组长度为混合音轨的数量
     */
    public static AudioMixer createWeightAudioMixer(float[] weights) {
        return new WeightAudioMixer(weights);
    }


    /**
     * 混合音频数据
     *
     * @param trackSize 混合的轨道数量
     * @param data      每轨音频数据,pcm位数需要为16位
     * @return 混合后的音频数据
     */
    public byte[] mixAudio(int trackSize, byte[]... data) {
        //数据为空
        if (data == null || data.length == 0) {
            return null;
        }

        //数据长度与设定音轨数量不符
        if (trackSize != data.length) {
            Log.e(TAG, "trackSize != data.length");
            return null;
        }

        int audioSize = data[0].length;

        //数据中每轨数据的长度必须一致
        for (int index = 1; index < data.length; index++) {
            if (data[index].length != audioSize) {
                Log.e(TAG, "column of the road of audio + " + index + " is different:" + data[index].length + "," + audioSize);
                return null;
            }
        }
        //将byte[]转变为 short[]
        short[][] sMulRoadAudio = new short[trackSize][audioSize / 2];
        //PCM音频16位的存储是大端存储方式，即低位在前，高位在后，例如(X1Y1, X2Y2, X3Y3)数据，它代表的采样点数值就是(（Y1 * 256 + X1）, （Y2 * 256 + X2）, （Y3 * 256 + X3）)
        for (int r = 0; r < trackSize; ++r) {
            for (int c = 0; c < audioSize / 2; ++c) {
                sMulRoadAudio[r][c] = (short) ((data[r][c * 2] & 0xff) | (data[r][c * 2 + 1] & 0xff) << 8);
            }
        }
        //参与运算的每轨数据
        short[] sMixAudio = new short[audioSize / 2];
        //最终转换完成的数据
        byte[] realMixAudio = new byte[audioSize];
        mix(sMulRoadAudio, sMixAudio, realMixAudio, trackSize, audioSize / 2);
        return realMixAudio;
    }

    /**
     * @param row    参与合成的音频数量
     * @param column 一段音频的采样点数，这里所有参与合成的音频的采样点数都是相同的
     */
    protected abstract void mix(short[][] sMulRoadAudio, short[] sMixAudio, byte[] realMixAudio, int row, int column);


    /**
     * 叠加合成器,多轨音频数据相加,存在数据溢出的风险
     */
    private static class AddAudioMixer extends AudioMixer {

        @Override
        protected void mix(short[][] sMulRoadAudio, short[] sMixAudio, byte[] realMixAudio, int row, int column) {
            int mixVal;
            int sr;
            for (int sc = 0; sc < column; ++sc) {
                mixVal = 0;
                sr = 0;
                //这里采取累加法
                for (; sr < row; ++sr) {
                    mixVal += sMulRoadAudio[sr][sc];
                }
                //最终值不能大于short最大值，因此可能出现溢出
                sMixAudio[sc] = (short) (mixVal);
            }
            //short值转为大端存储的双字节序列
            for (sr = 0; sr < column; ++sr) {
                realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
                realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
            }
        }
    }


    /**
     * 极值混合器
     */
    private static class ExtremeMixer extends AudioMixer {

        @Override
        protected void mix(short[][] sMulRoadAudio, short[] sMixAudio, byte[] realMixAudio, int row, int column) {
            int maxVal;
            int minVal;
            int sr;
            for (int sc = 0; sc < column; ++sc) {
                sr = 0;
                maxVal = sMulRoadAudio[sr][sc];
                minVal = sMulRoadAudio[sr][sc];
                for (sr = 1; sr < row; ++sr) {
                    maxVal = Math.max(sMulRoadAudio[sr][sc], maxVal);
                    minVal = Math.min(sMulRoadAudio[sr][sc], minVal);
                }
                if (maxVal != minVal) {
                    //极值不等的情况下,叠加
                    sMixAudio[sc] = (short) (maxVal + minVal);
                } else {
                    sMixAudio[sc] = (short) maxVal;
                }
            }
            for (sr = 0; sr < column; ++sr) {
                realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
                realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
            }
        }
    }

    /**
     * 平均值合成器,多轨音频数据相加然后平均
     */
    private static class AverageAudioMixer extends AudioMixer {

        @Override
        protected void mix(short[][] sMulRoadAudio, short[] sMixAudio, byte[] realMixAudio, int row, int column) {
            int mixVal;
            int sr;
            for (int sc = 0; sc < column; ++sc) {
                mixVal = 0;
                sr = 0;
                for (; sr < row; ++sr) {
                    mixVal += sMulRoadAudio[sr][sc];
                }
                sMixAudio[sc] = (short) (mixVal / row);
            }

            for (sr = 0; sr < column; ++sr) {
                realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
                realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
            }
        }
    }

    /**
     * 权重分配合成器,各音轨按指定权重进行分配
     */
    private static class WeightAudioMixer extends AudioMixer {
        private final float[] weights;

        /**
         * 权重分配
         *
         * @param weights {0.1,0.2,0.7}
         */
        public WeightAudioMixer(float[] weights) {
            this.weights = weights;
        }

        @Override
        protected void mix(short[][] sMulRoadAudio, short[] sMixAudio, byte[] realMixAudio, int row, int column) {
            int mixVal;
            int sr;
            for (int sc = 0; sc < column; ++sc) {
                mixVal = 0;
                sr = 0;
                for (; sr < row; ++sr) {
                    mixVal += sMulRoadAudio[sr][sc] * weights[sr];
                }
                sMixAudio[sc] = (short) (mixVal);
            }
            for (sr = 0; sr < column; ++sr) {
                realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
                realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
            }
        }
    }
}
