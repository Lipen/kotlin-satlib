package com.github.lipen.satlib.core

import com.github.lipen.satlib.utils.Observable

fun interface AssumptionsProvider : () -> List<Lit>

class AssumptionsObservable : Observable<AssumptionsProvider>() {
    fun collect(): List<Lit> {
        return listeners.flatMap { it() }
    }
}
