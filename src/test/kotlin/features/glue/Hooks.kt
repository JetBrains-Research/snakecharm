package features.glue

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.util.Disposer
import com.intellij.util.ReflectionUtil
import com.intellij.util.ui.UIUtil
import cucumber.api.java.After
import cucumber.api.java.Before
import java.lang.reflect.Modifier
import java.util.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-29
 */
class Hooks {
    @Before
    fun initParamdefs() {
        // todo: most likely we should remove this call
        IdeaTestApplication.getInstance()
    }

    @After(order = 1)
    @Throws(Throwable::class)
    fun cleanup() {
        //SeveritiesProvider.EP_NAME.getPoint(null).unregisterExtension(FindUsagesSteps.SEVERITIES_PROVIDER)
        InspectionProfileImpl.INIT_INSPECTIONS = false

        SnakemakeWorld.myFixture?.tearDown()
        SnakemakeWorld.myTestRootDisposable?.let { Disposer.dispose(it) }
        cleanupSwingDataStructures()
        Disposer.setDebugMode(true)
        UIUtil.removeLeakingAppleListeners()
        //TODO UsefulTestCase.waitForAppLeakingThreads(10, TimeUnit.SECONDS)
    }

    @After(order = 0)
       @Throws(Throwable::class)
       fun cleanupMyWorld() {
        for (field in SnakemakeWorld::class.java.declaredFields) {
            if (!Modifier.isPublic(field.modifiers)) {
                System.err.println("Cannot cleanup SnakemakeWorld, field isn't public: ${field.name}")
                System.exit(1)
            }
            if (field.name == "INSTANCE") {
                // skip kotlin object instance field
                continue
            }
            if (field.type == Boolean::class.javaPrimitiveType) {
                field.set(null, false)
            } else {
                field.set(null, null)
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