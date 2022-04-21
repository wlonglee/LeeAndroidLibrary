package com.lee.metronome;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;


/**
 * 解码音频文件,提取pcm流
 *
 * @author lee by 2020/8/13
 */
class AudioDecoder {
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
    //任务已经结束
    private static final int TASK_END = 0x04;

    public interface AudioListener {
        void onPrepare(int sample, int pcm, int channels);

        void onDecoderError(String error);

        void onAudioData(byte[] data);

        void onEnd();
    }

    private AudioListener listener;

    private MediaCodec codec;

    private MediaExtractor extractor;

    private MediaFormat format;

    private MediaCodec.BufferInfo bufferInfo;


    private int channels = 2;

    private int sample = 48000;

    private int pcm = AudioFormat.ENCODING_PCM_16BIT;

    private DecoderThread decoderTask;

    private volatile int taskStatus = TASK_NO_START;

    /**
     * 0表示暂停
     */
    private volatile int playbackParams = 0;

    private volatile boolean stop = true;

    /**
     * 是否自动启动解码
     */
    private final boolean isAuto;

    public AudioDecoder(boolean isAuto) {
        this.isAuto = isAuto;
    }

    public AudioDecoder() {
        this(false);
    }


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

    private void onEnd() {
        if (listener != null) {
            listener.onEnd();
        }
    }

    private void loadAudio() {
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
                startTask();
            }
        }
    }

    private void configCodec(int index, String mime) {
        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            LE("channels:"+channels);
        }

        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sample = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        }

        if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pcm = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
            } else {
                pcm = AudioFormat.ENCODING_PCM_16BIT;
            }
        }


        extractor.selectTrack(index);
        try {
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            bufferInfo = new MediaCodec.BufferInfo();
            decoderTask = new DecoderThread();
            if (listener != null) {
                listener.onPrepare(sample, pcm, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            onError("create decoder error:" + e.toString());
            LE("create decoder error:" + e.toString());
        }
    }

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
                decoderTask.start();
            }
        }
    }


    private void release() {

        codec.stop();
        codec.release();

        extractor.release();

        codec = null;
        extractor = null;
        format = null;
        decoderTask = null;

        onEnd();
    }

    private class DecoderThread extends Thread {

        private volatile boolean dos;
        private volatile boolean ros;

        @Override
        public void run() {
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
                if (stop) {
                    break;
                }

                while (playbackParams == 0) {
                    sleep();
                    if (stop) {
                        break task;
                    }
                }
                if (!dos)
                    dealDecoderData();

                if (!ros)
                    dealRenderData();

                if (dos && ros) {
                    break;
                }
            }

            taskStatus = TASK_END;
            LE("task end");
            //解码完成
            release();
        }

        private void dealDecoderData() {
            int index = codec.dequeueInputBuffer(1000 * 16);
            if (index >= 0) {
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                if (inputBuffer == null) {
                    LE("inputBuffer is null");
                    return;
                }
                int sampleSize = extractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            BUFFER_FLAG_END_OF_STREAM
                    );
                    dos = true;
                } else {
                    long sampleTime = extractor.getSampleTime();
                    codec.queueInputBuffer(
                            index,
                            0,
                            sampleSize,
                            sampleTime,
                            0
                    );

                    extractor.advance();
                }
            }

        }

        private void dealRenderData() {
            int index = codec.dequeueOutputBuffer(bufferInfo, 1000 * 16);
            if (index >= 0) {
                long pts = bufferInfo.presentationTimeUs;
//                LE("sampleTime2:" + (pts * 1.0f / duration) * 100);
                if (pts >= 0) {
                    render(index);
                }
                int flag = bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                if (flag != 0) {
                    ros = true;
                }
            }
        }

        private void render(int index) {
            ByteBuffer buffer = codec.getOutputBuffer(index);
            if (buffer == null) {
                LE("buffer is null");
                return;
            }
            byte[] chunk = new byte[bufferInfo.size];
            buffer.get(chunk);
            buffer.clear();

            if (channels == 1) {
                onAudioData(chunk);
            } else{
                onAudioData(channelSingleLeft(chunk));
            }
            codec.releaseOutputBuffer(index, false);
        }



        private byte[] channelSingleLeft(byte[] chunk) {
            byte[] singleChunk = new byte[chunk.length / channels];

            for (int i = 0, count = 0; i < chunk.length; count += 2, i += channels * 2) {
                singleChunk[count] = chunk[i];
                singleChunk[count + 1] = chunk[i + 1];
            }
            return singleChunk;
        }


        private void sleep() {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
