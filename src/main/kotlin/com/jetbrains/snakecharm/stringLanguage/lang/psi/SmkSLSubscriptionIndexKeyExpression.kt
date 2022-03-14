package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.jetbrains.python.psi.PyReferenceExpression

interface SmkSLSubscriptionIndexKeyExpression : SmkSLReferenceExpression, PyReferenceExpression {
    /**
     * Returns [true] if index key is empty
     */
    // E.g. for 'config[]' -> returns true
    // 'config[key]' -> returns false
    fun hasEmptyIndex(): Boolean
}