package com.jetbrains.snakecharm.inspections

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.elementType
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

/**
 * See also: [com.jetbrains.snakecharm.lang.highlighter.SnakemakeVisitorFilter]
 */
class SmkIgnorePyInspectionExtension : PyInspectionExtension() {
    override fun ignoreUnresolvedMember(type: PyType, name: String, context: TypeEvalContext): Boolean {
        if (type is SmkAvailableForSubscriptionType) {
            return name == "get" || name == "__getitem__"
        }
        return false
    }

    override fun ignoreShadowed(element: PsiElement) = element is SmkRuleOrCheckpointArgsSection

    override fun ignoreUnresolvedReference(
        node: PyElement,
        reference: PsiReference,
        context: TypeEvalContext,
    ): Boolean {
        if (SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(node)) {
            if (node.parent.elementType == SmkElementTypes.USE_IMPORTED_RULES_NAMES) {
                return true
            }

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

    override fun ignoreUnused(local: PsiElement?, evalContext: TypeEvalContext): Boolean {
        // If inspection is suppressed, SOE: in Parser #380 not happen
        // temporary turn off suppressing
        if (local is SmkRuleOrCheckpointArgsSection) {
            return true
        }
        return false
    }
}
