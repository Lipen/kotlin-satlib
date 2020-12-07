package com.github.lipen.satlib.utils

fun interface AssumptionsProvider : () -> List<Lit>

class AssumptionsObservable : Observable<AssumptionsProvider>() {
    fun collect(): List<Lit> {
        return listeners.flatMap { it() }
    }
}
