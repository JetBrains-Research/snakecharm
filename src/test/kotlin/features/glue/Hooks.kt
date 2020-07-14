package features.glue

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestApplicationManager
import io.cucumber.java.After
import io.cucumber.java.Before
import java.lang.reflect.Modifier
import kotlin.system.exitProcess

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-29
 */
class Hooks {
    @Before
    fun initParamdefs() {
        // todo: most likely we should remove this call
        TestApplicationManager.getInstance()
    }

    @After(order = 1)
    @Throws(Throwable::class)
    fun cleanup() {
        //SeveritiesProvider.EP_NAME.getPoint(null).unregisterExtension(FindUsagesSteps.SEVERITIES_PROVIDER)
        InspectionProfileImpl.INIT_INSPECTIONS = false

        SnakemakeWorld.myFixture?.tearDown()
        SnakemakeWorld.myTestRootDisposable?.let { Disposer.dispose(it) }
    }

    @After(order = 0)
    @Throws(Throwable::class)
    fun cleanupMyWorld() {
        val inspectionProblemsCounts = SnakemakeWorld.myInspectionProblemsCounts

        for (field in SnakemakeWorld::class.java.declaredFields) {
            if (!Modifier.isPublic(field.modifiers)) {
                System.err.println("Cannot cleanup SnakemakeWorld, field isn't public: ${field.name}")
                exitProcess(1)
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

        require(inspectionProblemsCounts == null) {
            "You have steps that defines expected inspection problems (${inspectionProblemsCounts!!.entries}), " +
                    "but step that compares is with actual problems in file. Please add step: 'When I check highlighting <..>s'"
        }
    }
}