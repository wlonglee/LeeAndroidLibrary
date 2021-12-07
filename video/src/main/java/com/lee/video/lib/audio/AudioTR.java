package com.lee.video.lib.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

/**
 * 简化AudioTrack、AudioRecord的使用
 *
 * @author lee
 * @date 2021/10/28
 */
public final class AudioTR {
    /**
     * 音频播放
     */
    private AudioTrack audioTrack;

    /**
     * audioTrack是否就绪
     */
    private boolean trackPre = false;

    /**
     * 音频录制
     */
    private AudioRecord audioRecord;

    /**
     * audioRecord是否就绪
     */
    private boolean recordPre = false;

    /**
     * 录音监听器
     */
    private AudioRecordListener audioRecordListener;

    /**
     * 录音线程
     */
    private RecordThread recordThread;


    private AudioTR() {
    }

    /**
     * 创建audioTrack
     */
    public static class AudioTrackBuilder {
        //pcm位数
        private int pcmEncodeBit = AudioFormat.ENCODING_PCM_16BIT;

        //双声道
        private int channel = AudioFormat.CHANNEL_OUT_STEREO;

        //采样率
        private int sampleRate = 48000;

        //音频类型
        private int streamType = AudioManager.STREAM_MUSIC;

        //音频模式 -- 模式为AudioTrack.MODE_STATIC时需要写入音频数据的长度
        private int streamMode = AudioTrack.MODE_STREAM;

        //音频模式为静态时,需要告知音频数据长度
        private int staticSize = 0;

        //自否自动启动播放,AudioTrack.MODE_STREAM模式下推荐为true
        private boolean auto = false;

        public AudioTrackBuilder setPcmEncodeBit(int pcmEncodeBit) {
            this.pcmEncodeBit = pcmEncodeBit;
            return this;
        }

        public AudioTrackBuilder setChannel(int channel) {
            this.channel = channel;
            return this;
        }

        public AudioTrackBuilder setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public AudioTrackBuilder setStreamType(int streamType) {
            this.streamType = streamType;
            return this;
        }

        public AudioTrackBuilder setStreamMode(int streamMode) {
            this.streamMode = streamMode;
            return this;
        }

        public AudioTrackBuilder setStaticSize(int staticSize) {
            this.staticSize = staticSize;
            return this;
        }

        public AudioTrackBuilder setAuto(boolean auto) {
            this.auto = auto;
            return this;
        }

        public AudioTR build() {
            AudioTR audioTR = new AudioTR();
            audioTR.generateAudioTrack(sampleRate, channel, pcmEncodeBit, streamType, streamMode, staticSize, auto);
            return audioTR;
        }
    }

