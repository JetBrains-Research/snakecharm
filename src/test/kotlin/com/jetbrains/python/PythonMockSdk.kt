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

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.jetbrains.python.codeInsight.typing.PyTypeShed
import com.jetbrains.python.codeInsight.userSkeletons.PyUserSkeletonsUtil
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUtil
import org.jetbrains.annotations.NonNls
import java.io.File
import java.util.*

/**
 * We cannot re-use PythonMockSdk because api not available in Platform artifacts
 * 
 * @author yole
 */
object PythonMockSdk {
    @NonNls
    private val MOCK_SDK_NAME = "Mock Python SDK"

    fun create(
            testDataRoot: String,
            version: String,
            vararg additionalRoots: VirtualFile
    ): Sdk {
        val mock_path = "$testDataRoot/MockSdk$version/"

        val sdkHome = File(mock_path, "bin/python$version").path
        val sdkType = PythonSdkType.getInstance()

        val roots = MultiMap.create<OrderRootType, VirtualFile>()

        val libPath = File(mock_path, "Lib")
        if (libPath.exists()) {
            roots.putValue(OrderRootType.CLASSES, LocalFileSystem.getInstance().refreshAndFindFileByIoFile(libPath))
        }

        roots.putValue(OrderRootType.CLASSES, PyUserSkeletonsUtil.getUserSkeletonsDirectory())

        val level = LanguageLevel.fromPythonVersion(version)
        val typeShedDir = PyTypeShed.directory!!
        PyTypeShed.findRootsForLanguageLevel(level).forEach { path ->
            val file = typeShedDir.findFileByRelativePath(path)
            if (file != null) {
                roots.putValue(OrderRootType.CLASSES, file)
            }
        }

        val mock_stubs_path = mock_path + PythonSdkUtil.SKELETON_DIR_NAME
        val classes = OrderRootType.CLASSES
        ContainerUtil.putIfNotNull(
                    classes,
                    LocalFileSystem.getInstance().refreshAndFindFileByPath(mock_stubs_path),
                    roots
                )
        roots.putValues(classes, Arrays.asList(*additionalRoots))
        roots.putValue(PythonSdkType.BUILTIN_ROOT_TYPE, LocalFileSystem.getInstance().refreshAndFindFileByPath(mock_stubs_path))

        for (root in additionalRoots) {
            roots.putValue(OrderRootType.CLASSES, root)
        }

        val sdk = MockSdk("$MOCK_SDK_NAME $version", sdkHome, "Python $version Mock SDK", roots, sdkType)

        // com.jetbrains.python.psi.resolve.PythonSdkPathCache.getInstance() corrupts SDK, so have to clone
        return sdk.clone()
    }
}
