package com.jetbrains.snakecharm.facet

import com.intellij.framework.detection.FacetBasedFrameworkDetector
import com.intellij.framework.detection.FileContentPattern
import com.jetbrains.snakecharm.SmkFileType

/**
 * Detects 'Snakemake' in project by snakemake related files
 * For some reason works in IDEA, not in PyCharm
 */
class SmkFrameworkDetector : FacetBasedFrameworkDetector<SnakemakeFacet, SmkFacetConfiguration>("snakemake") {
    override fun createSuitableFilePattern() = FileContentPattern.fileContent()!!
    override fun getFacetType() = SmkFacetType.INSTANCE
    override fun getFileType() = SmkFileType.INSTANCE
}