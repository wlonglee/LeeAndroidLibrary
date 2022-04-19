package com.lee.lame.mp3;

/**
 * 实时转换MP3实例对象
 *
 * @author lee
 * @date 2021/1/19
 */
public class Mp3Data {
    /**
     * pcm流的采样率
     */
    int inSample;
    /**
     * pcm流数据的通道数量
     */
    int inChannel;
    /**
     * 输出mp3的采样率
     */
    int outSample;
    /**
     * 转换质量 0~9  默认5
     */
    int quality;
    /**
     * 输出MP3的路径,转换完成时需要添加vbr头信息
     */
    String outPath;

    public Mp3Data(int inSample, int inChannel, int outSample, int quality, String outPath) {
        this.inSample = inSample;
        this.inChannel = inChannel;
        this.outSample = outSample;
        this.quality = quality;
        this.outPath = outPath;
    }
}
