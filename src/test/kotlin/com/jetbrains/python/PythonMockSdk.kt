/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.jetbrains.python.codeInsight.typing.PyTypeShed.findAllRootsForLanguageLevel
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType.MOCK_PY_MARKER_KEY
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.VirtualEnvSdkFlavor
import java.io.File


/**
 * We cannot re-use PythonMockSdk because api not available in Platform artifacts
 *
 * @author yole
 */
object PythonMockSdk {
    fun create(
        testDataRoot: String,
        level: LanguageLevel = LanguageLevel.getLatest(),
        sdkNameSuffix: String = "",
        vararg additionalRoots: VirtualFile
    ): Sdk {
        removeCrashingProHelpersLocator()
        return create(
            "Mock ${PyNames.PYTHON_SDK_ID_NAME} ${level.toPythonVersion()}$sdkNameSuffix",
            "$testDataRoot/MockSdk${level.toPythonVersion()}",
            PyMockSdkType(level),
            level,
            *additionalRoots
        )
    }

    private fun create(
        sdkName: String = "MockSdk",
         mockSdkPath: String,
         sdkType: SdkTypeId,
         level: LanguageLevel,
        vararg additionalRoots:  VirtualFile
    ): Sdk {
        val roots = MultiMap.create<OrderRootType, VirtualFile>()
        roots.putValues(OrderRootType.CLASSES, createRoots(mockSdkPath, level))
        roots.putValues(OrderRootType.CLASSES, listOf(*additionalRoots))

        val sdk = ProjectJdkTable.getInstance().createSdk(sdkName, sdkType)
        val sdkModificator = sdk.sdkModificator
        sdkModificator.homePath = "$mockSdkPath/bin/python${level.toPythonVersion()}"
        sdkModificator.sdkAdditionalData =
            PythonSdkAdditionalData(PyFlavorAndData(PyFlavorData.Empty, VirtualEnvSdkFlavor.getInstance()))
        sdkModificator.setVersionString(toVersionString(level))

        createRoots(mockSdkPath, level).forEach { vFile: VirtualFile? ->
            sdkModificator.addRoot(
                vFile!!, OrderRootType.CLASSES
            )
        }

        additionalRoots.forEach { vFile ->
            sdkModificator.addRoot(vFile, OrderRootType.CLASSES)
        }

        val application: Application = ApplicationManager.getApplication()
        val runnable = Runnable { sdkModificator.commitChanges() }
        if (application.isDispatchThread()) {
            application.runWriteAction(runnable)
        } else {
            application.invokeAndWait { application.runWriteAction(runnable) }
        }
        sdk.putUserData(MOCK_PY_MARKER_KEY, true)
        return sdk
    }

    /**
     * Unregister the Pro `PythonProHelpersLocator` from the `com.jetbrains.python.pythonHelpersLocator`
     * extension point (test JVM only).
     *
     * Creating the mock SDK triggers `PyTypeShed`'s lazy init, which calls
     * `PythonHelpersLocator.getHelpersRoots()` — that iterates every registered locator with no
     * exception guard. The obfuscated Pro locator's `getRoot()` calls `getPluginDistDirByClass`, which
     * throws `IllegalStateException: .../plugins/python/lib/modules should be lib directory` because the
     * unified 2026.1 Python plugin ships its code as v2 content modules under `lib/modules/` rather than
     * directly under `lib/`. That is purely a gradle-test-sandbox artifact (the flattened test classpath
     * means the plugin classes aren't under a `PluginAwareClassLoader`, so the safe branch of
     * `getPluginDistDirByClass` isn't taken; upstream
     * https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/2070, unfixed). Unlike the
     * community locator it reads no `idea.python.helpers.path` property, so it can't be pointed at a
     * valid root. Removing just this one dynamic EP leaves the community locator (fed by the
     * `-Didea.python.helpers.path` jvmArg) and the rest of the Pro Python plugin intact, so Python
     * resolution still works. Idempotent — safe to call before every SDK creation. Runtime is unaffected.
     *
     * This lives here rather than in a test-case base class because [create] is the one point every
     * test path funnels through: the cucumber glue calls it directly, and [com.jetbrains.snakecharm.SnakemakeTestCase]
     * reaches it via `PyLightProjectDescriptor.getSdk()`.
     */
    private fun removeCrashingProHelpersLocator() {
        val ep = ApplicationManager.getApplication()?.extensionArea
            ?.getExtensionPointIfRegistered<Any>("com.jetbrains.python.pythonHelpersLocator") ?: return
        ep.unregisterExtensions(
            { className, _ -> className != "com.jetbrains.python.PythonProHelpersLocator" },
            false,
        )
    }

    private fun toVersionString( level: LanguageLevel) = "Python ${level.toPythonVersion()}"

    private fun createRoots( mockSdkPath: String,  level: LanguageLevel): List<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        val localFS = LocalFileSystem.getInstance()
        ContainerUtil.addIfNotNull(
            result, localFS.refreshAndFindFileByIoFile(File(mockSdkPath, "Lib"))
        )
        ContainerUtil.addIfNotNull(
            result,
            localFS.refreshAndFindFileByIoFile(File(mockSdkPath, PythonSdkUtil.SKELETON_DIR_NAME))
        )
        result.addAll(findAllRootsForLanguageLevel(level))
        return result
    }

    private class PyMockSdkType(
        private val level: LanguageLevel
    ) : SdkTypeId {
        override fun getName() =  PyNames.PYTHON_SDK_ID_NAME

        override fun getVersionString(sdk: Sdk) = toVersionString(level)

        override fun saveAdditionalData(currentSdk: SdkAdditionalData, additional: org.jdom.Element) {}

        override fun loadAdditionalData(currentSdk: Sdk, additional: org.jdom.Element): SdkAdditionalData? = null
    }
}
