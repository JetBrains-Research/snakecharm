package com.jetbrains.snakecharm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl
import com.intellij.python.community.helpersLocator.PythonHelpersLocator
import java.nio.file.Path

/**
 * Test-only workaround for `com.jetbrains.python.pythonHelpersLocator` being unusable in the Gradle
 * test JVM. Two different breakages with one upstream cause, and which one you get depends on the
 * platform:
 *
 * Creating the mock SDK triggers `PyTypeShed`'s lazy init, which calls
 * `PythonHelpersLocator.getHelpersRoots()`. On **2026.1** that iterates every registered locator with
 * no exception guard, and the obfuscated Pro locator's `getRoot()` calls `getPluginDistDirByClass`,
 * which throws `IllegalStateException: .../plugins/python-ce/lib/modules should be lib directory`
 * because the unified Python plugin ships its code as v2 content modules under `lib/modules/` rather
 * than directly under `lib/`. On **2026.2** the EP itself is declared by the
 * `intellij.python.community.helpersLocator` content module, which the flat test classpath never
 * loads, so `getHelpersRoots()` instead dies with "Missing extension point:
 * com.jetbrains.python.pythonHelpersLocator".
 *
 * Both are gradle-test-sandbox artifacts of the same upstream bug — the flattened test classpath means
 * the plugin classes aren't under a `PluginAwareClassLoader`, so the safe branch of
 * `getPluginDistDirByClass` isn't taken
 * ([intellij-platform-gradle-plugin#2183](https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2183),
 * open; the earlier #2070 was closed as a duplicate of it on 2026-09-11, *not* fixed — don't read
 * that closure as a reason to drop this). Runtime is unaffected by either.
 *
 * Kept out of `com.jetbrains.python.PythonMockSdk` -- that file is a vendored copy of a JetBrains
 * class, and every line of ours in it is a line to re-merge the next time it is re-vendored.
 */
object SmkTestPythonHelpersLocatorFix {
    private const val HELPERS_LOCATOR_EP = "com.jetbrains.python.pythonHelpersLocator"
    private const val PRO_HELPERS_LOCATOR_FQN = "com.jetbrains.python.PythonProHelpersLocator"
    private const val HELPERS_PATH_PROPERTY = "idea.python.helpers.path"

    private val LOG = Logger.getInstance(SmkTestPythonHelpersLocatorFix::class.java)

    /** Latched on success only, so an EP that is repopulated later is dealt with again. */
    @Volatile
    private var configured = false

    /** Separate from [configured] so a fruitless retry does not re-log the warning. */
    @Volatile
    private var removalDidNothingWarned = false

    /**
     * Make the helpers-locator EP usable: prune the crashing Pro locator where the EP exists (2026.1),
     * and register the EP outright where it does not (2026.2).
     *
     * Idempotent — safe to call before every SDK creation, and called from `PythonMockSdk.create`,
     * the one point every test path funnels through.
     */
    fun configurePythonHelpersLocator() {
        if (configured) {
            return
        }
        val area = ApplicationManager.getApplication()?.extensionArea ?: return

        val ep = area.getExtensionPointIfRegistered<Any>(HELPERS_LOCATOR_EP)
        if (ep != null) {
            // Removing just this one dynamic extension leaves the community locator (fed by the
            // -Didea.python.helpers.path jvmArg) and the rest of the Pro Python plugin intact, so
            // Python resolution still works. Unlike the community locator the Pro one reads no
            // property, so it cannot simply be pointed at a valid root.
            ep.unregisterExtensions(
                { className, _ ->
                    val isProLocator = className == PRO_HELPERS_LOCATOR_FQN
                    if (isProLocator) {
                        configured = true
                    }
                    !isProLocator
                },
                false,
            )
            if (!configured) {
                warnRemovalDidNothing("no extension named '$PRO_HELPERS_LOCATOR_FQN' is registered on it")
            }
            return
        }

        // `PythonHelpersLocatorDefault` would resolve to the same root -- its `getRoot()` reads the
        // helpers property before it ever tries the plugin dist dir -- but it is declared by the same
        // content module that never loads, so register our own rather than reach for an internal class
        // the flat test classpath only happens to expose.
        val helpersPath = requireNotNull(System.getProperty(HELPERS_PATH_PROPERTY)) {
            "'$HELPERS_PATH_PROPERTY' is not set; see the test task's jvmArgumentProviders in build.gradle.kts"
        }
        (area as ExtensionsAreaImpl).registerExtensionPoint(
            HELPERS_LOCATOR_EP,
            PythonHelpersLocator::class.java.name,
            ExtensionPoint.Kind.INTERFACE,
            true,
        )
        area.getExtensionPoint<PythonHelpersLocator>(HELPERS_LOCATOR_EP).registerExtension(
            object : PythonHelpersLocator {
                override fun getRoot(): Path = Path.of(helpersPath)
            },
            ApplicationManager.getApplication(),
        )
        configured = true
    }

    /**
     * The Pro-locator anchors are plugin internals with no compile-time check, so a rename in a
     * platform update turns the removal into a silent no-op. Say so out loud: the opaque
     * `... should be lib directory` crash it guards against takes the whole suite down without naming
     * a cause, and this line sits in the log right before it.
     *
     * A warning rather than a failure because the locator legitimately does not exist on every
     * platform (e.g. `platformType = PC` / IDEA + community PythonCore, which never had it) — which is
     * also why it is logged only once: on such a platform nothing is ever removed, so the caller keeps
     * retrying, once per scenario.
     */
    private fun warnRemovalDidNothing(reason: String) {
        if (!removalDidNothingWarned) {
            removalDidNothingWarned = true
            LOG.warn(
                "PythonProHelpersLocator was not unregistered: $reason. Harmless if this platform has no " +
                        "Pro Python plugin; otherwise the class or EP name changed and the removal is a " +
                        "no-op -- expect PyTypeShed init to fail with 'IllegalStateException: " +
                        ".../lib/modules should be lib directory' across the whole suite. " +
                        "Fix the names in SmkTestPythonHelpersLocatorFix."
            )
        }
    }
}
