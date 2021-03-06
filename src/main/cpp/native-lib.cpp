#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_bankaccounts_accounts_getAPIkey(JNIEnv *env, jobject object) {
    std::string API_KEY = "8>BDgyhnJm83sBDeDnhI:74F";
    return env->NewStringUTF(API_KEY.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_bankaccounts_accounts_getEncryptionKey(JNIEnv *env, jobject thiz) {
    std::string ENCRYPTION_KEY = "cbjziozc49812qsjifgb51988";
    return env->NewStringUTF(ENCRYPTION_KEY.c_str());
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_bankaccounts_MainActivity_getHashedPwd(JNIEnv *env, jobject thiz) {
    std::string MASTERKEY = ";ruw9J=puB>3tAv:vg;y<:8FDJkui?HA=6FIFeD<>zkh:EGM:u;:Dm67oCKnDi;I";
    return env->NewStringUTF(MASTERKEY.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_bankaccounts_accounts_getUserFile(JNIEnv *env, jobject thiz) {
    std::string userFile = "w\u0081v\u0086{|D>Btyq|";
    return env->NewStringUTF(userFile.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_bankaccounts_accounts_getAccountFile(JNIEnv *env, jobject thiz) {
    std::string accountFile = "cqt\u0083K\u0081L~}vk0x\u0084\u0083D";
    return env->NewStringUTF(accountFile.c_str());
}