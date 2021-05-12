package com.lee.android.lib.audio;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;


/**
 * 解码音频文件,提取pcm流
 *
 * @author lee by 2020/8/13
 */
public class AudioDecoder {
    /**
     * 是否输出log
     */
    public static boolean isDebug = false;

    //任务尚未开始
    private static final int TASK_NO_START = 0x01;
    //任务正在进行中
    private static final int TASK_ING = 0x02;
    //任务暂停中
    private static final int TASK_PAUSE = 0x03;
    //任务终止
    private static final int TASK_STOP = 0x04;
    //任务完成
    private static final int TASK_END = 0x05;

    //解码回调
    public interface AudioListener {
        /**
         * 解码器准备就绪
         *
         * @param sample   采样率
         * @param pcm      pcm位数
         * @param channels 音频通道数
         */
        void onPrepare(int sample, int pcm, int channels);

        /**
         * 解码出错
         */
        void onDecoderError(String error);

        /**
         * 解码到的音频PCM数据
         */
        void onAudioData(byte[] data);

        /**
         * 解码进度
         */
        void onProgress(float p);

        /**
         * 解码终止
         */
        void onStop();

        /**
         * 解码完成
         */
        void onEnd();
    }

    /**
     * 解码相关回调
     */
    private AudioListener listener;

    /**
     * 解码器
     */
    private MediaCodec codec;

    /**
     * 文件提取器
     */
    private MediaExtractor extractor;

    /**
     * 音频格式
     */
    private MediaFormat format;

    /**
     * 缓冲区
     */
    private MediaCodec.BufferInfo bufferInfo;

    /**
     * 音轨通道数
     */
    private int channels;

    /**
     * 采样率
     */
    private int sample;

    /**
     * pcm位数
     */
    private int pcmBit;

    /**
     * 解码线程
     */
    private DecoderThread decoderTask;

    /**
     * 解码任务标志
     */
    private volatile int taskStatus = TASK_NO_START;

    /**
     * 0表示暂停
     */
    private volatile int playbackParams = 0;

    /**
     * 解码是否停止
     */
    private volatile boolean stop = true;

    /**
     * 是否自动启动解码线程
     * 为true,在setAudioPath后就会自行解码，所以记得在setAudioPath之前设置listener
     */
    private final boolean isAuto;

    /**
     * 数据长度,单位毫秒值
     */
    private long duration = 1;

    /**
     * 线程池
     */
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    /**
     * @param isAuto 是否自动启用解码
     */
    public AudioDecoder(boolean isAuto) {
        this.isAuto = isAuto;
    }

    public AudioDecoder() {
        this(false);
    }

    /**
     * 设置监听,isAuto为true的时候,需要在setAudioPath之前调用
     * @param listener
     */
    public void setListener(AudioListener listener) {
        this.listener = listener;
    }

    private void LE(String msg) {
        if (isDebug)
            Log.e("lee_decoder", msg);
    }

    /**
     * 设置解析的音频文件路径
     */
    public void setAudioPath(String path) {
        extractor = new MediaExtractor();
        try {
            taskStatus = TASK_NO_START;
            extractor.setDataSource(path);
            //加载音频
            loadAudio();
        } catch (Exception e) {

            e.printStackTrace();
            onError("setDataSource error:" + e.toString());
            LE("setDataSource error:" + e.toString());
        }
    }

    /**
     * 设置资源目录中的文件
     */
    public void setAudioPath(AssetFileDescriptor afd) {
        setAudioPath(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
    }

    public void setAudioPath(FileDescriptor fd, long offset, long length) {
        extractor = new MediaExtractor();
        try {
            taskStatus = TASK_NO_START;
            extractor.setDataSource(fd, offset, length);
            //加载音频
            loadAudio();
        } catch (Exception e) {

            e.printStackTrace();
            onError("setDataSource error:" + e.toString());
            LE("setDataSource error:" + e.toString());
        }
    }


    private void onError(String msg) {
        if (listener != null)
            listener.onDecoderError(msg);
    }

    private void onAudioData(byte[] data) {
        if (listener != null)
            listener.onAudioData(data);
    }

    private void onStop() {
        if (listener != null) {
            listener.onStop();
        }
    }

    private void onEnd() {
        if (listener != null) {
            listener.onEnd();
        }
    }

    private void onProgress(float p) {
        if (listener != null) {
            listener.onProgress(p);
        }
    }

    /**
     * 加载音频数据
     */
    private void loadAudio() {
        //遍历获取音轨轨道
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            String AUDIO_MIME = "audio/";
            if (mime != null && mime.startsWith(AUDIO_MIME)) {
                this.format = format;
                configCodec(i, mime);
                break;
            }
        }

        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        if (codec == null) {
            //未加载到资源
            extractor.release();
            extractor = null;
            onError("no audio resource");
            LE("no audio resource");
        } else {
            if (isAuto) {
                //自动启动解码
                startTask();
            }
        }
    }

