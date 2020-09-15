#include <jni.h>

extern "C" JNIEXPORT jdouble JNICALL Java_uzh_scenere_helpers_CppHelper_evaluateAverage(JNIEnv *env,
                                                                                        jobject,
                                                                                        jobjectArray a) {
    jdouble totalAverage = 0;
    jsize len = (*env).GetArrayLength(a);
    for (int i = 0; i < len; i++) {
        jintArray innerArray = static_cast<jintArray>((*env).GetObjectArrayElement(a, i));
        jint *inner = (*env).GetIntArrayElements(innerArray, nullptr);
        jsize innerLen = (*env).GetArrayLength(innerArray);
        for (int j = 0; j < innerLen; j++) {
           totalAverage += static_cast<jdouble>(inner[j])/(len*innerLen);
        }
    }

    return totalAverage;
}