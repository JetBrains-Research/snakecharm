package hellocucumber

import com.intellij.testFramework.fixtures.CodeInsightTestFixture

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
object SnakemakeWorld {
    var myFixture: CodeInsightTestFixture? = null
    fun fixture()= myFixture!!
}