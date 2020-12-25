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
            sdkNameSuffix: String = "",
            vararg additionalRoots: VirtualFile
    ): Sdk {
        val mockPath = "$testDataRoot/MockSdk$version/"

        val sdkHome = File(mockPath, "bin/python$version").path
        val sdkType = PythonSdkType.getInstance()

        val roots = MultiMap.create<OrderRootType, VirtualFile>()
        val classes = OrderRootType.CLASSES

        ContainerUtil.putIfNotNull(
            classes,
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(mockPath, "Lib")),
            roots
        )

        ContainerUtil.putIfNotNull(
            classes,
            PyUserSkeletonsUtil.getUserSkeletonsDirectory(),
            roots
        )

        val level = LanguageLevel.fromPythonVersion(version)!!
        val typeShedDir = PyTypeShed.directory!!
        PyTypeShed.findRootsForLanguageLevel(level).forEach { path ->
            val file = typeShedDir.findFileByRelativePath(path)
            if (file != null) {
                roots.putValue(classes, file)
            }
        }

        val mockStubsPath = mockPath + PythonSdkUtil.SKELETON_DIR_NAME
        ContainerUtil.putIfNotNull(
                    classes,
                    LocalFileSystem.getInstance().refreshAndFindFileByPath(mockStubsPath),
                    roots
                )
        roots.putValues(classes, listOf(*additionalRoots))

        val sdk = MockSdk("$MOCK_SDK_NAME $version$sdkNameSuffix", sdkHome, "Python $version Mock SDK", roots, sdkType)

        // com.jetbrains.python.psi.resolve.PythonSdkPathCache.getInstance() corrupts SDK, so have to clone
        return sdk.clone()
    }
}
