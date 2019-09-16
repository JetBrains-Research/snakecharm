package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.openapi.util.Disposer
import com.intellij.util.ReflectionUtil
import com.intellij.util.ui.UIUtil
import cucumber.api.java.After
import java.lang.reflect.Modifier
import java.util.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-29
 */
class Hooks {

    @After(order = 1)
    @Throws(Throwable::class)
    fun cleanup() {
        //SeveritiesProvider.EP_NAME.getPoint(null).unregisterExtension(FindUsagesSteps.SEVERITIES_PROVIDER)
        InspectionProfileImpl.INIT_INSPECTIONS = false
        SnakemakeWorld.myFixture?.tearDown()

        Disposer.dispose(SnakemakeWorld.myTestRootDisposable)
        cleanupSwingDataStructures()
        Disposer.setDebugMode(true)
        UIUtil.removeLeakingAppleListeners()
        //TODO UsefulTestCase.waitForAppLeakingThreads(10, TimeUnit.SECONDS)

        for (field in SnakemakeWorld::class.java.declaredFields) {
            if (field.modifiers == Modifier.PUBLIC) {
                if (field.type == Boolean::class.javaPrimitiveType) {
                    field.set(null, false)
                } else {
                    field.set(null, null)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun cleanupSwingDataStructures() {
        val manager = ReflectionUtil.getDeclaredMethod(Class.forName("javax.swing.KeyboardManager"), "getCurrentManager")!!.invoke(null)
        val componentKeyStrokeMap = ReflectionUtil.getField(manager.javaClass, manager, Hashtable::class.java, "componentKeyStrokeMap")
        componentKeyStrokeMap.clear()
        val containerMap = ReflectionUtil.getField(manager.javaClass, manager, Hashtable::class.java, "containerMap")
        containerMap.clear()
    }
}