package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext
import com.jetbrains.python.codeInsight.completion.PyFunctionInsertHandler
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.CHECKPOINT_CLASS_FQN
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import icons.PythonIcons

class CheckpointType(
        val checkpoint: SmkCheckPoint,
        private val checkpointClassType: PyType?
) : PyType {

    override fun assertValid(message: String?) {
        checkpointClassType?.assertValid(message)

        if (!checkpoint.isValid) {
            throw PsiInvalidElementAccessException(checkpoint, message)
        }
    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult>? {

        //TODO: provide solution if sdk not configured:
        // XXX we assume that these elements will be dropped and won't live long
        // val fakeFun = SmkFakePsiElement(checkpoint, name, PythonIcons.Python.Function)
        // return listOf(RatedResolveResult(SmkResolveUtil.RATE_NORMAL, fakeFun))

        return checkpointClassType?.resolveMember(name, location, direction, resolveContext)
    }

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        if (checkpointClassType != null) {
            return checkpointClassType.getCompletionVariants(completionPrefix, location, context)
        }

        // If snakemake library not configured
        // Checkpoints are quite confusing, so let's provide autocompletion always
        val item = LookupElementBuilder
                .create("get")
                .withTypeText(CHECKPOINT_CLASS_FQN)
                .withIcon(PythonIcons.Python.Function)
                .withInsertHandler(PyFunctionInsertHandler.INSTANCE)
        return arrayOf(item)
    }

    override fun isBuiltin() = false

    override fun getName() = "Checkpoint"

    companion object {
        fun resolveDeclarationClass(
                scopeOwner: ScopeOwner,
                typeEvalContext: TypeEvalContext
        ): PyClass? {

            //TODO: maybe using implicits cache?

//            val resolveResult = PyResolveUtil.resolveQualifiedNameInScope(
//                    QualifiedName.fromDottedString(CHECKPOINT_CLASS_FQN),
//                    scopeOwner,
//                    typeEvalContext
//            )
//            val pyClass = resolveResult.firstOrNull()
//            if (pyClass is PyClass) {
//                return pyClass
//            }
            return null
        }
    }
}