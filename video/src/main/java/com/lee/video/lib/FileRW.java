package com.lee.video.lib;

import android.os.Environment;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 组合文件读写
 *
 * @author lee
 * @date 2021/10/26
 */
public final class FileRW {
    /**
     * 存储根目录,赋值可作为全局统一的根布局,如有单独更改的需求,可在Builder中进行customPath的设置
     */
    public static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/";

    /**
     * 文件流操作
     */
    private RandomAccessFile raf;

    /**
     * 文件存储地址
     */
    private String filePath;

    /**
     * 获取文件存储地址
     */
    public String getFilePath() {
        return filePath;
    }

    private FileRW() {
    }

    /**
     * 构建文件写入
     */
    public static class WriteBuilder {
        //自定义根目录名
        private String customPath = "";

        //自定义子目录名
        private String dir = "";

        //文件名称
        private String fileName = "test";

        //文件后缀
        private String fileSuffix = "pcm";

        //文件大小,创建固定大小文件
        private long fileSize = 0;

        //文件已存在的情况下是否先删除该文件
        private boolean replace = true;

        //是否在文件结尾追加
        private boolean add = false;

        public WriteBuilder setCustomPath(String customPath) {
            this.customPath = customPath;
            return this;
        }

        public WriteBuilder setDir(String dir) {
            this.dir = dir;
            return this;
        }

        public WriteBuilder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public WriteBuilder setFileSuffix(String fileSuffix) {
            this.fileSuffix = fileSuffix;
            return this;
        }

        public WriteBuilder setFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public WriteBuilder setReplace(boolean replace) {
            this.replace = replace;
            return this;
        }

        public WriteBuilder setAdd(boolean add) {
            this.add = add;
            return this;
        }

        public FileRW build() {
            FileRW fileRW = new FileRW();
            fileRW.generateWrite(customPath, dir, fileName, fileSuffix, fileSize, replace, add);
            return fileRW;
        }
    }

    private void generateWrite(String customPath, String dir, String fileName, String fileSuffix, long fileSize, boolean replace, boolean add) {
        File dirFile;

        if (!"".equals(customPath)) {
            dirFile = new File(customPath + dir);
        } else {
            dirFile = new File(path + dir);
        }

        if (!dirFile.exists()) {
            //目录不存在,创建目录
            boolean mkDir = dirFile.mkdirs();
        }
        File file = new File(dirFile.getPath(), fileName + "." + fileSuffix);

        //需要删除原文件
        if (replace && file.exists()) {
            boolean d = file.delete();
        }
        filePath = file.getPath();

        try {
            raf = new RandomAccessFile(file, "rw");

            //设置文件大小
            if (fileSize > 0) {
                raf.setLength(fileSize);
            }

            //追加的时候,跳转到文件最后
            if (add) {
                raf.seek(raf.length());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 写入数据
     */
    public void writeData(@NotNull byte[] data) {
        writeData(data, data.length);
    }

    /**
     * 写入数据
     */
    public void writeData(@NotNull byte[] data, int size) {
        try {
            raf.write(data, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建文件读取
     */
    public static class ReadBuilder {
        //自定义根目录名
        private String customPath = "";

        //目录名
        private String dir = "";

        //文件名称
        private String fileName = "test";

        //文件后缀
        private String fileSuffix = "pcm";

        public ReadBuilder setCustomPath(String customPath) {
            this.customPath = customPath;
            return this;
        }

        public ReadBuilder setDir(String dir) {
            this.dir = dir;
            return this;
        }

        public ReadBuilder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public ReadBuilder setFileSuffix(String fileSuffix) {
            this.fileSuffix = fileSuffix;
            return this;
        }

        public FileRW build() {
            FileRW fileRW = new FileRW();
            fileRW.generateRead(customPath, dir, fileName, fileSuffix);
            return fileRW;
        }
    }

    private void generateRead(String customPath, String dir, String fileName, String fileSuffix) {
        File dirFile;

        if (!"".equals(customPath)) {
            dirFile = new File(customPath + dir);
        } else {
            dirFile = new File(path + dir);
        }

        File file = new File(dirFile.getPath(), fileName + "." + fileSuffix);
        filePath = file.getPath();

        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取数据
     */
    public int readData(@NotNull byte[] buffer) {
        return readData(buffer, buffer.length);
    }

    /**
     * 读取数据
     */
    public int readData(@NotNull byte[] buffer, int size) {
        try {
            return raf.read(buffer, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 跳转至指定点
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
        return new File(filePath).delete();
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
        return -1;
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
