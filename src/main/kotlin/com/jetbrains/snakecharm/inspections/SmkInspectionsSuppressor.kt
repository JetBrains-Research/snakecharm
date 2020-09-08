package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.SuppressionUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.inspections.quickfix.PySuppressInspectionFix
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.SmkSection
import java.util.regex.Pattern

/**
 *
 * Snakemake specific suppressor, e.g. for Rule-like sections and it's subsections
 */
class SmkInspectionsSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        // Here Could be any QuickFix, not only registered via #getSuppressActions

        val containingArgsSection = getContainingArgsSection(element)
        if (containingArgsSection != null) {
            // allow only for Snakemake Section arguments
            val suppressHolder = topmostExpressionForSectionArg(element)
            if (suppressHolder != null) {
                if (isSuppressedForElement(suppressHolder, toolId)) {
                    return true
                }
            }
        }

        val parentSmkSection = PsiTreeUtil.getParentOfType(element, SmkSection::class.java)
        if (parentSmkSection != null) {
            if (isSuppressedForElement(parentSmkSection, toolId)) {
                return true
            }

            val parentRuleLike = PsiTreeUtil.getParentOfType(parentSmkSection, SmkRuleLike::class.java)
            if (parentRuleLike != null) {
                return isSuppressedForElement(parentRuleLike, toolId)
            }
        }

        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return arrayOf(
            object : PySuppressInspectionFix(
                toolId, SnakemakeBundle.message("INSP.snakemake.suppressor.suppress.for.section.argument"),
                PyExpression::class.java
            ) {
                override fun getContainer(context: PsiElement?): PsiElement? {
                    if (getContainingArgsSection(context) != null) {
                        return topmostExpressionForSectionArg(context)
                    }
                    return null
                }
            },

            object : PySuppressInspectionFix(
                toolId,
                SnakemakeBundle.message("INSP.snakemake.suppressor.suppress.for.section"),
                SmkSection::class.java
            ) {
                override fun getContainer(context: PsiElement?): PsiElement? {
                    val container = super.getContainer(context)
                    if (container is SmkRuleLike<*>) {
                        return null
                    }
                    return container
                }
            },

            PySuppressInspectionFix(
                toolId,
                SnakemakeBundle.message("INSP.snakemake.suppressor.suppress.for.rule.like"),
                SmkRuleLike::class.java
            )

        )
    }

    private fun topmostExpressionForSectionArg(context: PsiElement?) = getTopmostParentOfType(
        context, PyExpression::class.java, false,
        SmkArgsSection::class.java
    )

    fun getContainingArgsSection(context: PsiElement?) =
        PsiTreeUtil.getParentOfType(context, SmkArgsSection::class.java)

    companion object {
        // TODO: as for PyCharm API
        private val SUPPRESS_PATTERN =
            Pattern.compile("\\s*noinspection\\s+([a-zA-Z_0-9.-]+(\\s*,\\s*[a-zA-Z_0-9.-]+)*)\\s*\\w*")

        fun <T : PsiElement?> getTopmostParentOfType(
            element: PsiElement?,
            aClass: Class<T>,
            strict: Boolean,
            vararg stopAt: Class<out PsiElement>
        ): T? {
            var answer = PsiTreeUtil.getParentOfType(element, aClass, strict, *stopAt)
            do {
                val next = PsiTreeUtil.getParentOfType(answer, aClass, true, *stopAt) ?: break
                answer = next
            } while (true)
            return answer
        }

        // TODO: as for PyCharm API
        private fun isSuppressedForElement(stmt: PyElement, suppressId: String): Boolean {
            var prevSibling = stmt.prevSibling
            if (prevSibling == null) {
                val parent = stmt.parent
                if (parent != null) {
                    prevSibling = parent.prevSibling
                }
            }
            while (prevSibling is PsiComment || prevSibling is PsiWhiteSpace) {
                if (prevSibling is PsiComment && isSuppressedInComment(
                        prevSibling.getText().substring(1).trim { it <= ' ' }, suppressId
                    )
                ) {
                    return true
                }
                prevSibling = prevSibling.prevSibling
            }
            return false
        }


        // TODO: as for PyCharm API
        private fun isSuppressedInComment(commentText: String, suppressId: String): Boolean {
            val m = SUPPRESS_PATTERN.matcher(commentText)
            return m.matches() && SuppressionUtil.isInspectionToolIdMentioned(m.group(1), suppressId)
        }
    }
}