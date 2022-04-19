#include <jni.h>
#include <string>
#include "lame/lame.h"

#include<android/log.h>

#define LOG_TAG "lee_lame"
#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_lee_lame_mp3_Mp3Util_getLameVersion(JNIEnv *env, jclass) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
    return env->NewStringUTF(get_lame_version());
}
}

/**
 * 把java中的jstring的类型转化成一个c语言中的char 字符串
 * @return char数组的首地址
 */
char *JString2CStr(JNIEnv *env, jstring str) {
    char *rtn = nullptr;
    jclass clsString = env->FindClass("java/lang/String");
    jstring code = env->NewStringUTF("UTF-8");
    jmethodID mid = env->GetMethodID(clsString, "getBytes", "(Ljava/lang/String;)[B");
    auto byteArray = (jbyteArray) env->CallObjectMethod(str, mid, code);
    jsize length = env->GetArrayLength(byteArray);
    jbyte *ba = env->GetByteArrayElements(byteArray, JNI_FALSE);
    if (length > 0) {
        rtn = (char *) malloc(length + 1); //"\0"
        memcpy(rtn, ba, length);
        rtn[length] = 0;
    }
    env->ReleaseByteArrayElements(byteArray, ba, 0);
    return rtn;
}


void publishJavaProgress(JNIEnv *env, jfloat progress) {
    jclass clazz = env->FindClass("com/lee/lame/mp3/Mp3Util");
    if (!clazz) {
        LOG_I("can't find clazz");
    }
    jmethodID methodId = env->GetStaticMethodID(clazz, "convertProgress", "(F)V");
    if (!methodId) {
        LOG_I("can't find methodId");
    }
    env->CallStaticVoidMethod(clazz, methodId, progress);
    env->DeleteLocalRef(clazz);
}

