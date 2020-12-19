package com.jetbrains.snakecharm.facet

import com.intellij.framework.FrameworkType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.jetbrains.python.PythonModuleTypeBase
import com.jetbrains.snakecharm.SnakemakeBundle

class SmkSupportFrameworkType : FrameworkType(ID) {
    override fun getPresentableName() = SnakemakeBundle.message("smk.framework.display.name")
    override fun getIcon() = SmkSupportProjectSettings.getIcon()

    companion object {
        const val ID = "SmkSupportFrameworkType"

        fun isSuitableModuleType(module: Module) = isSuitableModuleType(ModuleType.get(module))

        fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean {
            // XXX let's allow in python modules only, but actually some user could
            // also want this in other language module + python facet
            return moduleType is PythonModuleTypeBase
        }
    }
}