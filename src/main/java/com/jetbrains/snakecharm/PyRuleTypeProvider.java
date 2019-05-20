package com.jetbrains.snakecharm;

import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PyRuleTypeProvider extends PyTypeProviderBase {
    @Nullable
    @Override
    public PyType getReferenceExpressionType(@NotNull PyReferenceExpression referenceExpression, @NotNull TypeEvalContext context) {
        if (!referenceExpression.isQualified()) {
            final String name = referenceExpression.getReferencedName();
            if ("rules".equals(name)) {
                return new PyRuleType(referenceExpression.getContainingFile());
            }
        }

        return null;
    }
}