    /**
     * 配置数据
     */
    private void configCodec(int index, String mime) {
        //获取音轨信息
        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        }

        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sample = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        }

        if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pcmBit = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
            } else {
                pcmBit = AudioFormat.ENCODING_PCM_16BIT;
            }
        }

        if (format.containsKey(MediaFormat.KEY_DURATION)) {
            duration = format.getLong(MediaFormat.KEY_DURATION);
        }

        //选择该轨
        extractor.selectTrack(index);
        try {
            //创建解码器
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            bufferInfo = new MediaCodec.BufferInfo();
            decoderTask = new DecoderThread();
            if (listener != null) {
                listener.onPrepare(sample, pcmBit, channels);
            }
        } catch (Exception e) {
            e.printStackTrace();
            onError("create decoder error:" + e.toString());
            LE("create decoder error:" + e.toString());
        }
    }

    /**
     * 启动解码
     */
    public void startTask() {
        if (taskStatus == TASK_NO_START) {
            LE("task start");
            taskStatus = TASK_ING;
            playbackParams = 1;
            stop = false;
            if (codec != null) {
                codec.start();
            }
            if (decoderTask != null) {
                pool.execute(decoderTask);
            }
        }
    }

    /**
     * 是否处于解码状态
     */
    public boolean isPlay() {
        return taskStatus == TASK_ING;
    }

    /**
     * 是否处于暂停状态
     */
    public boolean isPause() {
        return taskStatus == TASK_PAUSE;
    }

    /**
     * 暂停解码
     */
    public void pauseTask() {
        if (taskStatus == TASK_ING) {
            taskStatus = TASK_PAUSE;
            playbackParams = 0;
        }
    }

    /**
     * 恢复解码
     */
    public void restoreTask() {
        if (taskStatus == TASK_PAUSE) {
            taskStatus = TASK_ING;
            playbackParams = 1;
        }
    }

    /**
     * 停止解码
     */
    public void stopTask() {
        if (taskStatus == TASK_NO_START || taskStatus == TASK_STOP) {
            onStop();
        } else if (taskStatus == TASK_END) {
            onEnd();
        } else {
            if (!stop) {
                stop = true;
                taskStatus = TASK_END;
            }
        }
    }


    /**
     * 解码或停止后自动释放资源
     */
    private void release() {

        codec.stop();
        codec.release();

        extractor.release();

        codec = null;
        extractor = null;
        format = null;
        decoderTask = null;

        if (taskStatus == TASK_END)
            onEnd();
        else
            onStop();
    }

    /**
     * 解码线程
     */
    private class DecoderThread implements Runnable {

        private volatile boolean dos;
        private volatile boolean ros;

        @Override
        public void run() {
            //解码操作线程
            try {
                decoder();
            } catch (Exception e) {
                e.printStackTrace();
                onError("decoder err:" + e.toString());
                LE("decoder err:" + e.toString());
            }
        }

        private void decoder() {
            task:
            while (true) {
                //停止直接跳出
                if (stop) {
                    break;
                }

                //暂停
                while (playbackParams == 0) {
                    sleep();
                    if (stop) {
                        break task;
                    }
                }

                //解码
                if (!dos)
                    dealDecoderData();

                //渲染
                if (!ros)
                    dealRenderData();

                if (dos && ros) {
                    break;
                }
            }

            if (dos && ros)
                taskStatus = TASK_END;
            else
                taskStatus = TASK_STOP;
            //解码完成
            release();
        }

        /**
         * 解码
         */
        private void dealDecoderData() {
            //获取数据
            int index = codec.dequeueInputBuffer(1000 * 16);
            if (index >= 0) {
                //获取缓存区
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    LE("inputBuffer is null");
                    return;
                }
                //读取数据
                int sampleSize = extractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    //数据读取完成,压入结束标志
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            BUFFER_FLAG_END_OF_STREAM
                    );
                    dos = true;
                } else {
                    //将数据压入缓冲区
                    long sampleTime = extractor.getSampleTime();
                    codec.queueInputBuffer(
                            index,
                            0,
                            sampleSize,
                            sampleTime,
                            0
                    );
                    //进入下一帧
                    extractor.advance();
                }
            }

        }

        /**
         * 渲染数据
         */
        private void dealRenderData() {
            //获取渲染缓冲
            int index = codec.dequeueOutputBuffer(bufferInfo, 1000 * 16);
            if (index >= 0) {
                //渲染时间
                long pts = bufferInfo.presentationTimeUs;
                //获取解码后的PCM数据
                if (pts > duration) {
                    onProgress(1);
                } else {
                    onProgress(((int) (pts * 1f / duration * 10000)) / 100f);
                }

                //渲染结束
                int flag = bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                if (flag != 0) {
                    ros = true;
                }
            }
        }

        /**
         * 渲染
         */
        private void render(int index) {
            //根据缓冲区索引获取缓冲
            ByteBuffer buffer = codec.getOutputBuffer(index);
            if (buffer == null) {
                LE("buffer is null");
                return;
            }

            //获取数据
            byte[] chunk = new byte[bufferInfo.size];
            buffer.get(chunk);
            buffer.clear();
            onAudioData(chunk);
            //释放缓冲区
            codec.releaseOutputBuffer(index, false);
        }

        private void sleep() {
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
