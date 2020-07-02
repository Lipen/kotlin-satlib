package com.github.lipen.satlib.solver

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

internal object MyDisplayNameGenerator : DisplayNameGenerator.Standard() {
    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method?): String {
        return testMethod!!.name
    }
}