void publishJavaEnd(JNIEnv *env) {
    jclass clazz = env->FindClass("com/lee/lame/mp3/Mp3Util");
    if (!clazz) {
        LOG_I("can't find clazz");
    }
    jmethodID methodId = env->GetStaticMethodID(clazz, "convertEnd", "()V");
    if (!methodId) {
        LOG_I("can't find methodId");
    }
    env->CallStaticVoidMethod(clazz, methodId);
    env->DeleteLocalRef(clazz);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lee_lame_mp3_Mp3Util_convert2Mp3(JNIEnv *env, jclass, jstring in_path, jint in_sample,
                                          jint in_channel, jstring mp3_path, jint out_sample,
                                          jboolean is_wav, jint quality) {
    char *cWav = JString2CStr(env, in_path);
    char *cMp3 = JString2CStr(env, mp3_path);
    LOG_I("inPath = %s", cWav);
    LOG_I("mp3 = %s", cMp3);

    short int wav_buffer[8192 * 2];
    unsigned char mp3_buffer[8192];

    FILE *fWav = fopen(cWav, "rb");
    FILE *fMp3 = fopen(cMp3, "wb+");

    fseek(fWav, 0, SEEK_END);
    int fileSize = ftell(fWav);


    fseek(fWav, 0, SEEK_SET);
    //配置lame
    lame_t lame = lame_init();
    lame_set_in_samplerate(lame, in_sample);
    lame_set_num_channels(lame, in_channel);
    lame_set_out_samplerate(lame, out_sample);
    lame_set_quality(lame, quality);
    lame_set_VBR(lame, vbr_default); //使用VBR模式
    lame_init_params(lame);
    LOG_I("lame init");

    int read;
    int write;
    int total = 0;
    double progress;
    size_t size = sizeof(short int) * 2;
    if (is_wav) {
        fread(wav_buffer, size, 44, fWav); //wav跳过头部44个字节
        total += 44;
    }
    read = fread(wav_buffer, size, 8192, fWav);
    while (read != 0) {
        total += read;
        progress = total * size * 100.0 / fileSize;
        write = lame_encode_buffer_interleaved(lame, wav_buffer, read, mp3_buffer, 8192);
        publishJavaProgress(env, progress);
        //把转化后的mp3数据写到文件里
        if (write != 0) {
            fwrite(mp3_buffer, sizeof(unsigned char), write, fMp3);
        } else {
            LOG_D("converting write 0");
        }
        read = fread(wav_buffer, sizeof(short int) * 2, 8192, fWav);
    }


    //刷新缓冲区
    int flush = lame_encode_flush(lame, mp3_buffer, 8192);
    //写入数据,不写入会导致音频提前50ms结束
    fwrite(mp3_buffer, sizeof(unsigned char), flush, fMp3);
    //写入VBR头,不写入会导致音频开始的时间节点后置50ms左右
    lame_mp3_tags_fid(lame, fMp3);
    LOG_I("convert  finish");
    lame_close(lame);
    fclose(fWav);
    fclose(fMp3);
    LOG_I("lame close");
    publishJavaEnd(env);
}


/**
 * 全局转换实例,用于实时转换
 */
static lame_global_flags *lgf = nullptr;
extern "C"
JNIEXPORT void JNICALL
Java_com_lee_lame_mp3_Mp3Util_initMp3Encoder(JNIEnv *env, jclass, jint in_sample,
                                             jint in_channel, jint out_sample, jint quality) {
    if (lgf != nullptr) {
        lame_close(lgf);
        lgf = nullptr;
    }
    lgf = lame_init();

    lame_set_in_samplerate(lgf, in_sample);
    lame_set_num_channels(lgf, in_channel);
    lame_set_out_samplerate(lgf, out_sample);
    lame_set_quality(lgf, quality);
    lame_set_VBR(lgf, vbr_default); //使用VBR模式

    lame_init_params(lgf);

    LOG_I("lame init");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_lee_lame_mp3_Mp3Util_encodeMp3(JNIEnv *env, jclass, jshortArray pcm_left,
                                        jshortArray pcm_right, jint size, jbyteArray mp3_buffer) {
    jshort *j_buffer_l = env->GetShortArrayElements(pcm_left, nullptr);

    jshort *j_buffer_r = env->GetShortArrayElements(pcm_right, nullptr);

    const jsize mp3buffer_size = env->GetArrayLength(mp3_buffer);

    jbyte *j_mp3buffer = env->GetByteArrayElements(mp3_buffer, nullptr);

    int result = lame_encode_buffer(lgf, j_buffer_l, j_buffer_r,
                                    size, reinterpret_cast<unsigned char *>(j_mp3buffer),
                                    mp3buffer_size);

    env->ReleaseShortArrayElements(pcm_left, j_buffer_l, 0);
    env->ReleaseShortArrayElements(pcm_right, j_buffer_r, 0);
    env->ReleaseByteArrayElements(mp3_buffer, j_mp3buffer, 0);

    return result;
}

extern "C"
JNIEXPORT int JNICALL
Java_com_lee_lame_mp3_Mp3Util_flushMp3(JNIEnv *env, jclass, jbyteArray mp3_buffer,
                                       jstring mp3_path) {


    const jsize mp3buf_size = env->GetArrayLength(mp3_buffer);
    jbyte *j_mp3buf = env->GetByteArrayElements(mp3_buffer, nullptr);

    int flush = lame_encode_flush(lgf, reinterpret_cast<unsigned char *>(j_mp3buf), mp3buf_size);
    LOG_D("lame flush %d", flush);

    char *cMp3 = JString2CStr(env, mp3_path);
    FILE *fMp3 = fopen(cMp3, "rb+");

    //写入VBR头,不写入会导致音频开始的时间节点后置50ms左右
    lame_mp3_tags_fid(lgf, fMp3);

    env->ReleaseByteArrayElements(mp3_buffer, j_mp3buf, 0);
    fclose(fMp3);
    LOG_I("lame flush over");
    return flush;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lee_lame_mp3_Mp3Util_closeMp3Encoder(JNIEnv *env, jclass) {
    if (lgf != nullptr) {
        lame_close(lgf);
        lgf = nullptr;
    }
    LOG_I("lame close");
}