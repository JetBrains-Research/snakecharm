package com.jetbrains.snakecharm.lang.psi

// rule section is an 'input' or 'run' section in a Snakemake rule
// which corresponds to SMKRuleParameterListStatement and SMKRuleRunParameter respectively
// thus the need for an interface: to group the two together
interface SMKRuleSection