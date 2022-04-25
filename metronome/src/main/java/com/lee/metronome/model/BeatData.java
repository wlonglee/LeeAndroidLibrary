package com.lee.metronome.model;

/**
 * 特定音频的节拍数据
 *
 * @author lee
 * @date 2021/1/13
 */
public class BeatData {
    /**
     * 参与播放的拍子数据,在当前节拍下计算出的每拍数据
     */
    private byte[] beat;

    /**
     * 该拍子的原始数据,解码获取到的原始音频数据
     */
    private byte[] srcData;

    /**
     * 原始数据是否准备就绪
     * true表示数据就绪
     */
    private boolean ready = false;

    /**
     * 原始数据总大小
     */
    public int totalSize;

    /**
     * 追加数据的偏移量
     */
    private int offset = 0;

    public void resetTotalSize() {
        ready = false;
        totalSize = 0;
        offset = 0;
    }

    public void initData() {
        ready = false;
        srcData = new byte[totalSize];
        offset = 0;
    }

    public void addData(byte[] data) {
        System.arraycopy(data, 0, srcData, offset, data.length);
        offset += data.length;
        if (offset == totalSize) {
            ready = true;
        }
    }

    /**
     * 配置空拍数据
     */
    public void initNoneData() {
        srcData = new byte[totalSize];
        ready = true;
    }

    public void settingBeat(int size) {
        beat = new byte[size];
        int realSize = Math.min(size, totalSize);
        System.arraycopy(srcData, 0, beat, 0, realSize);
    }

    public byte[] getBeat() {
        return beat;
    }

    public boolean isReady() {
        return ready;
    }
}
