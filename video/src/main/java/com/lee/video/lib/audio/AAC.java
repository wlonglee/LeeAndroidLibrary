package com.lee.video.lib.audio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * pcm编码为aac
 * aac解码为pcm
 *
 * @author lee
 * @date 2021/11/5
 */
public class AAC {

    private AAC() {
    }

    /**
     * 数据回调
     */
    public interface AacListener {
        /**
         * 编解码后的音频数据
         */
        void onAudioData(byte[] data);

        /**
         * 编解码资源释放结束
         */
        void onRelease();
    }

    /**
     * 编解码器
     */
    private MediaCodec codec;
    /**
     * 编解码器-缓存区
     */
    private MediaCodec.BufferInfo bufferInfo;

    /**
     * 编解码数据回调器
     */
    private AacListener aacListener;

    /**
     * aac数据的采样参数
     */
    private int aacSample;

    /**
     * aac数据的级别
     * main 0x01
     * lc  0x02
     * ssr 0x03
     */
    private int aacProfile;
    /**
     * aac数据通道数量
     */
    private int aacChannel;

    /**
     * 编码器是否设置就绪
     */
    private boolean encoderPre = false;

    /**
     * 解码器是否设置就绪
     */
    private boolean decoderPre = false;

    /**
     * 线程池-执行编解码任务
     */
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

    /**
     * 编解码数据队列-缓存当前待编解码的数据
     */
    private final List<byte[]> audioQueue = new ArrayList<>();

    /**
     * 编解码任务等待时间-单位为微秒,1000微秒为1毫秒
     */
    private long waitTime;

    /**
     * 编解码任务是否处于运行中
     */
    private boolean isRun = false;

    /**
     * 停止编解码器,因为缓存的原因,所以需要将数据完全处理完毕
     */
    private boolean isStopTask = false;

    /**
     * 压入线程是否终止
     */
    private volatile boolean dos = false;
    /**
     * 取出线程是否终止
     */
    private volatile boolean ros = false;

    /**
     * aac编码,将pcm流编码为aac
     */
    public static class AacEncoder {
        /**
         * 采样率
         */
        private int sample = 48000;

        /**
         * 通道数量 1-单声道 2-双声道
         */
        private int channelCount = 2;

        /**
         * 比特率率, 96kbps fm音质
         */
        private int bitRate = 96000;
        /**
         * 设定编解码任务的阻塞时间,单位为微秒,1微秒等于1000毫秒
         */
        private long waitTime = 1000 * 16;

        /**
         * 是否自动启动编码器
         */
        private boolean auto = false;


        private AacListener listener;

        public AacEncoder setSample(int sample) {
            this.sample = sample;
            return this;
        }

        public AacEncoder setChannelCount(int channelCount) {
            this.channelCount = channelCount;
            return this;
        }

