/**
 * Copyright (c) 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
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
    CaDiCaL::Solver *solver = decode(p);
    jint* array = env->GetIntArrayElements(literals, 0);
    for (int i = 0; i < array_length; i++) {
        solver->add(array[i]);
    }
    solver->add(0);
    env->ReleaseIntArrayElements(literals, array, 0);
  }

JNI_METHOD(jint, cadical_1solve__J)
  (JNIEnv*, jobject, jlong p) {
    return decode(p)->solve();
  }

JNI_METHOD(jint, cadical_1solve__JI)
  (JNIEnv*, jobject, jlong p, jint lit) {
    CaDiCaL::Solver* solver = decode(p);
    solver->assume(lit);
    return solver->solve();
  }

JNI_METHOD(jint, cadical_1solve__JII)
  (JNIEnv*, jobject, jlong p, jint lit1, jint lit2) {
    CaDiCaL::Solver* solver = decode(p);
    solver->assume(lit1);
    solver->assume(lit2);
    return solver->solve();
  }

JNI_METHOD(jint, cadical_1solve__JIII)
  (JNIEnv*, jobject, jlong p, jint lit1, jint lit2, jint lit3) {
    CaDiCaL::Solver* solver = decode(p);
    solver->assume(lit1);
    solver->assume(lit2);
    solver->assume(lit3);
    return solver->solve();
  }

JNI_METHOD(jint, cadical_1solve__J_3I)
  (JNIEnv* env, jobject, jlong p, jintArray assumptions) {
    jsize array_length = env->GetArrayLength(assumptions);
    CaDiCaL::Solver* solver = decode(p);
    jint * array = env->GetIntArrayElements(assumptions, 0);
    for (int i = 0; i < array_length; i++) {
        solver->assume(array[i]);
    }
    env->ReleaseIntArrayElements(assumptions, array, 0);
    return solver->solve();
  }

JNI_METHOD(jint, cadical_1get_1value)
  (JNIEnv*, jobject, jlong p, jint lit) {
    return decode(p)->val(lit);
  }

JNI_METHOD(jintArray, cadical_1get_1model)
  (JNIEnv* env, jobject, jlong p) {
    CaDiCaL::Solver * solver = decode(p);
    int size = solver->vars() + 1;
    jintArray result = env->NewIntArray(size);
    if (result == NULL) {
        return NULL;
    }
    jint model[size];
    for (int i = 1; i < size; i++) {
        model[i] = solver->val(i);
    }
    env->SetIntArrayRegion(result, 0, size, model);
    return result;
  }
