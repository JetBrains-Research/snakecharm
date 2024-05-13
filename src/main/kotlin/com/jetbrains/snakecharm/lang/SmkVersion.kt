package com.jetbrains.snakecharm.lang

class SmkVersion(
    val version: String
) : Comparable<SmkVersion> {
    val major: Int
    val minor: Int
    val patch: Int

    init {
        val split = version.split('.')
        if (split.size != 3) {
            throw IllegalArgumentException("Provided snakemake version $version is not correct")
        }
        major = split[0].toInt()
        minor = split[1].toInt()
        patch = split[2].toInt()
    }

    override fun toString(): String = version

    override fun compareTo(other: SmkVersion): Int {
        var res = major.compareTo(other.major)
        if (res == 0) res = minor.compareTo(other.minor)
        if (res == 0) res = patch.compareTo(other.patch)
        return res
    }

}