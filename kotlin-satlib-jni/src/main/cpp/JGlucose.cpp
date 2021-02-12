/**
 * Copyright © 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

#include <jni.h>
#include <stdint.h>

#include <glucose/simp/SimpSolver.h>
#include "com_github_lipen_satlib_solver_jni_JGlucose.h"

#define JNI_METHOD(rtype, name) \
    JNIEXPORT rtype JNICALL Java_com_github_lipen_satlib_solver_jni_JGlucose_##name

static inline jlong encode(Glucose::SimpSolver* p) {
    return (jlong) (intptr_t) p;
}

static inline Glucose::SimpSolver* decode(jlong h) {
    return (Glucose::SimpSolver*) (intptr_t) h;
}

static inline int lit2var(int lit) {
    return lit > 0 ? lit - 1 : -lit - 1;
}

JNI_METHOD(jlong, glucose_1ctor)
  (JNIEnv*, jobject) {
    return encode(new Glucose::SimpSolver());
  }

JNI_METHOD(void, glucose_1dtor)
  (JNIEnv*, jobject, jlong handle) {
    delete decode(handle);
  }

JNI_METHOD(jboolean, glucose_1okay)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->okay();
  }

JNI_METHOD(jboolean, glucose_1is_1incremental)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->isIncremental();
  }

JNI_METHOD(void, glucose_1set_1incremental)
  (JNIEnv*, jobject, jlong handle) {
    decode(handle)->setIncrementalMode();
  }

JNI_METHOD(jint, glucose_1nvars)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->nVars();
}

JNI_METHOD(jint, glucose_1nclauses)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->nClauses();
}

JNI_METHOD(jint, glucose_1nlearnts)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->nLearnts();
}

JNI_METHOD(jint, glucose_1decisions)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->decisions;
}

JNI_METHOD(jint, glucose_1propagations)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->propagations;
}

JNI_METHOD(jint, glucose_1conflicts)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->conflicts;
}

JNI_METHOD(jint, glucose_1new_1var)
  (JNIEnv*, jobject, jlong handle, jboolean polarity, jboolean decision) {
    int v = decode(handle)->newVar(polarity, decision);
    return (jint)(1 + v);
}

JNI_METHOD(void, glucose_1set_1polarity)
  (JNIEnv*, jobject, jlong handle, jint lit, jboolean polarity) {
    decode(handle)->setPolarity(lit2var(lit), polarity);
  }

JNI_METHOD(void, glucose_1set_1decision)
  (JNIEnv*, jobject, jlong handle, jint lit, jboolean b) {
    decode(handle)->setDecisionVar(lit2var(lit), b);
  }

JNI_METHOD(void, glucose_1set_1frozen)
  (JNIEnv*, jobject, jlong handle, jint lit, jboolean b) {
    decode(handle)->setFrozen(lit2var(lit), b);
  }

JNI_METHOD(jboolean, glucose_1simplify)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->simplify();
  }

JNI_METHOD(jboolean, glucose_1eliminate)
  (JNIEnv*, jobject, jlong handle, jboolean turn_off_elim) {
    return decode(handle)->eliminate(turn_off_elim);
  }

JNI_METHOD(jboolean, glucose_1is_1eliminated)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    return decode(handle)->isEliminated(lit2var(lit));
  }

JNI_METHOD(void, glucose_1set_1random_1seed)
  (JNIEnv*, jobject, jlong handle, jdouble random_seed) {
    decode(handle)->random_seed = random_seed;
  }

JNI_METHOD(void, glucose_1set_1random_1var_1freq)
  (JNIEnv*, jobject, jlong handle, jdouble random_var_freq) {
    decode(handle)->random_var_freq = random_var_freq;
  }

JNI_METHOD(void, glucose_1set_1rnd_1pol)
  (JNIEnv*, jobject, jlong handle, jboolean rnd_pol) {
    decode(handle)->rnd_pol = rnd_pol;
  }

JNI_METHOD(void, glucose_1set_1rnd_1init_1act)
  (JNIEnv*, jobject, jlong handle, jboolean rnd_init_act) {
    decode(handle)->rnd_init_act = rnd_init_act;
  }

JNI_METHOD(void, glucose_1interrupt)
  (JNIEnv*, jobject, jlong handle) {
    decode(handle)->interrupt();
  }

JNI_METHOD(void, glucose_1clear_1interrupt)
  (JNIEnv*, jobject, jlong handle) {
    decode(handle)->clearInterrupt();
  }

JNI_METHOD(void, glucose_1to_1dimacs)
  (JNIEnv* env, jobject, jlong handle, jstring arg) {
    const char* file = env->GetStringUTFChars(arg, 0);
    decode(handle)->toDimacs(file);
    env->ReleaseStringUTFChars(arg, file);
  }

static inline Glucose::Lit convert(int lit) {
    return Glucose::toLit(lit > 0 ? (lit << 1) - 2 : ((-lit) << 1) - 1);
}

JNI_METHOD(jboolean, glucose_1add_1clause__J)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->addEmptyClause();
  }

JNI_METHOD(jboolean, glucose_1add_1clause__JI)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    return decode(handle)->addClause(convert(lit));
  }

JNI_METHOD(jboolean, glucose_1add_1clause__JII)
  (JNIEnv*, jobject, jlong handle, jint lit1, jint lit2) {
    return decode(handle)->addClause(convert(lit1), convert(lit2));
  }

JNI_METHOD(jboolean, glucose_1add_1clause__JIII)
  (JNIEnv*, jobject, jlong handle, jint lit1, jint lit2, jint lit3) {
    return decode(handle)->addClause(convert(lit1), convert(lit2), convert(lit3));
  }

JNI_METHOD(jboolean, glucose_1add_1clause__J_3I)
  (JNIEnv* env, jobject, jlong handle, jintArray literals) {
    jint len = env->GetArrayLength(literals);
    Glucose::vec<Glucose::Lit> vec(len);

    jint* array = (jint*) env->GetPrimitiveArrayCritical(literals, 0);
    for (jint i = 0; i < len; i++) {
        vec[i] = convert(array[i]);
    }
    env->ReleasePrimitiveArrayCritical(literals, array, 0);

    return decode(handle)->addClause_(vec);
  }

JNI_METHOD(jboolean, glucose_1solve__JZZ)
  (JNIEnv*, jobject, jlong handle, jboolean do_simp, jboolean turn_off_simp) {
    return decode(handle)->solve(do_simp, turn_off_simp);
  }

JNI_METHOD(jboolean, glucose_1solve__J_3IZZ)
  (JNIEnv* env, jobject, jlong handle, jintArray assumptions, jboolean do_simp, jboolean turn_off_simp) {
    jint len = env->GetArrayLength(assumptions);
    Glucose::vec<Glucose::Lit> vec(len);

    jint* p = (jint*) env->GetPrimitiveArrayCritical(assumptions, 0);
    for (jint i = 0; i < len; i++) {
        vec[i] = convert(p[i]);
    }
    env->ReleasePrimitiveArrayCritical(assumptions, p, 0);

    return decode(handle)->solve(vec, do_simp, turn_off_simp);
  }

JNI_METHOD(jbyte, glucose_1get_1value)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    return (jbyte) Glucose::toInt(decode(handle)->modelValue(convert(lit)));
  }

JNI_METHOD(jbooleanArray, glucose_1get_1model)
  (JNIEnv* env, jobject, jlong handle) {
    Glucose::SimpSolver* solver = decode(handle);
    int size = solver->nVars() + 1;
    jbooleanArray result = env->NewBooleanArray(size);
    if (result == NULL) {
        return NULL;
    }
    jboolean* model = new jboolean[size];
    for (int i = 1; i < size; i++) {
        model[i] = solver->modelValue(convert(i)) == Glucose::lbool((uint8_t)0);
    }
    env->SetBooleanArrayRegion(result, 0, size, model);
    delete[] model;
    return result;
}
