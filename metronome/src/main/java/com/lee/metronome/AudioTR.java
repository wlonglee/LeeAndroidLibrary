package com.lee.metronome;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * 简化AudioTrack、AudioRecord的使用
 *
 * @author lee
 * @date 2021/10/28
 */
class AudioTR {
    /**
     * 音频播放
     */
    private AudioTrack audioTrack;

    /**
     * audioTrack是否就绪
     */
    private boolean trackPre = false;

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
}
