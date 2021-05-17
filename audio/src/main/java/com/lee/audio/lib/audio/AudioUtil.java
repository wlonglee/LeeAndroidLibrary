package com.lee.audio.lib.audio;

import android.media.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * 8位单声道    0声道     0声道     0声道     0声道...
 * 8位双声道    0声道(左) 1声道(右) 0声道(左)  1声道(右) 0声道(左) 1声道(右) 0声道(左)  1声道(右)...
 * 8位三声道    0声道(左) 1声道(右) 2声道(中)  0声道(左) 1声道(右) 2声道(中) 0声道(左)  1声道(右)...
 * 16位单声道   0声道(低) 0声道(高) 0声道(低)  0声道(高) 0声道(低) 0声道(高) 0声道(低)  0声道(高) ...
 * 16位双声道   0声道(低) 0声道(高) 1声道(低)  1声道(高) 0声道(低) 0声道(高) 1声道(低)  1声道(高) ...
 * 16位三声道   0声道(低) 0声道(高) 1声道(低)  1声道(高) 2声道(低) 2声道(高) 0声道(低)  0声道(高) ...
 * 音频pcm相关操作工具
 *
 * @author lee
 * @date 2021/5/12
 */
public class AudioUtil {

    /**
     * 8位pcm多声道数据取左声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channel1LeftB8(byte[] chunk, int channels) {
        return channelSingleLeft(chunk, channels, AudioFormat.ENCODING_PCM_8BIT);
    }

    /**
     * 16位pcm多声道数据取左声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channel1LeftB16(byte[] chunk, int channels) {
        return channelSingleLeft(chunk, channels, AudioFormat.ENCODING_PCM_16BIT);
    }

    /**
     * 8位pcm多声道数据取右声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channel1RightB8(byte[] chunk, int channels) {
        return channelSingleRight(chunk, channels, AudioFormat.ENCODING_PCM_8BIT);
    }

    /**
     * 16位pcm多声道数据取右声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channel1RightB16(byte[] chunk, int channels) {
        return channelSingleRight(chunk, channels, AudioFormat.ENCODING_PCM_16BIT);
    }

    /**
     * 8位pcm多声道数据取双声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channel2B8(byte[] chunk, int channels) {
        return channelDouble(chunk, channels, AudioFormat.ENCODING_PCM_8BIT);
    }

    /**
     * 16位pcm多声道数据取双声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @return 左声道数据
     */
    public static byte[] channel2B16(byte[] chunk, int channels) {
        return channelDouble(chunk, channels, AudioFormat.ENCODING_PCM_16BIT);
    }

    /**
     * 8位单声道数据转双声道
     *
     * @param chunk pcm数据
     * @return 双声道数据
     */
    public static byte[] channel2DoubleB8(byte[] chunk) {
        return channel1to2(chunk, AudioFormat.ENCODING_PCM_8BIT);
    }

    /**
     * 16位单声道数据转双声道
     *
     * @param chunk pcm数据
     * @return 双声道数据
     */
    public static byte[] channel2DoubleB16(byte[] chunk) {
        return channel1to2(chunk, AudioFormat.ENCODING_PCM_16BIT);
    }


    /**
     * 采样率转换
     *
     * @param chunk        需要转换的pcm数据
     * @param inputSample  转换前的采样率
     * @param outputSample 转换后的采样率
     * @return 转换后的pcm数据
     */
    public static byte[] audioSampleConvert(byte[] chunk, long inputSample, long outputSample) {
        if (inputSample == outputSample)
            return chunk;

        ShortBuffer buffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
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
     * 改变输出音量
     *
     * @param chunk   需要转换的pcm数据
     * @param newData 转换后的数据
     * @param db      增加或减少的db值
     */
    public static void audioVolumeConvert(byte[] chunk, byte[] newData, int db) {
        audioAmplify(chunk, chunk.length, newData, audioDB(db));
    }

    /**
     * 多声道数据 取左声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1,双声道为2,5.1声道为6等等
     * @param pcmBit   pcm位数,  AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @return 左声道数据
     */
    private static byte[] channelSingleLeft(byte[] chunk, int channels, int pcmBit) {
        //单声道数据直接返回
        if (channels == 1)
            return chunk;

        byte[] singleChunk = new byte[chunk.length / channels];

        if (pcmBit == AudioFormat.ENCODING_PCM_16BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
                singleChunk[count] = chunk[i];
                singleChunk[count + 1] = chunk[i + 1];
            }
        } else if (pcmBit == AudioFormat.ENCODING_PCM_8BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 1, i += channels) {
                singleChunk[count] = chunk[i];
            }
        }

