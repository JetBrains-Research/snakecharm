package features.glue

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.Disposable
import com.intellij.openapi.projectRoots.Sdk
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
    @JvmField var myCompletionListPresentations: List<LookupElementPresentation>? = null
    @JvmField var myGeneratedDocPopupText: String? = null
    @JvmField var myFoundRefs: List<PsiReference>? = null
    @JvmField var myFoundUsages: List<UsageInfo> = emptyList()
    @JvmField var myTestRootDisposable: TestDisposable? = null
    @JvmField var myInspectionProblemsCounts: MutableMap<String, Int>? = null
    @JvmField var myInspectionChecked: Boolean = false
    @JvmField var myPythonSnakemakeSdk: Sdk? = null
    @JvmField var myPythonOnlySdk: Sdk? = null

    fun injectionFixture() = myInjectionFixture!!
    fun fixture()= myFixture!!

    val MSG_COMPLETION_LIST_NOT_INITIALIZED = "Completion list not initialized, likely you've forgotten" +
            " 'I invoke autocompletion popup' step" +
            " or completion contained single variant matching item prefix so it was automatically inserted. For" +
            " the last scenario use `I invoke autocompletion popup and see a text:` step."

    fun completionList(): List<String> {
        requireNotNull(myCompletionList) { MSG_COMPLETION_LIST_NOT_INITIALIZED }
        return myCompletionList!!
    }
    fun completionListPresentations(): List<LookupElementPresentation> {
        requireNotNull(myCompletionListPresentations) { MSG_COMPLETION_LIST_NOT_INITIALIZED }
        return myCompletionListPresentations!!
    }

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