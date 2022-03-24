package com.jetbrains.snakecharm.inspections

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.completion.yamlKeys.SmkYAMLKeysStorage
import com.jetbrains.snakecharm.lang.psi.SmkModule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkUse
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.references.SmkSectionNameArgInPySubscriptionLikeReference
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.lang.psi.types.SmkConfigType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference

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
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(node)) {
            return false
        }

        if(isDefinedYAMLKeyValuePair(reference)) {
            return true
        }

        val refElement = reference.element

        val use = node.parentOfType<SmkUse>()
        if (use != null) {
            // Ignore references imported by 'module' from remote file
            //
            // First check that reference is in `use` imported rules list
            if (use.getDefinedReferencesOfImportedRuleNames()?.any { it == refElement } == true) {
                val module = use.getModuleName()?.reference?.resolve()
                val file = (module as? SmkModule)?.getPsiFile()?.virtualFile
                return module != null && (file == null || file is HttpVirtualFile)
            }
        }

        if (node is PyQualifiedExpression) {
            // Maybe referenceName is better here?
            //val referencedName = node.referencedName
            //return "config".equals(referencedName)
            return node.textMatches(SnakemakeAPI.SMK_VARS_CONFIG) ||
                    node.textMatches(SnakemakeAPI.SMK_VARS_PEP)
        }
        return false
    }

    private fun isDefinedYAMLKeyValuePair(reference: PsiReference) : Boolean{
        val type = (reference as? SmkSectionNameArgInPySubscriptionLikeReference)?.type ?: (reference as? SmkSLSubscriptionKeyReference)?.type
        if (type !is SmkConfigType){
            return false
        }
        val storage = reference.element.project.service<SmkYAMLKeysStorage>()
        return storage.keyWasDefinedInYAMLKeyValuePairs(reference.canonicalText)
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
