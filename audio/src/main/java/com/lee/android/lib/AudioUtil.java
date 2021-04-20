package com.lee.android.lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * 音频pcm相关操作工具
 *
 * @author lee
 * @date 2021/4/20
 */
public class AudioUtil {

    /**
     * 多声道数据 取左声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1、双声道为2、5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channelSingleLeft(byte[] chunk, int channels) {
        byte[] singleChunk = new byte[chunk.length / channels];

        for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
            singleChunk[count] = chunk[i];
            singleChunk[count + 1] = chunk[i + 1];
        }
        return singleChunk;
    }

    /**
     * 多声道数据 取右声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1、双声道为2、5.1声道为6等等
     * @return 右声道数据
     */
    public static byte[] channelSingleRight(byte[] chunk, int channels) {
        byte[] singleChunk = new byte[chunk.length / channels];

        for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
            singleChunk[count] = chunk[i + 2];
            singleChunk[count + 1] = chunk[i + 3];
        }
        return singleChunk;
    }

    /**
     * 多声道数据取 双声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1、双声道为2、5.1声道为6等等
     * @return 双声道数据
     */
    public static byte[] channel2(byte[] chunk, int channels) {
        byte[] doubleChunk = new byte[(chunk.length / channels) * 2];
        for (int i = 0, count = 0; i < chunk.length; count += 4, i += channels * 2) {
            doubleChunk[count] = chunk[i];
            doubleChunk[count + 1] = chunk[i + 1];
            doubleChunk[count + 2] = chunk[i + 2];
            doubleChunk[count + 3] = chunk[i + 3];
        }
        return doubleChunk;
    }

    /**
     * 采样率转换
     *
     * @param src          需要转换的pcm数据
     * @param inputSample  转换前的采样率
     * @param outputSample 转换后的采样率
     * @return 转换后的pcm数据
     */
    public static byte[] audioSampleConvert(byte[] src, long inputSample, long outputSample) {
        if (inputSample == outputSample)
            return src;

        ShortBuffer buffer = ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] input = new short[buffer.capacity()];
        buffer.get(input);
        // 输入音频长度
        int len = input.length;

        // 输出音频长度
        int outLen = (int) (1.0 * len * outputSample / inputSample);

        double[] S = new double[len];
        double[] T = new double[outLen];
        short[] output = new short[outLen];

        // 输入信号归一化
        for (int i = 0; i < len; i++) {
            S[i] = input[i] / 32768.0;
        }

        // 计算输入输出个数比
        double F = ((double) len - 1) / ((double) outLen - 1);

        double Fn;
        int Ceil;
        int Floor;
        for (int n = 0; n < outLen; n++) {

            // 计算输出对应输入的相邻下标
            Fn = F * n;
            Ceil = (int) Math.ceil(Fn);
            Floor = (int) Math.floor(Fn);

            // 防止下标溢出
            if (Ceil >= len && Floor < len) {
                Ceil = Floor;
            } else if (Ceil >= len) {
                Ceil = len - 1;
                Floor = len - 1;
            }

            // 相似三角形法计算输出点近似值
            T[n] = S[Floor] + (Fn - Floor) * (S[Ceil] - S[Floor]);
        }

        // 输出信号恢复
        for (int i = 0; i < outLen; i++) {
            output[i] = (short) (T[i] * 32768.0);
        }

        return toByteArray(output);
    }


    /**
     * 计算音量分贝增减后的变化值
     *
     * @param db 增加或降低的分贝
     * @return 该结果作为amplifyPCMData的入参
     */
    public static float audioDB(int db) {
        return (float) Math.pow(10, (double) db / 20);
    }

    /**
     * 音频增高或降低多少分贝
     *
     * @param pcmData  需要转换的pcm数据
     * @param size     pcm数据大小
     * @param newData  转换后的数据
     * @param sample   pcm的采样率
     * @param multiple 音量大小变化值,该值来源于audioDB函数的计算结果,避免多次计算
     * @return 转换后的数据长度
     */
    public static int audioAmplify(byte[] pcmData, int size, byte[] newData, int sample, float multiple) {
        short shortMax = (short) 0x7F00;
        short shortMin = (short) -0x7F00;
        int nCur = 0;
        if (16 == sample) {
            while (nCur < size) {
                short volum = getShort(pcmData, nCur);
                volum = (short) (volum * multiple);

                //防止破音
                if (volum < shortMin) {
                    volum = shortMin;
                } else if (volum > shortMax) {
                    volum = shortMax;
                }

                newData[nCur] = (byte) (volum & 0xFF);
                newData[nCur + 1] = (byte) ((volum >> 8) & 0xFF);
                nCur += 2;
            }
        }
        return 0;
    }

    private static short getShort(byte[] data, int start) {
        return (short) ((data[start] & 0xFF) | (data[start + 1] << 8));
    }

    public static byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i]);
            dest[i * 2 + 1] = (byte) ((src[i] >> 8));
        }
        return dest;
    }
}
