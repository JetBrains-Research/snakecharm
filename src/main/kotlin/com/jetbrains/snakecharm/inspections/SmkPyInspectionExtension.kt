package com.jetbrains.snakecharm.inspections

import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkPyInspectionExtension: PyInspectionExtension() {
    override fun ignoreUnresolvedMember(type: PyType, name: String, context: TypeEvalContext) =
            type is SmkAvailableForSubscriptionType && name == "__getitem__"
}
