/**
 * Copyright © 2020, Darya Grechishkina, Konstantin Chukharev, ITMO University
 */

#include <jni.h>
#include <vector>
#include <algorithm>

#include <cryptominisat5/cryptominisat.h>

#define JNI_METHOD(rtype, name) \
    JNIEXPORT rtype JNICALL Java_com_github_lipen_satlib_jni_JCryptoMiniSat_##name

static inline jlong encode(CMSat::SATSolver* p) {
	return static_cast<jlong>(reinterpret_cast<intptr_t>(p));
}

static inline CMSat::SATSolver* decode(jlong h) {
	return reinterpret_cast<CMSat::SATSolver*>(static_cast<intptr_t>(h));
}

static inline int correctReturnValue(const CMSat::lbool ret) {
    if (ret == CMSat::l_True) {
        return 10;
    } else if (ret == CMSat::l_False) {
        return 20;
    } else if (ret == CMSat::l_Undef) {
        return 0;
    } else {
        exit(-1);
    }
}

static inline CMSat::Lit toLit(int lit) {
    return CMSat::Lit(std::abs(lit) - 1, lit < 0);
}

static std::vector<CMSat::Lit> to_literals_vector(JNIEnv* env, jintArray literals) {
    jsize array_length = env->GetArrayLength(literals);
    std::vector<CMSat::Lit> clause;
    clause.reserve(array_length);
    jint* array = env->GetIntArrayElements(literals, 0);
    for (int i = 0; i < array_length; i++) {
        clause.push_back(toLit(array[i]));
    }
    env->ReleaseIntArrayElements(literals, array, 0);
    std::sort(clause.begin(), clause.end());
    return clause;
}

#ifdef __cplusplus
extern "C" {
#endif

JNI_METHOD(jlong, cms_1create)
  (JNIEnv*, jobject) {
    return encode(new CMSat::SATSolver);
  }

JNI_METHOD(void, cms_1delete)
  (JNIEnv*, jobject, jlong p) {
    delete decode(p);
  }

JNI_METHOD(void, cms_1interrupt)
  (JNIEnv * env, jobject, jlong p) {
    decode(p)->interrupt_asap();
  }

JNI_METHOD(void, cms_1new_1var)
  (JNIEnv*, jobject, jlong p) {
    decode(p)->new_var();
  }

JNI_METHOD(jint, cms_1nvars)
  (JNIEnv*, jobject, jlong p) {
    return decode(p)->nVars();
  }

JNI_METHOD(void, cms_1add_1clause)
  (JNIEnv* env, jobject, jlong p, jintArray literals) {
    auto lits = to_literals_vector(env, literals);
    decode(p)->add_clause(lits);
  }

JNI_METHOD(jint, cms_1solve__J)
  (JNIEnv*, jobject, jlong p) {
    return correctReturnValue(decode(p)->solve());
  }

JNI_METHOD(jint, cms_1solve__J_3I)
  (JNIEnv* env, jobject, jlong p, jintArray assumptions) {
    auto lits = to_literals_vector(env, assumptions);
    return correctReturnValue(decode(p)->solve(&lits));
  }

JNI_METHOD(jint, cms_1simplify__J)
  (JNIEnv*, jobject, jlong p) {
    return correctReturnValue(decode(p)->simplify());
  }

JNI_METHOD(jint, cms_1simplify__J_3I)
  (JNIEnv* env, jobject, jlong p, jintArray assumptions) {
    auto lits = to_literals_vector(env, assumptions);
    return correctReturnValue(decode(p)->simplify(&lits));
  }

JNI_METHOD(jbyte, cms_1get_1value)
  (JNIEnv*, jobject, jlong p, jint v) {
    // `v` is a variable, must be > 0
    return (jbyte) decode(p)->get_model()[v - 1].getValue();
  }

JNI_METHOD(jbooleanArray, cms_1get_1model)
  (JNIEnv* env, jobject, jlong p) {
    std::vector<CMSat::lbool> model = decode(p)->get_model();
    int size = model.size();
    jbooleanArray result = env->NewBooleanArray(size);
    if (result == NULL) {
        return NULL;
    }
    jboolean* literals = new jboolean[size];
    for (int i = 0; i < size; i++) {
        literals[i] = (model[i] == CMSat::l_True);
    }
    env->SetBooleanArrayRegion(result, 0, size, literals);
    delete[] literals;
    return result;
  }

JNI_METHOD(void, cms_1set_1num_1threads)
  (JNIEnv*, jobject, jlong p, jint n) {
    decode(p)->set_num_threads(n);
  }

JNI_METHOD(void, cms_1set_1max_1time)
  (JNIEnv*, jobject, jlong p, jdouble time) {
    decode(p)->set_max_time(time);
  }

JNI_METHOD(void, cms_1set_1timeout_1all_1calls)
  (JNIEnv*, jobject, jlong p, jdouble time) {
    decode(p)->set_timeout_all_calls(time);
  }

JNI_METHOD(void, cms_1set_1default_1polarity)
  (JNIEnv*, jobject, jlong p, jboolean polarity) {
    decode(p)->set_default_polarity(polarity);
  }

JNI_METHOD(void, cms_1no_1simplify)
  (JNIEnv*, jobject, jlong p) {
    decode(p)->set_no_simplify();
  }

JNI_METHOD(void, cms_1no_1simplify_1at_1startup)
  (JNIEnv*, jobject, jlong p) {
    decode(p)->set_no_simplify_at_startup();
  }

#ifdef __cplusplus
}
#endif
