package com.jetbrains.snakecharm;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.AccessDirection;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.jetbrains.python.psi.types.PyType;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile;

import java.util.ArrayList;
import java.util.List;


public class PyRuleType implements PyType {
    private List<Pair<String, PsiElement>> rules;

    public PyRuleType(PsiFile containingFile) {
        rules = ((SnakemakeFile)containingFile).collectRules();
    }

    @Nullable
    public List<? extends RatedResolveResult> resolveMember(@NotNull String name, @Nullable final PyExpression location,
                                                            @NotNull final AccessDirection direction, @NotNull final PyResolveContext resolveContext) {
        final List<RatedResolveResult> result = new ArrayList<>();

        for (Pair<String, PsiElement> rule : rules) {
            if (rule.component1().equals(name)) {
                result.add(new RatedResolveResult(0, rule.component2()));

                return result;
            }
        }

        return null;
    }

    public Object[] getCompletionVariants(String completionPrefix, PsiElement location, ProcessingContext context) {
        final List<Object> result = new ArrayList<>();

        if (completionPrefix != null) {
            for (Pair<String, PsiElement> rule : rules) {
                if (rule.component1().startsWith(completionPrefix)) {
                    result.add(LookupElementBuilder.create(rule.component1()));
                }
            }
        }

        return ArrayUtil.toObjectArray(result);
    }

    @Nullable
    public String getName() {
        return null;
    }

    public boolean isBuiltin() {
        return true;
    }

    public void assertValid(String message) {
    }
}
