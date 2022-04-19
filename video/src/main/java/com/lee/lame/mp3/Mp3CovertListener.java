package com.lee.lame.mp3;

/**
 * Mp3转码监听器
 *
 * @author lee
 * @date 2021/1/19
 */
public interface Mp3CovertListener {
    void onProgress(float progress);
    void onEnd();
}
