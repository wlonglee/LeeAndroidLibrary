package com.lee.android.lib;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 文件存储工具,构造函数分两类 一类用于写入文件,一类用于读取文件（读取文件仅r模式,增加读取效率）
 * 针对同一个文件写入和读取可同步进行,场景参照--->音频缓存,边下载进行缓存边播放
 *
 * @author lee
 * @date 2021/5/12
 */
public class FileSaveUtil {
    /**
     * 存储根目录
     */
    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/LavaMusic/";

    /**
     * 文件流操作
     */
    private RandomAccessFile raf;

    /**
     * 文件存储地址
     */
    private final String filePath;


    //######文件写入######

    /**
     * 创建一个文件 写入管理
     *
     * @param name      文件名
     * @param customDir 目录名
     * @param needDel   文件已存在的情况下是否删除
     */
    public FileSaveUtil(String name, String customDir, boolean needDel) {
        this(name, customDir, ".pcm", 0, needDel, false);
    }

    /**
     * 创建一个文件 写入管理
     *
     * @param name      文件名
     * @param customDir 目录名
     * @param suffix    文件后缀
     * @param needDel   文件已存在的情况下是否删除
     */
    public FileSaveUtil(String name, String customDir, String suffix, boolean needDel) {
        this(name, customDir, suffix, 0, needDel, false);
    }

    /**
     * 创建一个文件 写入管理
     *
     * @param name      文件名
     * @param customDir 目录名
     * @param suffix    文件后缀
     * @param fileSize  文件大小,可创建固定大小的文件
     * @param needDel   文件存在的情况下是否删除
     * @param isAdd     是否在结尾追加写入
     */
    public FileSaveUtil(String name, String customDir, String suffix, long fileSize, boolean needDel, boolean isAdd) {
        File dir = new File(path + customDir);
        if (!dir.exists()) {
            boolean mkdir = dir.mkdirs();
        }
        File file = new File(dir.getPath(), name + "." + suffix);
        if (needDel && file.exists()) {
            boolean del = file.delete();
        }
        filePath = file.getPath();
        try {
            raf = new RandomAccessFile(file, "rw");
            if (fileSize > 0) {
                raf.setLength(fileSize);
            }

            if (isAdd) {
                raf.seek(raf.length());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入数据
     */
    public void writeData(byte[] data, int size) {
        try {
            raf.write(data, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //######文件写入######


    //######文件读取######

    /**
     * 创建一个文件读取
     *
     * @param name      文件名
     * @param customDir 目录名
     */
    private FileSaveUtil(String name, String customDir) {
        this(name, customDir, ".pcm");
    }

    /**
     * 创建一个文件读取
     *
     * @param name      文件名
     * @param customDir 目录名
     * @param suffix    文件后缀
     */
    private FileSaveUtil(String name, String customDir, String suffix) {
        File dir = new File(path + customDir);
        File file = new File(dir.getPath(), name + "." + suffix);
        filePath = file.getPath();
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 读取数据
     */
    public int readData(byte[] buffer) {
        try {
            return raf.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 读取指定长度数据
     */
    public int readData(byte[] buffer, int size) {
        try {
            return raf.read(buffer, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //######文件读取######

    /**
     * 跳转到指定节点
     */
    public void seek(int pos) {
        try {
            raf.seek(pos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭文件流
     */
    public void close() {
        try {
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文件
     */
    public boolean delFile() {
        close();
        File file = new File(filePath);
        return file.delete();
    }

    /**
     * 获取文件大小
     */
    public long totalSize() {
        try {
            return raf.length();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 更新文件大小
     */
    public void updateFileSize(int fileSize) {
        try {
            raf.setLength(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
