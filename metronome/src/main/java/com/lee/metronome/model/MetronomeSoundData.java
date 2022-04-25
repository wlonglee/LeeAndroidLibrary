package com.lee.metronome.model;

import com.lee.metronome.MetronomeLog;
import com.lee.metronome.type.BeatType;

import java.util.ArrayList;

/**
 * 节拍发声声音数据
 *
 * @author lee
 * @date 2021/1/14
 */
public class MetronomeSoundData {
    //各类型节拍数据
    private final BeatData dripBeat = new BeatData(); //强拍
    private final BeatData dropBeat = new BeatData(); //次强拍
    private final BeatData tickBeat = new BeatData(); //弱拍
    private final BeatData noneBeat = new BeatData(); //空拍(没有声音)

    //拍子数据大小
    private int beatSize;

    /**
     * 设置原始数据
     */
    public void settingAllSrc(byte[] srcDrip, byte[] srcDrop, byte[] srcTick) {
        dripBeat.totalSize = srcDrip.length;
        dripBeat.initData();
        dripBeat.addData(srcDrip);

        dropBeat.totalSize = srcDrop.length;
        dropBeat.initData();
        dropBeat.addData(srcDrop);

        tickBeat.totalSize = srcTick.length;
        tickBeat.initData();
        tickBeat.addData(srcTick);

        MetronomeLog.INSTANCE.log("dripBeat:" + dripBeat.totalSize + "," + "dropBeat:" + dropBeat.totalSize + "," + "tickBeat:" + tickBeat.totalSize);
    }

    public void resetAllSrc() {
        dripBeat.resetTotalSize();
        dropBeat.resetTotalSize();
        tickBeat.resetTotalSize();
    }

    /**
     * 追加原始数据大小,仅适用于解码场景
     */
    public void addTotalSize(BeatType beatType, int size) {
        switch (beatType) {
            case DRIP:
                dripBeat.totalSize += size;
                break;
            case DROP:
                dropBeat.totalSize += size;
                break;
            case TICK:
                tickBeat.totalSize += size;
                break;
        }
    }

    /**
     * 设置原始数据,仅适用于解码场景
     */
    public void settingSrc(BeatType beatType, ArrayList<byte[]> data) {
        switch (beatType) {
            case DRIP:
                dripBeat.initData();
                for (byte[] d : data) {
                    dripBeat.addData(d);
                }
                break;
            case DROP:
                dropBeat.initData();
                for (byte[] d : data) {
                    dropBeat.addData(d);
                }
                break;
            case TICK:
                tickBeat.initData();
                for (byte[] d : data) {
                    tickBeat.addData(d);
                }
                break;
        }
    }

    public boolean isSrcReady() {
        return dripBeat.isReady() && dropBeat.isReady() && tickBeat.isReady();
    }

    /**
     * 生成指定长度的各拍数据
     */
    public void generateBeatData(int size) {
        beatSize = size;
        dripBeat.settingBeat(size);
        dropBeat.settingBeat(size);
        tickBeat.settingBeat(size);
    }

    /**
     * 生成空拍数据
     */
    public void generateNoneData() {
        noneBeat.totalSize = beatSize;
        noneBeat.initNoneData();
        noneBeat.settingBeat(beatSize);
    }


    public byte[] getBeat(BeatType beatType) {
        switch (beatType) {
            case DRIP:
                return dripBeat.getBeat();
            case DROP:
                return dropBeat.getBeat();
            case TICK:
                return tickBeat.getBeat();
            default:
                return noneBeat.getBeat();
        }
    }
}
