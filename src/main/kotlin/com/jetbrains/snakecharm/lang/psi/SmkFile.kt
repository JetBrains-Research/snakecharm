package com.jetbrains.snakecharm.lang.psi

import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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

    fun collectUses(visitedFiles: MutableSet<PsiFile> = mutableSetOf()): List<Pair<String, SmkUse>> {
        val useNameAndPsi = arrayListOf<Pair<String, SmkUse>>()

        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this

            override fun visitSmkUse(use: SmkUse) {
                use.getProducedRulesNames(visitedFiles).forEach {
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

    /**
     * Returns [PsiElement] from [SmkUse] which may produce [name] or returns null if no such element
     */
    fun resolveByRuleNamePattern(
        name: String
    ): PsiElement? {
        val uses = advancedCollectUseSectionsWithWildcards(mutableSetOf())
        return uses.firstOrNull { (first) ->
            val pattern = first.replaceFirst("*", "(?<name>\\w+)").replace("*", "\\k<name>") + '$'
            pattern.toRegex().matches(name)
        }?.second?.nameIdentifier
    }

    /**
     * Collects local rules, rules, defined in use section, and rules, imported by 'include:'
     */
    fun advancedCollectRules(visitedFiles: MutableSet<PsiFile>) = advancedCollect(visitedFiles) { file ->
        file.collectRules() + file.collectUses(visitedFiles) + file.collectCheckPoints()
    }

    /**
     * Collects elements of [SmkUse] with wildcard '*' in name.
     * It collects elements from current [SmkFile] and from files which were imported via 'include:'
     */
    private fun advancedCollectUseSectionsWithWildcards(visitedFiles: MutableSet<PsiFile>) =
        advancedCollect(visitedFiles) { file ->
            val useNameAndPsi = arrayListOf<Pair<String, SmkUse>>()

            file.acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
                override val pyElementVisitor: PyElementVisitor = this

                override fun visitSmkUse(use: SmkUse) {
                    val moduleFile =
                        (use.getModuleName()?.reference?.element?.reference?.resolve() as? SmkModule)?.getPsiFile()
                    val doesNotReferToLocalModule = (moduleFile == null || moduleFile.virtualFile is HttpVirtualFile)
                    if (use.name?.contains('*') == true && doesNotReferToLocalModule) {
                        useNameAndPsi.add((use.name ?: return) to use)
                    }
                }
            })

            useNameAndPsi
        }.map { it.first to it.second as SmkUse }

    /**
     * Collects elements of type [SmkRuleOrCheckpoint] from a current [SmkFile] using [additionalCollector]
     * and collects elements from other files which were imported via 'include:'
     */
    private fun advancedCollect(
        visitedFiles: MutableSet<PsiFile>,
        additionalCollector: (SmkFile) -> List<Pair<String, SmkRuleOrCheckpoint>>
    ): List<Pair<String, SmkRuleOrCheckpoint>> {
        if (!visitedFiles.add(this)) {
            return emptyList()
        }

        val result = mutableListOf<Pair<String, SmkRuleOrCheckpoint>>()
        result.addAll(additionalCollector(this))
        collectIncludes().forEach { include ->
            include.references.forEach { reference ->
                if (reference is SmkIncludeReference) {
                    val file = reference.resolve() as? SmkFile
                    if (file != null) {
                        result.addAll(file.advancedCollect(visitedFiles, additionalCollector))
                    }
                }
            }
        }
        return result
    }

    fun findPepfile(): SmkWorkflowArgsSection? {
        var pepfile: SmkWorkflowArgsSection? = null
        acceptChildren(object : PyElementVisitor(), SmkElementVisitor {
            override val pyElementVisitor: PyElementVisitor = this
            override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
                if (st.sectionKeyword == SnakemakeNames.WORKFLOW_PEPFILE_KEYWORD) {
                    pepfile = st
                }
            }
        })
        return pepfile
    }
}