        return singleChunk;
    }

    /**
     * 多声道数据 取右声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1、双声道为2、5.1声道为6等等
     * @param pcmBit   pcm位数,  AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @return 右声道数据
     */
    private static byte[] channelSingleRight(byte[] chunk, int channels, int pcmBit) {
        if (channels == 1)
            return chunk;

        byte[] singleChunk = new byte[chunk.length / channels];


        if (pcmBit == AudioFormat.ENCODING_PCM_16BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
                singleChunk[count] = chunk[i + 2];
                singleChunk[count + 1] = chunk[i + 3];
            }
        } else if (pcmBit == AudioFormat.ENCODING_PCM_8BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 1, i += channels) {
                singleChunk[count] = chunk[i + 1];
            }
        }
        return singleChunk;
    }

    /**
     * 多声道数据取 双声道
     *
     * @param chunk    pcm数据
     * @param channels 声道数量,单声道为1、双声道为2、5.1声道为6等等
     * @param pcmBit   pcm位数,  AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @return 双声道数据
     */
    private static byte[] channelDouble(byte[] chunk, int channels, int pcmBit) {
        if (channels <= 2) {
            return chunk;
        }

        byte[] doubleChunk = new byte[(chunk.length / channels) * 2];

        if (pcmBit == AudioFormat.ENCODING_PCM_16BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 4, i += channels * 2) {
                doubleChunk[count] = chunk[i];
                doubleChunk[count + 1] = chunk[i + 1];
                doubleChunk[count + 2] = chunk[i + 2];
                doubleChunk[count + 3] = chunk[i + 3];
            }
        } else if (pcmBit == AudioFormat.ENCODING_PCM_8BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels) {
                doubleChunk[count] = chunk[i];
                doubleChunk[count + 1] = chunk[i + 1];
            }
        }
        return doubleChunk;
    }

    /**
     * 单声道数据转双声道
     *
     * @param chunk  pcm数据
     * @param pcmBit pcm位数,  AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @return 双声道数据
     */
    private static byte[] channel1to2(byte[] chunk, int pcmBit) {
        byte[] doubleChunk = new byte[chunk.length * 2];

        if (pcmBit == AudioFormat.ENCODING_PCM_16BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 4, i += 2) {
                doubleChunk[count] = chunk[i];
                doubleChunk[count + 1] = chunk[i + 1];
                doubleChunk[count + 2] = chunk[i];
                doubleChunk[count + 3] = chunk[i + 1];
            }
        } else if (pcmBit == AudioFormat.ENCODING_PCM_8BIT) {
            for (int i = 0, count = 0; i < chunk.length; count += 2, i++) {
                doubleChunk[count] = chunk[i];
                doubleChunk[count + 1] = chunk[i];
            }
        }
        return doubleChunk;
    }

    /**
     * 计算音量分贝增减后的变化值
     *
     * @param db 增加或降低的分贝值
     * @return 该结果建议作为amplifyPCMData的入参
     */
    private static float audioDB(int db) {
        return (float) Math.pow(10, (double) db / 20);
    }

    /**
     * 音频增高或降低多少分贝
     *
     * @param pcmData  需要转换的pcm数据
     * @param size     pcm数据大小
     * @param newData  转换后的数据
     * @param multiple 音量大小变化值,该值来源于audioDB函数的计算结果,避免多次计算
     */
    private static void audioAmplify(byte[] pcmData, int size, byte[] newData, float multiple) {
        short shortMax = (short) 0x7F00;
        short shortMin = (short) -0x7F00;
        int nCur = 0;
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
