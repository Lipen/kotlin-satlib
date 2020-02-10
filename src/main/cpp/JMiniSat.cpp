/**
 * Copyright (c) 2016, Miklos Maroti, University of Szeged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

#include <jni.h>
#include <stdint.h>

#include <minisat/simp/SimpSolver.h>
#include "com_github_lipen_jnisat_JMiniSat.h"

#define JNI_METHOD(rtype, name) \
    JNIEXPORT rtype JNICALL Java_com_github_lipen_jnisat_JMiniSat_##name

static inline jlong encode(Minisat::SimpSolver* p) {
	return (jlong) (intptr_t) p;
}

static inline Minisat::SimpSolver* decode(jlong h) {
	return (Minisat::SimpSolver*) (intptr_t) h;
}

JNI_METHOD(jlong, minisat_1ctor)(JNIEnv *env, jobject thiz) {
	return encode(new Minisat::SimpSolver());
}

JNI_METHOD(void, minisat_1dtor)(JNIEnv *env, jobject thiz,
        jlong handle) {
	delete decode(handle);
}

JNI_METHOD(jint, minisat_1new_1var)(JNIEnv *env, jobject thiz,
         jlong handle, jbyte polarity) {
	int v = decode(handle)->newVar(Minisat::lbool((uint8_t) polarity));
	return (jint)(1 + v);
}

JNI_METHOD(void, minisat_1set_1decision_1var)(JNIEnv *env, jobject thiz,
		jlong handle, jint lit, jboolean value) {
	int v = lit > 0 ? lit - 1 : -lit - 1;
	decode(handle)->setDecisionVar(v, value);
}

JNI_METHOD(void, minisat_1set_1frozen)(JNIEnv *env, jobject thiz,
        jlong handle, jint lit, jboolean value) {
	int v = lit > 0 ? lit - 1 : -lit - 1;
	decode(handle)->setFrozen(v, value);
}

static inline Minisat::Lit convert(int lit) {
	return Minisat::toLit(lit > 0 ? (lit << 1) - 2 : ((-lit) << 1) - 1);
}

JNI_METHOD(jboolean, minisat_1add_1clause__JI)(JNIEnv *env, jobject thiz,
		jlong handle, jint lit) {
	return decode(handle)->addClause(convert(lit));
}

JNI_METHOD(jboolean, minisat_1add_1clause__JII)(JNIEnv *env, jobject thiz,
		jlong handle, jint lit1, jint lit2) {
	return decode(handle)->addClause(convert(lit1), convert(lit2));
}

JNI_METHOD(jboolean, minisat_1add_1clause__JIII)(JNIEnv *env, jobject thiz,
		jlong handle, jint lit1, jint lit2, jint lit3) {
	return decode(handle)->addClause(convert(lit1), convert(lit2), convert(lit3));
}

JNI_METHOD(jboolean, minisat_1add_1clause__J_3I)(JNIEnv *env, jobject thiz,
		jlong handle, jintArray lits) {
	jint len = env->GetArrayLength(lits);
	Minisat::vec < Minisat::Lit > vec(len);

	jint *p = (jint*) env->GetPrimitiveArrayCritical(lits, 0);
	for (jint i = 0; i < len; i++)
		vec[i] = convert(p[i]);
	env->ReleasePrimitiveArrayCritical(lits, p, 0);

	return decode(handle)->addClause_(vec);
}

JNI_METHOD(jboolean, minisat_1solve)(JNIEnv *env, jobject thiz,
        jlong handle, jboolean simplify, jboolean turnoff) {
	return decode(handle)->solve((bool) simplify, (bool) turnoff);
}

JNI_METHOD(jboolean, minisat_1simplify)(JNIEnv *env, jobject thiz,
        jlong handle) {
	return decode(handle)->simplify();
}

JNI_METHOD(jboolean, minisat_1eliminate)(JNIEnv *env, jobject thiz,
        jlong handle, jboolean turnoff) {
	return decode(handle)->eliminate((bool) turnoff);
}

JNI_METHOD(jboolean, minisat_1is_1eliminated)(JNIEnv *env, jobject thiz,
		 jlong handle, jint lit) {
	int v = lit > 0 ? lit - 1 : -lit - 1;
	return decode(handle)->isEliminated(v);
}

JNI_METHOD(jboolean, minisat_1okay)(JNIEnv *env, jobject thiz,
        jlong handle) {
	return decode(handle)->okay();
}

JNI_METHOD(jbyte, minisat_1model_1value)(JNIEnv *env, jobject thiz,
        jlong handle, jint lit) {
	return (jbyte) Minisat::toInt(decode(handle)->modelValue(convert(lit)));
}
