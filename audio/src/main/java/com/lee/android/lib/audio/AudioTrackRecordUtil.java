package com.lee.android.lib.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

/**
 * 组合audioTrack、audioRecord
 *
 * @author lee by 2020/10/13
 */
public class AudioTrackRecordUtil {

    /**
     * 音频播放
     */
    private static AudioLeeTrack audioLeeTrack;

    /**
     * 录音
     */
    private static AudioLeeRecord audioLeeRecord;


    /**
     * 创建一个声音播放器
     *
     * @param sampleRate   采样率
     * @param pcmEncodeBit 采样位数  AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @param channel      通道数 1单声道 2双声道
     */
    public static void createAudioTrack(int sampleRate, int pcmEncodeBit, int channel) {
        audioLeeTrack = new AudioLeeTrack();
        audioLeeTrack.createAudioTrack(sampleRate, pcmEncodeBit, channel);
    }

    /**
     * 播放中写入数据
     */
    public static void writeData(byte[] chunk, int size) {
        if (audioLeeTrack != null)
            audioLeeTrack.writeData(chunk, size);
    }

    /**
     * 暂停播放
     */
    public static void pauseTrack() {
        if (audioLeeTrack != null)
            audioLeeTrack.pauseTrack();
    }

    /**
     * 恢复播放
     */
    public static void restoreTrack() {
        if (audioLeeTrack != null)
            audioLeeTrack.playTrack();
    }

    /**
     * 关闭声音播放器
     */
    public static void stopAudioTrack() {
        if (audioLeeTrack != null) {
            audioLeeTrack.closeTrack();
            audioLeeTrack = null;
        }
    }

    /**
     * 创建单声道录音
     *
     * @param sampleRate   采样率
     * @param pcmEncodeBit 位数, AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @param listener     监听器
     */
    public static void createAudioRecord(int sampleRate, int pcmEncodeBit, RecordListener listener) {
        createAudioRecord(sampleRate, AudioFormat.CHANNEL_IN_MONO, pcmEncodeBit, listener);
    }

    /**
     * 创建双声道录音
     *
     * @param sampleRate   采样率
     * @param channel      单声道1 双声道 2
     * @param pcmEncodeBit 位数, AudioFormat.ENCODING_PCM_16BIT/AudioFormat.ENCODING_PCM_8BIT
     * @param listener     录音回调
     */
    public static void createAudioRecord(int sampleRate, int channel, int pcmEncodeBit, RecordListener listener) {
        audioLeeRecord = new AudioLeeRecord(listener);
        int channels = AudioFormat.CHANNEL_IN_MONO;
        if (channel >= 2) {
            channels = AudioFormat.CHANNEL_IN_STEREO;
        }
        audioLeeRecord.createAudioRecord(sampleRate, channels, pcmEncodeBit);
    }

    /**
     * 启动录音
     */
    public static void startRecord() {
        if (audioLeeRecord != null)
            audioLeeRecord.startRecord();
    }

    /**
     * 暂停录音
     */
    public static void pauseRecord() {
        if (audioLeeRecord != null)
            audioLeeRecord.pauseRecord();
    }

    /**
     * 恢复录音
     */
    public static void restoreRecord() {
        if (audioLeeRecord != null)
            audioLeeRecord.restoreRecord();
    }

    /**
     * 停止录音
     */
    public static void stopRecord() {
        if (audioLeeRecord != null)
            audioLeeRecord.stopRecord();
    }

    /**
     * 录音相关回调
     */
    public interface RecordListener {
        /**
         * 录音数据
         *
         * @param data pcm数据
         * @param size 数据大小
         */
        void recordOfByte(byte[] data, int size);

        /**
         * 录音结束,调用stopRecord后触发
         */
        void recordEnd();
    }

    static class AudioLeeRecord {
        private AudioRecord audioRecord;
        private int bufferSizeInBytes;

        private RecordThread recordThread;

        private RecordListener listener;
        private Status recordStatus = Status.NO_RECORD;

        enum Status {
            //未开始
            NO_RECORD,
            //录音中
            RECORDING,
            //暂停
            PAUSE
        }

        AudioLeeRecord(RecordListener listener) {
            this.listener = listener;
        }

        void pauseRecord() {
            recordThread.pauseRecord();
        }

        void restoreRecord() {
            recordThread.restoreRecord();
        }

        void stopRecord() {
            recordThread.stopRecord();
        }

        public class RecordThread extends Thread {

            void startRecord() {

                if (recordStatus == Status.RECORDING) {
                    return;
                }
                recordStatus = Status.RECORDING;
                audioRecord.startRecording();
                start();
            }

            void pauseRecord() {
                if (recordStatus == Status.RECORDING) {
                    recordStatus = Status.PAUSE;
                }
            }

            void restoreRecord() {
                if (recordStatus == Status.PAUSE) {
                    recordStatus = Status.RECORDING;
                }
            }

            void stopRecord() {
                if (recordStatus == Status.RECORDING || recordStatus == Status.PAUSE) {
                    recordStatus = Status.NO_RECORD;
                }
            }

            @Override
            public void run() {
                byte[] data = new byte[bufferSizeInBytes];
                int readSize;
                while (recordStatus != Status.NO_RECORD) {
                    readSize = audioRecord.read(data, 0, bufferSizeInBytes);
                    if (recordStatus == Status.RECORDING && listener != null) {
                        listener.recordOfByte(data, readSize);
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;

                if (listener != null) {
                    listener.recordEnd();
                }
                listener = null;
                audioLeeRecord = null;

            }
        }

        void createAudioRecord(int sampleRate, int channel, int pcmEncodeBit) {
            bufferSizeInBytes = AudioRecord.getMinBufferSize(
                    sampleRate,
                    channel,
                    pcmEncodeBit);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channel,
                    pcmEncodeBit,
                    bufferSizeInBytes);
        }

        void startRecord() {
            recordThread = new RecordThread();
            recordThread.startRecord();
        }
    }


    public static class AudioLeeTrack {
        private AudioTrack audioTrack;

        void createAudioTrack(int sampleRate, int pcmEncodeBit, int channel) {
            int channels = AudioFormat.CHANNEL_OUT_MONO;
            if (channel == 2) {
                channels = AudioFormat.CHANNEL_OUT_STEREO;
            }


            int minBufferSize =
                    AudioTrack.getMinBufferSize(sampleRate, channels, pcmEncodeBit);
            audioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setChannelMask(channels)
                            .setSampleRate(sampleRate)
                            .setEncoding(pcmEncodeBit)
                            .build(),
                    minBufferSize,
                    AudioTrack.MODE_STREAM,

                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            audioTrack.setVolume(1);
            audioTrack.play();
        }

        void pauseTrack() {
            if (audioTrack != null) {
                audioTrack.pause();
            }
        }

        void playTrack() {
            if (audioTrack != null) {
                audioTrack.play();
            }
        }

        void writeData(byte[] chunk, int size) {
            audioTrack.write(chunk, 0, size);
        }

        void closeTrack() {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}
