package com.jetbrains.snakecharm.inspections

import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkIgnorePyInspectionExtension: PyInspectionExtension() {
    override fun ignoreUnresolvedMember(type: PyType, name: String, context: TypeEvalContext): Boolean {
        if (type is SmkAvailableForSubscriptionType) {
            return name == "get" || name == "__getitem__"
        }
        return  false
    }

    // ignoreShadowed
    // ignoreMissingDocstring
    // ignoreMethodParameters
    // getFunctionParametersFromUsage
    // ignorePackageNameInRequirements
    // ignoreUnresolvedReference
    // ignoreProtectedSymbol
    // ignoreInitNewSignatures
}
