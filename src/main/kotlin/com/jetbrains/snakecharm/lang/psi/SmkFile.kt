package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.FileViewProvider
import com.intellij.psi.stubs.StubElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyFileImpl
import com.jetbrains.snakecharm.SmkFileType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, SnakemakeLanguageDialect) {
    // consider extend SnakemakeScopeOwner:ScopeOwner
    // e.g. CythonFile, CythonScopeOwner

    override fun getIcon(flags: Int) = SmkFileType.INSTANCE.icon

    override fun toString() = "SnakemakeFile: $name"

    override fun getStub(): StubElement<SmkFile>? = null

    fun collectSubworkflows(): List<Pair<String, SmkSubworkflow>> {
        val subworkflowNameAndPsi = arrayListOf<Pair<String, SmkSubworkflow>>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
                if (subworkflow.name != null) {
                    subworkflowNameAndPsi.add(subworkflow.name!! to subworkflow)
                }
            }
        })
        return subworkflowNameAndPsi
    }

    fun collectModules(): List<Pair<String, SmkModule>> {
        val moduleNameAndPsi = arrayListOf<Pair<String, SmkModule>>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkModule(module: SmkModule) {
                if (module.name != null) {
                    moduleNameAndPsi.add(module.name!! to module)
                }
            }
        })
        return moduleNameAndPsi
    }

    fun collectUses(): List<Pair<String, SmkUse>> {
        val useNameAndPsi = arrayListOf<Pair<String, SmkUse>>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkUse(use: SmkUse) {
                use.getProducedRulesNames().forEach {
                    useNameAndPsi.add(it.first to use)
                }
            }
        })
        return useNameAndPsi
    }

    fun collectCheckPoints(): List<Pair<String, SmkCheckPoint>> {
        val checkpointNameAndPsi = arrayListOf<Pair<String, SmkCheckPoint>>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
                if (checkPoint.name != null) {
                    checkpointNameAndPsi.add(checkPoint.name!! to checkPoint)
                }
            }
        })
        return checkpointNameAndPsi
    }

    fun collectRules(): List<Pair<String, SmkRule>> {
        // TODO: add tests, this is simple impl for internship task practice
        val ruleNameAndPsi = arrayListOf<Pair<String, SmkRule>>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkRule(rule: SmkRule) {
                if (rule.name != null) {
                    ruleNameAndPsi.add(rule.name!! to rule)
                }
            }
        })
        return ruleNameAndPsi
    }

    fun collectIncludes(): List<SmkWorkflowArgsSection> {
        val includeStatements = mutableListOf<SmkWorkflowArgsSection>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
                if (st.sectionKeyword == SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD) {
                    includeStatements.add(st)
                }
            }
        })

        return includeStatements
    }
}