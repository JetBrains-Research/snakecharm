package features.glue

import com.intellij.openapi.Disposable
import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.InjectionTestFixture
import com.intellij.usageView.UsageInfo

/**
 * @author Roman.Chernyatchik
 * @date 2019-04-28
 */
object SnakemakeWorld {
    // NB: all fields should be null by default and with @JvmField notation (for cleanup Hook)
    @JvmField var myInjectionFixture: InjectionTestFixture? = null
    @JvmField var myFixture: CodeInsightTestFixture? = null
    @JvmField var myCompletionList: List<String>? = null
    @JvmField var myGeneratedDocPopupText: String? = null
    @JvmField var myFoundRefs: List<PsiReference>? = null
    @JvmField var myFoundUsages: List<UsageInfo> = emptyList()
    @JvmField var myTestRootDisposable: TestDisposable? = null

    fun injectionFixture() = myInjectionFixture!!
    fun fixture()= myFixture!!
    fun completionList()= myCompletionList!!

    fun getOffsetUnderCaret() = fixture().editor.caretModel.offset
    fun findPsiElementUnderCaret() = fixture().file.findElementAt(getOffsetUnderCaret())
}


class TestDisposable : Disposable {
    @Volatile
    var isDisposed: Boolean = false

    override fun dispose() {
        isDisposed = true
    }
}