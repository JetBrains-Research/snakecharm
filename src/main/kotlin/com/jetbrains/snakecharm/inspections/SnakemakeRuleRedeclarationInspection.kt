package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

class SnakemakeRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : PyInspectionVisitor(holder, session) {
        private val ruleNames = mutableSetOf<String>()
        private val namesToDeclarations = mutableMapOf<String, MutableList<SMKRule>>()

        override fun visitFile(file: PsiFile?) {
            if (file !is SnakemakeFile) {
                return
            }

            val rules = file.children.filter { it is SMKRule }
            rules.forEach { namesToDeclarations[(it as SMKRule).name]?.add(it) }
            //for ()
        }

        override fun visitElement(element: PsiElement?) {
            val rule = (element?.node as LeafPsiElement).parent as? SMKRule ?: return
            val name = rule.name ?: return
            //val rule = element?.node?.treeParent as? SMKRule ?: return

            if (ruleNames.contains(name)) {
                registerProblem(element, "Multiple declarations of rule ${rule.name}.")
            } else {
                ruleNames.add(name)
            }

            super.visitElement(element)

        }
    }
}