    private void generateAudioTrack(int sampleRate, int channel, int pcmEncodeBit, int streamType, int streamMode, int staticSize, boolean auto) {
        int minBufferSize =
                AudioTrack.getMinBufferSize(sampleRate, channel, pcmEncodeBit);

        if (streamMode == AudioTrack.MODE_STATIC) {
            minBufferSize = staticSize;
        }
        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setLegacyStreamType(streamType)
                        .build(),
                new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channel)
                        .setEncoding(pcmEncodeBit)
                        .build(),
                minBufferSize,
                streamMode,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (auto) {
            playTrack();
        }
    }

    /**
     * 检测track是否ok
     */
    private boolean checkTrack() {
        return trackPre && audioTrack != null;
    }

    /**
     * 启动播放
     */
    public void playTrack() {
        if (!trackPre && audioTrack != null) {
            trackPre = true;
            audioTrack.play();
        }
    }

    /**
     * 写入数据
     */
    public void writeData(byte[] chunk, int size) {
        if (checkTrack()) {
            audioTrack.write(chunk, 0, size);
        }
    }

    /**
     * 设置音量
     */
    public void setVolume(float volume) {
        if (checkTrack()) {
            audioTrack.setVolume(volume);
        }
    }


    /**
     * 暂停播放
     */
    public void pauseTrack() {
        if (checkTrack()) {
            audioTrack.pause();
        }
    }

    /**
     * 停止播放并释放资源
     */
    public void stopTrack() {
        if (checkTrack()) {
            trackPre = false;
            audioTrack.stop();
            audioTrack.release();
            //对象已经被释放,如需再次使用,需要重新创建对象
            audioTrack = null;
        }
    }

    /**
     * 创建录音器
     */
    public static class AudioRecordBuilder {
        //pcm位数
        private int pcmEncodeBit = AudioFormat.ENCODING_PCM_16BIT;

        //双声道
        private int channels = AudioFormat.CHANNEL_IN_STEREO;

        //采样率
        private int sampleRate = 48000;

        //数据来源
        private int audioSource = MediaRecorder.AudioSource.MIC;

        private AudioRecordListener listener;

        //自否自动启动录制
        private boolean auto = false;

        public AudioRecordBuilder setPcmEncodeBit(int pcmEncodeBit) {
            this.pcmEncodeBit = pcmEncodeBit;
            return this;
        }

        public AudioRecordBuilder setChannels(int channels) {
            this.channels = channels;
            return this;
        }

        public AudioRecordBuilder setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public AudioRecordBuilder setAudioSource(int audioSource) {
            this.audioSource = audioSource;
            return this;
        }

        public AudioRecordBuilder setAuto(boolean auto) {
            this.auto = auto;
            return this;
        }

        public AudioRecordBuilder setListener(AudioRecordListener listener) {
            this.listener = listener;
            return this;
        }

        public AudioTR build() {
            AudioTR audioTR = new AudioTR();
            audioTR.generateAudioRecord(sampleRate, channels, pcmEncodeBit, audioSource, auto, listener);
            return audioTR;
        }
    }

    private void generateAudioRecord(int sampleRate, int channels, int pcmEncodeBit, int audioSource, boolean auto, AudioRecordListener listener) {
        int recordBufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, pcmEncodeBit);
        audioRecord = new AudioRecord(
                audioSource,
                sampleRate,
                channels,
                pcmEncodeBit,
                recordBufferSize);
        audioRecordListener = listener;
        if (auto) {
            startRecord();
        }
    }

    /**
     * 检测record是否ok
     */
    private boolean checkRecord() {
        return recordPre && audioRecord != null && recordThread == null;
    }

    /**
     * 启动录音
     */
    public void startRecord() {
        if (!recordPre && audioRecord != null && recordThread == null) {
            recordPre = true;
            recordThread = new RecordThread();
            recordThread.startRecord();
        }
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        if (checkRecord()) {
            recordThread.pauseRecord();
        }
    }

    /**
     * 恢复录音
     */
    public void restoreRecord() {
        if (checkRecord()) {
            recordThread.restoreRecord();
        }
    }

    /**
     * 停止录音并释放资源，由于录音处于子线程,所以真正的释放时机会到onRecordEnd中
     */
    public void stopRecord() {
        if (checkRecord()) {
            recordThread.stopRecord();
        }
    }


    /**
     * 录音结束并完全释放资源
     */
    private void onRecordEnd() {
        if (audioRecordListener != null) {
            recordPre = false;
            audioRecordListener.onRecordEnd();
            //录音结束后,录音器对象已经被释放,想要录音需再次创建录音对象
            //此处避免内存泄露,将对象全部清除
            audioRecordListener = null;
            recordThread = null;
        }
    }

    /**
     * 录音数据回调
     */
    private void onRecordData(byte[] data) {
        if (audioRecordListener != null) {
            audioRecordListener.onRecordData(data);
        }
    }

    /**
     * 录音状态
     */
    private enum Status {
        //未开始
        NO_RECORD,
        //录音中
        RECORDING,
        //暂停
        PAUSE
    }


    public interface AudioRecordListener {
        /**
         * 录音数据回调
         */
        void onRecordData(byte[] data);

        /**
         * 录音结束
         */
        void onRecordEnd();
    }

    /**
     * 录音子线程
     */
    private class RecordThread extends Thread {
        //录音状态
        private Status recordStatus = Status.NO_RECORD;

        //启动录音
        void startRecord() {
            if (recordStatus == Status.RECORDING) {
                return;
            }
            recordStatus = Status.RECORDING;
            audioRecord.startRecording();
            start();
        }

        //暂停录音
        void pauseRecord() {
            if (recordStatus == Status.RECORDING) {
                recordStatus = Status.PAUSE;
            }
        }

        //恢复录音
        void restoreRecord() {
            if (recordStatus == Status.PAUSE) {
                recordStatus = Status.RECORDING;
            }
        }

        //停止录音
        void stopRecord() {
            if (recordStatus == Status.RECORDING || recordStatus == Status.PAUSE) {
                recordStatus = Status.NO_RECORD;
            }
        }

        @Override
        public void run() {
            byte[] data = new byte[4096];
            int readSize;
            while (recordStatus != Status.NO_RECORD) {
                readSize = audioRecord.read(data, 0, 4096);
                if (recordStatus == Status.RECORDING) {
                    //录制中回调录音数据
                    byte[] chunk = new byte[readSize];
                    System.arraycopy(data, 0, chunk, 0, readSize);
                    onRecordData(chunk);
                }
            }

            //录音结束,释放对象
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            onRecordEnd();
        }
    }
}
