package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_BENCHMARK
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CONDA
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_CWL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_GROUP
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_INPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_LOG
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_MESSAGE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_OUTPUT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_PARAMS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_PRIORITY
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RESOURCES
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SCRIPT
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SHADOW
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SHELL
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_SINGULARITY
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_THREADS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_VERSION
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_WRAPPER

class SMKRuleParameterListStatement(node: ASTNode): SmkSectionStatement(node) { // PyNamedElementContainer
    companion object {
        val EXECUTION_KEYWORDS = setOf(SECTION_SHELL, SECTION_SCRIPT, SECTION_WRAPPER, SECTION_CWL)

        val PARAMS_NAMES = setOf(
                SECTION_OUTPUT, SECTION_INPUT, SECTION_PARAMS, SECTION_LOG, SECTION_RESOURCES,
                SECTION_BENCHMARK, SECTION_VERSION, SECTION_MESSAGE, SECTION_SHELL, SECTION_THREADS, SECTION_SINGULARITY,
                SECTION_PRIORITY, SECTION_WILDCARD_CONSTRAINTS, SECTION_GROUP, SECTION_SHADOW,
                SECTION_CONDA,
                SECTION_SCRIPT, SECTION_WRAPPER, SECTION_CWL
        )
    }
  
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRuleParameterListStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}