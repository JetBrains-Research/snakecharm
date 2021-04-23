package com.jetbrains.snakecharm.inspections

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkIgnorePyInspectionExtension: PyInspectionExtension() {
    override fun ignoreUnresolvedMember(type: PyType, name: String, context: TypeEvalContext): Boolean {
        if (type is SmkAvailableForSubscriptionType) {
            return name == "get" || name == "__getitem__"
        }
        return  false
    }

    override fun ignoreShadowed(element: PsiElement) = element is SmkRuleOrCheckpointArgsSection

    override fun ignoreUnresolvedReference(
        node: PyElement,
        reference: PsiReference,
        context: TypeEvalContext
    ): Boolean {
        if (SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(node)) {
            if (node is PyQualifiedExpression) {
                // Maybe referenceName is better here?
                //val referencedName = node.referencedName
                //return "config".equals(referencedName)
                return node.textMatches(SnakemakeAPI.SMK_VARS_CONFIG)
            }
        }
        return false
    }
    // ignoreMissingDocstring
    // ignoreMethodParameters
    // getFunctionParametersFromUsage
    // ignorePackageNameInRequirements
    // ignoreUnresolvedReference
    // ignoreProtectedSymbol
    // ignoreInitNewSignatures
}
