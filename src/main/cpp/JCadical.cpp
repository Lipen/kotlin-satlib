/**
 * Copyright © 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

#include <jni.h>

#include <cadical/cadical.hpp>
#include "com_github_lipen_jnisat_JCadical.h"

#define JNI_METHOD(rtype, name) \
    JNIEXPORT rtype JNICALL Java_com_github_lipen_jnisat_JCadical_##name

static inline jlong encode(CaDiCaL::Solver* p) {
    return (jlong) (intptr_t) p;
}

static inline CaDiCaL::Solver* decode(jlong h) {
    return (CaDiCaL::Solver*) (intptr_t) h;
}

JNI_METHOD(jlong, cadical_1create)
  (JNIEnv*, jobject) {
    return encode(new CaDiCaL::Solver);
  }

JNI_METHOD(void, cadical_1delete)
  (JNIEnv*, jobject, jlong p) {
    delete decode(p);
  }

JNI_METHOD(jboolean, cadical_1set)
  (JNIEnv* env, jobject, jlong p, jstring name, jint value) {
    const char* s = env->GetStringUTFChars(name, 0);
    bool b = decode(p)->set(s, value);
    env->ReleaseStringUTFChars(name, s);
    return b;
  }

JNI_METHOD(jboolean, cadical_1set_1long_1option)
  (JNIEnv* env, jobject, jlong p, jstring arg) {
    const char* s = env->GetStringUTFChars(arg, 0);
    bool b = decode(p)->set_long_option(s);
    env->ReleaseStringUTFChars(arg, s);
    return b;
  }

JNI_METHOD(jboolean, cadical_1frozen)
  (JNIEnv*, jobject, jlong p, jint lit) {
    return decode(p)->frozen(lit);
  }

JNI_METHOD(void, cadical_1freeze)
  (JNIEnv*, jobject, jlong p, jint lit) {
    decode(p)->freeze(lit);
  }

JNI_METHOD(void, cadical_1melt)
  (JNIEnv*, jobject, jlong p, jint lit) {
    decode(p)->melt(lit);
  }

JNI_METHOD(jint, cadical_1fixed)
  (JNIEnv*, jobject, jlong p, jint lit) {
    return decode(p)->fixed(lit);
  }

JNI_METHOD(jboolean, cadical_1failed)
  (JNIEnv*, jobject, jlong p, jint lit) {
    return decode(p)->failed(lit);
  }

JNI_METHOD(void, cadical_1optimize)
  (JNIEnv*, jobject, jlong p, jint value) {
    decode(p)->optimize(value);
  }

JNI_METHOD(void, cadical_1simplify)
  (JNIEnv*, jobject, jlong p) {
    decode(p)->simplify();
  }

JNI_METHOD(void, cadical_1terminate)
  (JNIEnv*, jobject, jlong p) {
    decode(p)->terminate();
  }

JNI_METHOD(void, cadical_1add)
  (JNIEnv*, jobject, jlong p, jint lit) {
    decode(p)->add(lit);
  }

JNI_METHOD(void, cadical_1assume)
  (JNIEnv*, jobject, jlong p, jint lit) {
    decode(p)->assume(lit);
  }

JNI_METHOD(void, cadical_1add_1clause)
  (JNIEnv* env, jobject, jlong p, jintArray literals) {
    jsize array_length = env->GetArrayLength(literals);
    CaDiCaL::Solver* solver = decode(p);

    jint* array = env->GetIntArrayElements(literals, 0);
    for (int i = 0; i < array_length; i++) {
        solver->add(array[i]);
    }
    solver->add(0);
    env->ReleaseIntArrayElements(literals, array, 0);
  }

JNI_METHOD(void, cadical_1add_1assumption)
  (JNIEnv* env, jobject, jlong p, jintArray literals) {
    jsize array_length = env->GetArrayLength(literals);
    CaDiCaL::Solver* solver = decode(p);

    jint* array = env->GetIntArrayElements(literals, 0);
    for (int i = 0; i < array_length; i++) {
        solver->assume(array[i]);
    }
    env->ReleaseIntArrayElements(literals, array, 0);
  }

JNI_METHOD(jint, cadical_1solve)
  (JNIEnv*, jobject, jlong p) {
    return decode(p)->solve();
  }

JNI_METHOD(jboolean, cadical_1get_1value)
  (JNIEnv*, jobject, jlong p, jint lit) {
    return decode(p)->val(lit) > 0;
  }

JNI_METHOD(jbooleanArray, cadical_1get_1model)
  (JNIEnv* env, jobject, jlong p) {
    CaDiCaL::Solver* solver = decode(p);
    int size = solver->vars() + 1;
    jbooleanArray result = env->NewBooleanArray(size);
    if (result == NULL) {
        return NULL;
    }
    jboolean* model = new jboolean[size];
    for (int i = 1; i < size; i++) {
        model[i] = solver->val(i) > 0;
    }
    env->SetBooleanArrayRegion(result, 0, size, model);
    delete[] model;
    return result;
  }
