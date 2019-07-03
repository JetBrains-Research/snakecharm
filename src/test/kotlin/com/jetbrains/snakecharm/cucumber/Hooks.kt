package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInspection.ex.InspectionProfileImpl
import cucumber.api.java.After

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-29
 */
class Hooks {

    @After(order = 1)
    @Throws(Throwable::class)
    fun cleanup() {
        InspectionProfileImpl.INIT_INSPECTIONS = false
        SnakemakeWorld.myFixture?.tearDown()
    }
//
//    @After(order = 0)
//    @Throws(Throwable::class)
//    fun cleanMyWorld() {
//        for (field in SnakemakeWorld::class.java.declaredFields) {
//            if (field.type == Boolean::class.javaPrimitiveType) {
//                field.set(null, false)
//            } else {
//                field.set(null, null)
//            }
//        }
//    }
}