package com.github.lipen.satlib.util

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

object MyDisplayNameGenerator : DisplayNameGenerator.Standard() {
    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method?): String {
        return testMethod!!.name
    }
}
