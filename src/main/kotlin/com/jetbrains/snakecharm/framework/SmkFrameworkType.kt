package com.jetbrains.snakecharm.framework

import com.intellij.framework.FrameworkType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.jetbrains.python.PythonModuleTypeBase
import com.jetbrains.python.module.PythonModuleType
import com.jetbrains.snakecharm.SnakemakeBundle

class SmkFrameworkType : FrameworkType(ID) {
    override fun getPresentableName() = SnakemakeBundle.message("smk.framework.display.name")
    override fun getIcon() = SmkSupportProjectSettings.getIcon()

    companion object {
        const val ID = "SmkSupportFrameworkType"

        fun isSuitableModuleType(module: Module) = isSuitableModuleType(ModuleType.get(module))

        fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean {
            // XXX let's allow in python modules only, but actually some user could
            // also want this in other language module + python facet
            return (moduleType is PythonModuleTypeBase) || (moduleType?.id == PythonModuleType.getInstance().id)
        }
    }
}