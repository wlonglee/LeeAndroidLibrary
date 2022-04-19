package com.lee.lame.mp3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * mp3工具类
 *
 * @author lee
 * @date 2021/1/18
 */
public class Mp3Util {
    static {
        System.loadLibrary("lame-lib");
    }

    /**
     * 获取lame版本
     */
    public static native String getLameVersion();

    //######整个文件的转换 START######//
    private static Mp3CovertListener mp3CovertListener;

    //设置整文件转码进度监听
    public static void setCovertListener(Mp3CovertListener listener) {
        mp3CovertListener = listener;
    }

    //移除整文件转码进度监听
    public static void removeCovertListener(){
        mp3CovertListener=null;
    }

    //将pcm文件转换为mp3
    public static void convert4Pcm(String pcmPath, int inSample, int inChannel, String mp3Path, int outSample) {
        convert4Pcm(pcmPath, inSample, inChannel, mp3Path, outSample, 5);
    }

    public static void convert4Pcm(String pcmPath, int inSample, int inChannel, String mp3Path, int outSample, int quality) {
        convert2Mp3(pcmPath, inSample, inChannel, mp3Path, outSample, false, quality);
    }

    //将wav文件转换为mp3
    public static void convert4Wav(String wavPath, int inSample, int inChannel, String mp3Path, int outSample) {
        convert4Wav(wavPath, inSample, inChannel, mp3Path, outSample, 5);
    }

    public static void convert4Wav(String wavPath, int inSample, int inChannel, String mp3Path, int outSample, int quality) {
        convert2Mp3(wavPath, inSample, inChannel, mp3Path, outSample, true, quality);
    }

    /**
     * 将wav或pcm文件转换为mp3
     *
     * @param inPath    pcm或wav文件路径
     * @param inSample  pcm或wav的采样率
     * @param inChannel pcm或wav的通道数
     * @param mp3Path   输出的mp3文件路径
     * @param outSample 输出的mp3文件采样率
     * @param isWav     是否是wav文件,wav转换的时候会跳过头部44个字节的数据
     * @param quality   转换的质量 0~9
     */
    private static native void convert2Mp3(String inPath, int inSample, int inChannel, String mp3Path, int outSample, boolean isWav, int quality);

    /**
     * JNI回调函数
     *
     * @param progress 整文件实时转码进度
     */
    public static void convertProgress(float progress) {
        if (mp3CovertListener != null) {
            mp3CovertListener.onProgress(progress);
        }
    }

    public static void convertEnd() {
        if (mp3CovertListener != null) {
            mp3CovertListener.onEnd();
        }
    }

    //######整个文件的转换 END######//

    //######实时pcm流转换 START######//
    private static Mp3Data mp3;

    public static void initRealTimeMp3(int inSample, int inChannel, int outSample, String mp3Path) {
        initRealTimeMp3(inSample, inChannel, outSample, 5, mp3Path);
    }

    /**
     * 初始化一个实时转换pcm到mp3的实例
     *
     * @param inSample  pcm采样率
     * @param inChannel pcm通道数
     * @param outSample 输出采样率
     * @param quality   转换质量0~9
     * @param mp3Path   输出MP3的路径,转换完成时需要添加vbr头信息
     */
    public static void initRealTimeMp3(int inSample, int inChannel, int outSample, int quality, String mp3Path) {
        mp3 = new Mp3Data(inSample, inChannel, outSample, quality, mp3Path);
        initMp3Encoder(inSample, inChannel, outSample, quality);
    }

    /**
     * 实时将pcm流转码为mp3流
     *
     * @param pcm       pcm流数据
     * @param size      pcm流数据实际长度
     * @param mp3Buffer 转换后的mp3流
     * @return 转码得到的mp3数据大小
     */
    public static int encodeRealTimeMp3(byte[] pcm, int size, byte[] mp3Buffer) {
        return encodeMp3(pcm, mp3.inChannel, size, mp3Buffer);
    }

    /**
     * 关闭前刷新缓冲区数据
     *
     * @param mp3Buffer 转换后的mp3流
     * @return 返回缓冲区的数据大小
     */
    public static int flushRealTimeMp3(byte[] mp3Buffer) {
        return flushMp3(mp3Buffer, mp3.outPath);
    }

    /**
     * 关闭实例对象
     */
    public static void closeRealTimeMp3() {
        closeMp3Encoder();
        mp3 = null;
    }


    /**
     * 初始化一个实时转换pcm到mp3的实例
     *
     * @param inSample  pcm采样率
     * @param inChannel pcm通道数
     * @param outSample 输出采样率
     * @param quality   转换质量0~9
     */
    private static native void initMp3Encoder(int inSample, int inChannel, int outSample, int quality);


    private static int encodeMp3(byte[] pcm, int channel, int size, byte[] mp3Buffer) {
        byte[] pcmData;
        if (size != pcm.length) {
            pcmData = new byte[size];
            System.arraycopy(pcm, 0, pcmData, 0, size);
        } else {
            pcmData = pcm;
        }
        short[] pcmLeft;
        short[] pcmRight;
        if (channel > 1) {
            pcmLeft = byte2Short(channelSingleLeft(pcmData, channel));
            pcmRight = byte2Short(channelSingleRight(pcmData, channel));
        } else {
            pcmLeft = byte2Short(pcmData);
            pcmRight = pcmLeft;
        }
        int pcmSize = pcmLeft.length;
        return encodeMp3(pcmLeft, pcmRight, pcmSize, mp3Buffer);
    }

    /**
     * 实时将pcm流转码为mp3流
     *
     * @param pcmLeft   左声道数据
     * @param pcmRight  右声道数据
     * @param size      pcm流数据实际长度
     * @param mp3Buffer 转换后的mp3流
     * @return 转码得到的mp3数据大小
     */
    private static native int encodeMp3(short[] pcmLeft, short[] pcmRight, int size, byte[] mp3Buffer);

    /**
     * 关闭前刷新缓冲区数据
     *
     * @param mp3Buffer 转换后的mp3流
     * @param mp3Path   需要mp3文件的路径,写入MP3 vbr头信息,这样MP3音频波开始的位置不会发生后置偏移,同时长度信息也会准确
     * @return 返回缓冲区的数据大小
     */
    private static native int flushMp3(byte[] mp3Buffer, String mp3Path);

    /**
     * 关闭实例对象
     */
    private static native void closeMp3Encoder();

    //######实时pcm流转换 END######//

    /**
     * byte数组转short
     */
    private static short[] byte2Short(byte[] src) {
        ShortBuffer buffer = ByteBuffer.wrap(src).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] result = new short[buffer.capacity()];
        buffer.get(result);
        return result;
    }

    /**
     * 获取左声道数据
     */
    private static byte[] channelSingleLeft(byte[] chunk, int channels) {
        byte[] singleChunk = new byte[chunk.length / channels];

        for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
            singleChunk[count] = chunk[i];
            singleChunk[count + 1] = chunk[i + 1];
        }
        return singleChunk;
    }

    /**
     * 获取右声道数据
     */
    private static byte[] channelSingleRight(byte[] chunk, int channels) {
        byte[] singleChunk = new byte[chunk.length / channels];

        for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
            singleChunk[count] = chunk[i + 2];
            singleChunk[count + 1] = chunk[i + 3];
        }
        return singleChunk;
    }


    public static double getVolume(byte[] buffer, int size) {
        long v = 0;
        for (byte b : buffer) {
            v += b * b;
        }
        //平方和除以数据总长度,得到音量大小。
        double mean = v / (double) size;
        //        Log.d(TAG, "分贝值:" + volume);
        return 10 * Math.log10(mean);
    }
}
