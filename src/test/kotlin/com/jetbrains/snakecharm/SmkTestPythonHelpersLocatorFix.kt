package com.jetbrains.snakecharm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger

/**
 * Test-only workaround for the Pro Python helpers locator crashing under the Gradle test sandbox.
 *
 * Kept out of `com.jetbrains.python.PythonMockSdk` -- that file is a vendored copy of a JetBrains
 * class, and every line of ours in it is a line to re-merge the next time it is re-vendored.
 */
object SmkTestPythonHelpersLocatorFix {
    private const val HELPERS_LOCATOR_EP = "com.jetbrains.python.pythonHelpersLocator"
    private const val PRO_HELPERS_LOCATOR_FQN = "com.jetbrains.python.PythonProHelpersLocator"

    private val LOG = Logger.getInstance(SmkTestPythonHelpersLocatorFix::class.java)

    /** Latched on a *successful* removal only, so a repopulated EP is dealt with again. */
    @Volatile
    private var proHelpersLocatorRemoved = false

    /** Separate from [proHelpersLocatorRemoved] so a fruitless retry does not re-log the warning. */
    @Volatile
    private var removalDidNothingWarned = false

    /**
     * Unregister the Pro `PythonProHelpersLocator` from the `com.jetbrains.python.pythonHelpersLocator`
     * extension point (test JVM only).
     *
     * Creating the mock SDK triggers `PyTypeShed`'s lazy init, which calls
     * `PythonHelpersLocator.getHelpersRoots()` — that iterates every registered locator with no
     * exception guard. The obfuscated Pro locator's `getRoot()` calls `getPluginDistDirByClass`, which
     * throws `IllegalStateException: .../plugins/python-ce/lib/modules should be lib directory` because the
     * unified 2026.1 Python plugin ships its code as v2 content modules under `lib/modules/` rather than
     * directly under `lib/`. That is purely a gradle-test-sandbox artifact (the flattened test classpath
     * means the plugin classes aren't under a `PluginAwareClassLoader`, so the safe branch of
     * `getPluginDistDirByClass` isn't taken; upstream
     * https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2183, open -- the issue this
     * used to cite, #2070, was closed on 2026-09-11 as a *duplicate* of it and not as fixed, so don't
     * read that closure as a reason to drop this). Unlike the
     * community locator it reads no `idea.python.helpers.path` property, so it can't be pointed at a
     * valid root. Removing just this one dynamic EP leaves the community locator (fed by the
     * `-Didea.python.helpers.path` jvmArg) and the rest of the Pro Python plugin intact, so Python
     * resolution still works. Idempotent — safe to call before every SDK creation. Runtime is unaffected.
     *
     * Called from `PythonMockSdk.create`, the one point every test path funnels through.
     */
    fun removeCrashingProHelpersLocator() {
        if (proHelpersLocatorRemoved) {
            return
        }
        val ep = ApplicationManager.getApplication()?.extensionArea
            ?.getExtensionPointIfRegistered<Any>(HELPERS_LOCATOR_EP)
        if (ep == null) {
            warnRemovalDidNothing("extension point '$HELPERS_LOCATOR_EP' is not registered")
            return
        }
        ep.unregisterExtensions(
            { className, _ ->
                val isProLocator = className == PRO_HELPERS_LOCATOR_FQN
                if (isProLocator) {
                    proHelpersLocatorRemoved = true
                }
                !isProLocator
            },
            false,
        )
        if (!proHelpersLocatorRemoved) {
            warnRemovalDidNothing("no extension named '$PRO_HELPERS_LOCATOR_FQN' is registered on it")
        }
    }

    /**
     * Both anchors of [removeCrashingProHelpersLocator] are Pro-plugin internals with no compile-time
     * check, so a rename in a platform update turns the removal into a silent no-op. Say so out loud:
     * the opaque `... should be lib directory` crash it guards against takes the whole suite down
     * without naming a cause, and this line sits in the log right before it.
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
