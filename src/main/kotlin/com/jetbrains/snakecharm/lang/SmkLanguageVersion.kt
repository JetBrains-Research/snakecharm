package com.jetbrains.snakecharm.lang

import org.jsoup.internal.StringUtil

class SmkLanguageVersion(
    val version: String
) : Comparable<SmkLanguageVersion> {
    val major: Int
    val minor: Int
    val patch: Int

    init {
        val split = version.split('.')
        if (split.size > 3 || split.isEmpty()) {
            throw IllegalArgumentException("Provided snakemake version $version is not correct")
        }
        for (s in split) {
            if (!StringUtil.isNumeric(s)) {
                throw IllegalArgumentException("Provided snakemake version $version is not correct")
            }
        }
        major = split[0].toInt()
        minor = if (split.size > 1) split[1].toInt() else 0
        patch = if (split.size > 2) split[2].toInt() else 0
    }

    override fun toString(): String = version

    override fun compareTo(other: SmkLanguageVersion): Int {
        var res = major.compareTo(other.major)
        if (res == 0) res = minor.compareTo(other.minor)
        if (res == 0) res = patch.compareTo(other.patch)
        return res
    }

}