        public AacEncoder setBitRate(int bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public AacEncoder setWaitTime(long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public AacEncoder setListener(AacListener listener) {
            this.listener = listener;
            return this;
        }

        public AacEncoder setAuto(boolean auto) {
            this.auto = auto;
            return this;
        }

        public AAC build() {
            AAC aac = new AAC();
            aac.generateEncoder(sample, channelCount, waitTime, auto, bitRate, listener);
            return aac;
        }
    }

    /**
     * 构建编码器
     *
     * @param sample   采样率
     * @param channel  通道数
     * @param waitTime 编解码任务等待时间-单位为微秒,1000微秒为1毫秒
     * @param auto     是否自动启动编码器
     * @param bitRate  比特率率, 96kbps fm音质
     * @param listener 监听器
     */
    private void generateEncoder(int sample, int channel, long waitTime, boolean auto, int bitRate, AacListener listener) {
        try {
            aacSample = getADTSSample(sample);
            aacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            aacChannel = channel;
            this.waitTime = waitTime;
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sample, channel);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile);
            if (channel == 2)
                format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO); //双声道
            else
                format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO); //单声道
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);  //aac一帧有1024个采样点    1024*2*2=4096
//            int bitRate = sample * 2 * channel;
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channel);

            //构建编码器
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            bufferInfo = new MediaCodec.BufferInfo();
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            aacListener = listener;

            if (auto) {
                startEncoder();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 压入编解码数据任务
     */
    private final Runnable coderTask = new Runnable() {
        @Override
        public void run() {
            dos = false;
            long pts = 0;
            while (true) {
                if (audioQueue.size() > 0) {
                    int index = codec.dequeueInputBuffer(waitTime);
                    if (index < 0) {
                        continue;
                    }
                    ByteBuffer inputBuffer = codec.getInputBuffer(index);
                    if (inputBuffer == null) {
                        continue;
                    }
                    byte[] inData = audioQueue.remove(0);
                    if (inData != null) {
                        inputBuffer.put(inData, 0, inData.length);
                        codec.queueInputBuffer(index, 0, inData.length, pts, 0);
                        pts += (inData.length / 88.2f) * 1000;
                    } else {
                        codec.queueInputBuffer(index, 0, 0, 0, 0);
                    }
                } else if (isStopTask) {
                    //停止了任务,压入结束标志
                    int index = codec.dequeueInputBuffer(waitTime);
                    if (index < 0) {
                        continue;
                    }
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                }
            }
            dos = true;
            goEnd();
        }
    };

    /**
     * 取出编码好的数据
     */
    private final Runnable encoderTask = new Runnable() {
        @Override
        public void run() {
            int perPcmSize;
            byte[] outData;
            ros = false;
            while (true) {
                //获取索引
                int index = codec.dequeueOutputBuffer(bufferInfo, waitTime);
                if (index >= 0) {
                    ByteBuffer buffer = codec.getOutputBuffer(index);
                    if (buffer == null) {
                        continue;
                    }
                    perPcmSize = bufferInfo.size + 7;
                    outData = new byte[perPcmSize];

                    buffer.position(bufferInfo.offset);
                    buffer.limit(bufferInfo.offset + bufferInfo.size);
                    addADTSHeader(outData, perPcmSize, aacSample, aacProfile, aacChannel);

                    buffer.get(outData, 7, bufferInfo.size);
                    buffer.position(bufferInfo.offset);
                    onAudioData(outData);

                    codec.releaseOutputBuffer(index, false);
                }
                //结束标志
                int flag = bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                if (flag != 0) {
                    break;
                }
            }
            ros = true;
            goEnd();
        }
    };


    /**
     * 启动编码器
     */
    public void startEncoder() {
        if (!encoderPre) {
            //启动编码
            encoderPre = true;
            isRun = false;
            codec.start();
        }
    }

    /**
     * 编码pcm数据为aac
     */
    public void encoderPcm2Aac(byte[] data) {
        encoderPcm2Aac(data, data.length);
    }

    /**
     * 编码pcm数据为aac
     */
    public void encoderPcm2Aac(byte[] data, int size) {
        if (!encoderPre) {
            //编码器未配置
            return;
        }
        byte[] s = new byte[size];
        System.arraycopy(data, 0, s, 0, size);
        audioQueue.add(s);
        if (!isRun) {
            isRun = true;
            isStopTask = false;
            pool.execute(coderTask);
            pool.execute(encoderTask);
        }
    }

    /**
     * 停止编码器
     */
    public void stopEncoder() {
        if (!encoderPre) {
            return;
        }
        isStopTask = true;
        Log.e("lee", "停止编码器:" + audioQueue.size());
    }


    private void release() {
        encoderPre = false;
        decoderPre = false;
        codec.stop();
        codec.release();
        codec = null;
        bufferInfo = null;
        if (aacListener != null) {
            aacListener.onRelease();
        }
    }

    /**
     * aac解码,将aac帧数据解码为pcm流
     */
    public static class AacDecoder {
        /**
         * 采样率
         */
        private int sample = 48000;

        /**
         * 通道数量 1-单声道 2-双声道
         */
        private int channelCount = 2;

        /**
         * 是否自动启动编码器
         */
        private boolean auto = false;

        /**
         * 设定编解码任务的阻塞时间,单位为微秒,1微秒等于1000毫秒
         */
        private long waitTime = 1000 * 16;


        private AacListener listener;

        public AacDecoder setSample(int sample) {
            this.sample = sample;
            return this;
        }

        public AacDecoder setChannelCount(int channelCount) {
            this.channelCount = channelCount;
            return this;
        }

        public AacDecoder setWaitTime(long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public AacDecoder setListener(AacListener listener) {
            this.listener = listener;
            return this;
        }

        public AacDecoder setAuto(boolean auto) {
            this.auto = auto;
            return this;
        }

        public AAC build() {
            AAC aac = new AAC();
            aac.generateDecoder(sample, channelCount, waitTime, auto, listener);
            return aac;
        }
    }

    private void generateDecoder(int sample, int channel, long waitTime, boolean auto, AacListener listener) {
        try {
            //需要解码数据的类型
            aacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            aacSample = getADTSSample(sample);
            aacChannel = channel;
            this.waitTime = waitTime;
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sample, channel);
            //用来标记AAC是否有adts头，1->有
            format.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile);
            //AAC Profile 5bits | 采样率 4bits | 声道数 4bits | 其他 3bits
            // main 0x01  lc 0x02 ssr 0x03
            // 采样率参照getADTSSample
            //其他为0
            //  00010  0100 0010 000 --> 0001 0010 0001 0000 --> 0x12 0x10
            byte[] data = new byte[2];
            data[0] = (byte) ((aacProfile << 3) | (aacSample >>> 1));
            data[1] = (byte) (((aacSample & 0x01) << 7) + (aacChannel << 3));
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            format.setByteBuffer("csd-0", csd_0);


            //构建解码器
            codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            bufferInfo = new MediaCodec.BufferInfo();
            codec.configure(format, null, null, 0);
            aacListener = listener;
            if (auto) {
                startDecoder();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 启动解码器
     */
    public void startDecoder() {
        if (!decoderPre) {
            //启动编码
            decoderPre = true;
            isRun = false;
            codec.start();
        }
    }

    /**
     * 取出解码好的数据
     */
    private final Runnable decoderTask = new Runnable() {
        @Override
        public void run() {
            byte[] outData;
            ros = false;
            while (true) {
                //获取索引
                int index = codec.dequeueOutputBuffer(bufferInfo, waitTime);
                if (index >= 0) {
                    ByteBuffer buffer = codec.getOutputBuffer(index);
                    if (buffer == null) {
                        continue;
                    }
                    outData = new byte[bufferInfo.size];

                    buffer.position(bufferInfo.offset);
                    buffer.limit(bufferInfo.offset + bufferInfo.size);
                    buffer.get(outData);
                    onAudioData(outData);
                    codec.releaseOutputBuffer(index, false);
                }
                //结束标志
                int flag = bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                if (flag != 0) {
                    break;
                }
            }
            ros = true;
            goEnd();
        }
    };

    private void goEnd() {
        if (dos && ros) {
            release();
        }
    }


    /**
     * 解码aac数据为pcm
     */
    public void decoderAac2Pcm(byte[] data, int size) {
        if (!decoderPre) {
            return;
        }
        byte[] s = new byte[size];
        System.arraycopy(data, 0, s, 0, size);
        audioQueue.add(s);
        if (!isRun) {
            isRun = true;
            isStopTask = false;
            pool.execute(coderTask);
            pool.execute(decoderTask);
        }
    }

    /**
     * 停止解码器
     */
    public void stopDecoder() {
        if (!decoderPre) {
            return;
        }
        isStopTask = true;
        Log.e("lee", "停止解码器:" + audioQueue.size());
    }

    /**
     * 编解码好的数据
     */
    private void onAudioData(byte[] outData) {
        if (aacListener != null) {
            aacListener.onAudioData(outData);
        }
    }

    /**
     * 添加aac头信息
     * 一般是7个字节,如果需要对数据进行CRC校验,则额外有2个字节的校验码
     * 7个字节共56位
     * 28位为固定头信息,28位为可变头信息
     * 固定头信息
     * syncword  帧开头标志,固定为0xFFF                  -->12位
     * ID        标识符 0表示MPEG-4,1表示MPEG-2          -->1位
     * layer     固定为0x00                             -->2位
     * absent    0进行CRC校验,1不进行CRC校验             -->1位
     * <p>
     * profile   AAC类型 main 0x00  lc 0x01 ssr 0x02   --> 2位
     * sample   采样率,参照getADTSSample                --> 4位
     * priv-bit 私有位,编码设置0,解码忽略                 -->1位
     * channel 通道数量,1声道~5声道,6是5.1声道,7是7.1声道  -->3位
     * orig    编码设置0,解码忽略                        -->1位
     * home    编码设置0,解码忽略                        -->1位
     * 可变头信息
     * bit 编码设置0,解码忽略                        -->1位
     * start 编码设置0,解码忽略                      -->1位
     * length 帧长度,头信息长度+数据帧长度,即 (absent==0?9:7)+audioDataLength  -->13位
     * buffer 0x7FFF,码率可变                         --->11位
     * number 当前帧有多少个数据块+1个原始帧(一个AAC原始帧包含一段时间内1024个采样及相关数据) -->2位   该值为0即表示只有一个aac数据块
     */
    private void addADTSHeader(byte[] packet, int packetLen, int sample, int profile, int channelCount) {
        //头信息 1111 1111 1111 1 00 1 10 0100  0  010 0 0 0 0 size+7  111 1111 1111 00
        //转变为 1111 1111 1111 1001 1001 0000 1000 00(size+7)1 1111 1111 1100
        //        f   f    f     9   5    0     8                f   f     c

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (sample << 2) + (channelCount >> 2));
        packet[3] = (byte) (((channelCount & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * 将采样率转变为adts的采样率
     */
    private int getADTSSample(int sample) {
        int rate = 4;
        switch (sample) {
            case 96000:
                rate = 0;
                break;
            case 88200:
                rate = 1;
                break;
            case 64000:
                rate = 2;
                break;
            case 48000:
                rate = 3;
                break;
            case 44100:
                rate = 4;
                break;
            case 32000:
                rate = 5;
                break;
            case 24000:
                rate = 6;
                break;
            case 22050:
                rate = 7;
                break;
            case 16000:
                rate = 8;
                break;
            case 12000:
                rate = 9;
                break;
            case 11025:
                rate = 10;
                break;
            case 8000:
                rate = 11;
                break;
            case 7350:
                rate = 12;
                break;
        }
        return rate;
    }


}




