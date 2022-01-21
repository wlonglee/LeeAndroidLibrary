package com.lee.android.demo.gif;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * 默认的缓存相关管理
 *
 * @author lee
 * @date 2021/5/24
 */
public class StandardBitmapProvider implements GifDecoder.BitmapProvider {

    private StandardBitmapProvider() {
    }

    //全局单例
    private static final StandardBitmapProvider instance = new StandardBitmapProvider();

    public static StandardBitmapProvider getInstance() {
        return instance;
    }

    private static final Object sPoolSync = new Object();

    //缓存池
    private final ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
    private final ArrayList<int[]> intArrayList = new ArrayList<>();
    private final ArrayList<byte[]> byteArrayList = new ArrayList<>();

    /**
     * 清除全部数据
     */
    public void clearAll() {
        synchronized (sPoolSync) {
            for (Bitmap bitmap : bitmapArrayList) {
                bitmap.recycle();
            }
            bitmapArrayList.clear();
            intArrayList.clear();
            byteArrayList.clear();
        }
    }

    @NonNull
    @Override
    public Bitmap obtain(int width, int height, @NonNull Bitmap.Config config) {
        synchronized (sPoolSync) {
            if (bitmapArrayList.size() > 0) {
                return bitmapArrayList.remove(0);
            }
        }
        return Bitmap.createBitmap(width, height, config);
    }

    @Override
    public void release(@NonNull Bitmap bitmap) {
        synchronized (sPoolSync) {
            bitmapArrayList.add(bitmap);
        }
    }

    @NonNull
    @Override
    public byte[] obtainByteArray(int size) {
        synchronized (sPoolSync) {
            if (byteArrayList.size() > 0) {
                return byteArrayList.remove(0);
            }
        }
        return new byte[size];
    }

    @Override
    public void release(@NonNull byte[] bytes) {
        synchronized (sPoolSync) {
            byteArrayList.add(bytes);
        }
    }

    @NonNull
    @Override
    public int[] obtainIntArray(int size) {
        synchronized (sPoolSync) {
            if (intArrayList.size() > 0) {
                return intArrayList.remove(0);
            }
        }
        return new int[size];
    }

    @Override
    public void release(@NonNull int[] array) {
        synchronized (sPoolSync) {
            intArrayList.add(array);
        }
    }
}
