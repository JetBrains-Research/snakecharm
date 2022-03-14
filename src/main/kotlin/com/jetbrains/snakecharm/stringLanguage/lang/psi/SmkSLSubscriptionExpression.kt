package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.jetbrains.python.psi.PySubscriptionExpression

interface SmkSLSubscriptionExpression : SmkSLReferenceExpression, PySubscriptionExpression {
    /**
     * Returns list of all subscription keys
     */
    // E.g. for 'config[first][second]'
    // It should return listOf("first", "second")
    fun getSubscriptionKeys(): MutableList<String>
}