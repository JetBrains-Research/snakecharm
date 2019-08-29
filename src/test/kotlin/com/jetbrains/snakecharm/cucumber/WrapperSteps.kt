package com.jetbrains.snakecharm.cucumber

import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.codeInsight.wrapper.WrapperStorage
import cucumber.api.java.en.Given
import java.io.File

class WrapperSteps {
    @Given("^I prepare wrapper storage")
    fun iPrepareWrapperStorage() {
        // TODO still doesn't work when run from gradle
        val storageDataFileLines = File("${SnakemakeTestUtil.getTestDataPath()}/wrapper/storage_data.txt").readLines()

        WrapperStorage.getInstance().wrapperStringStorage =
                storageDataFileLines.map { it.replace("\\n", "\n") }.toMutableList()
        WrapperStorage.getInstance().wrapperStringStorage.forEach { println(it) }
    }
}